package com.koroli.dynamicqueryforge.annotation;

import com.koroli.dynamicqueryforge.executors.DatabaseType;
import com.koroli.dynamicqueryforge.executors.MongoDBDatabaseClient;
import com.koroli.dynamicqueryforge.executors.PostgreSQLDatabaseClient;
import com.koroli.dynamicqueryforge.parser.SQLParser;
import com.koroli.dynamicqueryforge.parser.SQLTreeBuilder;
import com.koroli.queryconverter.converters.QueryConverter;
import com.koroli.queryconverter.exceptions.QueryConversionException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс Proxy для обработки аннотации @Query на методах
 */
@Component
public class RepositoryProxy implements InvocationHandler {

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

        // Генерация финального запроса
        String newQuery = switch (databaseType) {
            case POSTGRESQL -> filteredStatement.toString();
            case MONGODB -> convertToMongoQuery(filteredStatement);
            default -> throw new UnsupportedOperationException("Неподдерживаемый тип базы данных");
        };

        // Выполнение запроса и получение результата
        return executeQuery(newQuery, paramNameToValue);
    }

    /**
     * Собирает значения параметров метода в map "имя параметра -> значение"
     *
     * @param method метод, параметры которого анализируются
     * @param args   аргументы, переданные в метод
     * @return карта параметров
     */
    private Map<String, Object> collectParamNameToValue(Method method, Object[] args) {
        Map<String, Object> paramNameToValue = new HashMap<>();

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
     * Удаляет условия из WHERE, если соответствующие параметры равны null или отсутствуют в map
     *
     * @param statement        исходный SQL запрос
     * @param paramNameToValue карта параметров
     * @return модифицированный SQL запрос
     */
    private Statement removeNullParameterConditions(Statement statement, Map<String, Object> paramNameToValue) {
        if (!(statement instanceof Select select)) {
            return statement;
        }

        if (!(select instanceof PlainSelect plainSelect)) {
            return statement;
        }

        Expression whereExp = plainSelect.getWhere();

        if (whereExp == null) {
            return statement;
        }

        // Удаляем условия, ссылающиеся на null-параметры
        Expression filteredWhere = filterWhereExpression(whereExp, paramNameToValue);
        plainSelect.setWhere(filteredWhere);

        return statement;
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
            return queryConverter.convert(SQLParser.parse(statement.toString()));
        } catch (QueryConversionException e) {
            throw new RuntimeException("Ошибка при конвертации SQL в MongoDB запрос", e);
        }
    }


    private Object executeQuery(String query, Map<String, Object> params) {
        return switch (databaseType) {
            case POSTGRESQL -> {
                ResultSet resultSet = postgreSQLDatabaseClient.executeQuery(query, params);
                yield processResultSet(resultSet);
            }
            case MONGODB -> {
                Iterable<Document> documents = mongoDBDatabaseClient.executeQuery("users", Document.parse(query));
                yield processMongoResults(documents);
            }
            default -> throw new UnsupportedOperationException("Неподдерживаемый тип базы данных");
        };
    }

    private List<Map<String, Object>> processResultSet(ResultSet resultSet) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), resultSet.getObject(i));
                }
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обработке результатов PostgreSQL", e);
        }
        return results;
    }

    private List<Map<String, Object>> processMongoResults(Iterable<Document> documents) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Document document : documents) {
            results.add(document);
        }
        return results;
    }
}
