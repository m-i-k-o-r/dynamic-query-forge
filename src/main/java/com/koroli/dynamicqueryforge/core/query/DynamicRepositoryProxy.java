package com.koroli.dynamicqueryforge.core.query;

import com.koroli.dynamicqueryforge.core.expression.ExpressionTreeEditor;
import com.koroli.dynamicqueryforge.core.expression.SQLTreeBuilder;
import com.koroli.dynamicqueryforge.core.parser.SQLParser;
import com.koroli.dynamicqueryforge.executor.DatabaseType;
import com.koroli.dynamicqueryforge.executor.MongoDBDatabaseClient;
import com.koroli.dynamicqueryforge.executor.PostgreSQLDatabaseClient;
import com.koroli.dynamicqueryforge.utils.ExpressionConverterUtils;
import com.koroli.queryconverter.converters.QueryConverter;
import com.koroli.queryconverter.exceptions.QueryConversionException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Класс Proxy для обработки аннотации @Query на методах
 */
@Component
public class DynamicRepositoryProxy implements InvocationHandler {

    @Autowired
    private QueryConverter queryConverter;

    @Autowired
    private DatabaseType databaseType;

    @Autowired(required = false)
    private PostgreSQLDatabaseClient postgreSQLDatabaseClient;

    @Autowired(required = false)
    private MongoDBDatabaseClient mongoDBDatabaseClient;

    /**
     * Метод, который перехватывает вызовы методов интерфейса
     * <br/>
     * Если метод аннотирован @Query, обрабатывает SQL запрос
     *
     * @param proxy  прокси объект
     * @param method вызываемый метод
     * @param args   аргументы вызова метода
     * @return результат обработки (пока что результат это финальный SQL запрос)
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        Query queryAnnotation = method.getAnnotation(Query.class);
        if (queryAnnotation == null) {
            return null;
        }

        // Парсинг и обработка исходного SQL-запроса
        String originalQuery = queryAnnotation.value();
        Statement statement = SQLParser.parse(originalQuery);
        System.out.println(SQLTreeBuilder.buildTree(statement));          // Дерево лога оригинального запроса

        // Сбор параметров и удаление условий с null
        Map<String, Object> paramNameToValue = collectParamNameToValue(method, args);
        Statement filteredStatement = removeNullParameterConditions(statement, paramNameToValue);
        System.out.println(SQLTreeBuilder.buildTree(filteredStatement));  // Дерево лога обновленного запроса

        Class<?> returnType = getReturnType(method);
        boolean isSingleResult = !List.class.isAssignableFrom(method.getReturnType());

        // Генерация финального запроса
        return switch (databaseType) {
            case POSTGRESQL -> postgreSQLDatabaseClient.executeQuery(filteredStatement, returnType, isSingleResult);
            case MONGODB -> {
                String collectionName = "users";
                yield mongoDBDatabaseClient.executeQuery(collectionName, filteredStatement, returnType, isSingleResult);
            }
            default -> throw new UnsupportedOperationException("Неподдерживаемый тип базы данных");
        };
    }

    private Class<?> getReturnType(Method method) {
        if (List.class.isAssignableFrom(method.getReturnType())) {
            ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
            return (Class<?>) genericReturnType.getActualTypeArguments()[0];
        } else {
            return method.getReturnType();
        }
    }

    /**
     * Собирает значения параметров метода в map "имя параметра -> значение"
     *
     * @param method метод, параметры которого анализируются
     * @param args   аргументы, переданные в метод
     * @return карта параметров
     */
    private Map<String, Object> collectParamNameToValue(Method method, Object[] args) {
        Map<String, Object> paramNameToValue = new LinkedHashMap<>();

        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object argumentValue = args[i];

            System.out.println("Parameter #" + (i + 1) + " = " + argumentValue);

            // Проверяем, есть ли у параметра аннотация @Param
            boolean hasParamAnnotation = false;
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof Param paramAnn) {
                    String paramName = paramAnn.value();
                    paramNameToValue.put(paramName, argumentValue);
                    hasParamAnnotation = true;

                    System.out.println("    ├── @Param: " + paramAnn.value());
                    break;
                }
            }

            // Если аннотации @Param нет, используем имя параметра как ключ
            if (!hasParamAnnotation) {
                paramNameToValue.put(parameter.getName(), argumentValue);
            }
            System.out.println("    └── name variable: " + parameter.getName());
        }

        return paramNameToValue;
    }

    /**
     * Удаляет условия из запроса, если соответствующие параметры равны null или отсутствуют в map
     *
     * @param statement        исходный SQL запрос
     * @param paramNameToValue карта параметров
     * @return модифицированный SQL запрос
     */
    private Statement removeNullParameterConditions(Statement statement, Map<String, Object> paramNameToValue) {
        return switch (statement) {
            case Select select -> removeNullParameterConditionsFromSelect(select, paramNameToValue);
            case Update update -> removeNullParameterConditionsFromUpdate(update, paramNameToValue);
            case Insert insert -> removeNullParameterConditionsFromInsert(insert, paramNameToValue);
            case Delete delete -> removeNullParameterConditionsFromDelete(delete, paramNameToValue);
            default -> statement;
        };
    }

    private Statement removeNullParameterConditionsFromSelect(Select select, Map<String, Object> paramNameToValue) {
        if (!(select instanceof PlainSelect plainSelect)) {
            return select;
        }

        Expression whereExp = plainSelect.getWhere();

        if (whereExp != null) {
            Expression filteredWhere = filterWhereExpression(whereExp, paramNameToValue);
            plainSelect.setWhere(filteredWhere);
        }

        return select;
    }

    private Statement removeNullParameterConditionsFromUpdate(Update update, Map<String, Object> paramNameToValue) {
        // Обработка WHERE
        Expression whereExp = update.getWhere();
        if (whereExp != null) {
            Expression filteredWhere = filterWhereExpression(whereExp, paramNameToValue);
            update.setWhere(filteredWhere);
        }

        // Обработка SET
        List<Column> columns = update.getColumns();
        List<Expression> expressions = update.getExpressions();
        if (columns != null && expressions != null) {
            List<Expression> filteredExpressions = new ArrayList<>();
            for (Expression expression : expressions) {
                Expression filteredExpression = filterWhereExpression(expression, paramNameToValue);
                filteredExpressions.add(filteredExpression);
            }
            update.setExpressions(filteredExpressions);
        }

        return update;
    }

    private Statement removeNullParameterConditionsFromInsert(Insert insert, Map<String, Object> paramNameToValue) {
        // Если вставка через VALUES
        if (insert.getValues() != null) {
            ExpressionList expressionList = insert.getValues().getExpressions();

            List<Expression> oldExpressions = expressionList.getExpressions();
            List<Expression> newExpressions = new ArrayList<>();

            for (Expression expr : oldExpressions) {
                Expression filteredExpr = filterWhereExpression(expr, paramNameToValue);
                Expression replacedExpr = replaceParametersWithValues(filteredExpr, paramNameToValue);

                if (replacedExpr != null) {
                    newExpressions.add(replacedExpr);
                }
            }
            expressionList.setExpressions(newExpressions);
        }

        // Если INSERT ... SELECT
        if (insert.getSelect() != null) {
            removeNullParameterConditionsFromSelect(insert.getSelect(), paramNameToValue);
        }

        return insert;
    }

    private Statement removeNullParameterConditionsFromDelete(Delete delete, Map<String, Object> paramNameToValue) {
        Expression whereExp = delete.getWhere();

        if (whereExp != null) {
            Expression filteredWhere = filterWhereExpression(whereExp, paramNameToValue);
            delete.setWhere(filteredWhere);
        }

        return delete;
    }

    /**
     * Проходит по выражению и подставляет значения параметров вместо ссылок.
     *
     * @param expression       исходное выражение
     * @param paramNameToValue карта параметров
     * @return выражение с подставленными значениями
     */
    private Expression replaceParametersWithValues(Expression expression, Map<String, Object> paramNameToValue) {
        return switch (expression) {
            case JdbcNamedParameter jdbcParam -> {
                String paramName = jdbcParam.getName();

                if (paramNameToValue.containsKey(paramName)) {
                    Object value = paramNameToValue.get(paramName);
                    if (value != null) {
                        yield ExpressionConverterUtils.convertParameterValue(value);
                    }
                }

                yield expression;
            }

            // Если узел представляет собой "колонку"
            case Column column -> {
                String columnName = column.getColumnName();

                if (paramNameToValue.containsKey(columnName)) {
                    Object value = paramNameToValue.get(columnName);
                    if (value != null) {
                        yield ExpressionConverterUtils.convertParameterValue(value);
                    }
                }

                yield expression;
            }

            // Если это бинарное выражение
            case BinaryExpression binaryExpression -> {
                binaryExpression.setLeftExpression(
                        replaceParametersWithValues(binaryExpression.getLeftExpression(), paramNameToValue)
                );
                binaryExpression.setRightExpression(
                        replaceParametersWithValues(binaryExpression.getRightExpression(), paramNameToValue)
                );
                yield binaryExpression;
            }

            // Если ничего из вышеперечисленного не подошло
            case null, default -> expression;
        };
    }

    /**
     * Рекурсивно обрабатывает выражение WHERE для удаления подвыражений,
     * ссылающихся на null или отсутствующие параметры
     *
     * @param expression       выражение WHERE
     * @param paramNameToValue карта параметров
     * @return модифицированное выражение WHERE
     */
    private Expression filterWhereExpression(Expression expression, Map<String, Object> paramNameToValue) {
        ExpressionTreeEditor editor = new ExpressionTreeEditor(paramNameToValue);
        return editor.modify(expression);
    }


    private String convertToMongoQuery(Statement statement) {
        try {
            return queryConverter.convert(statement);
        } catch (QueryConversionException e) {
            throw new RuntimeException("Ошибка при конвертации SQL в MongoDB запрос", e);
        }
    }
}
