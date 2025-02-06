package com.koroli.dynamicqueryforge.core.query;

import com.koroli.dynamicqueryforge.core.expression.ExpressionTreeEditor;
import com.koroli.dynamicqueryforge.core.parser.SQLParser;
import com.koroli.dynamicqueryforge.executor.DatabaseType;
import com.koroli.dynamicqueryforge.executor.MongoDBDatabaseClient;
import com.koroli.dynamicqueryforge.executor.PostgreSQLDatabaseClient;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Proxy-класс для обработки методов, аннотированных @Query.
 */
@Component
public class DynamicRepositoryProxy implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRepositoryProxy.class);

    @Autowired
    private boolean logModifiedQueryEnabled;

    @Autowired
    private DatabaseType databaseType;

    @Autowired(required = false)
    private PostgreSQLDatabaseClient postgreSQLDatabaseClient;

    @Autowired(required = false)
    private MongoDBDatabaseClient mongoDBDatabaseClient;

    /**
     * Перехватывает вызовы методов и обрабатывает SQL-запросы.
     *
     * @param proxy  прокси-объект
     * @param method вызываемый метод
     * @param args   аргументы вызова метода
     * @return результат выполнения запроса
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        UUID requestId = UUID.randomUUID();

        Query queryAnnotation = method.getAnnotation(Query.class);
        if (queryAnnotation == null) return null;

        // Парсинг SQL-запроса
        String originalQuery = queryAnnotation.value();
        Statement statement = SQLParser.parse(originalQuery);
        logQuery(requestId, "Original SQL Query", statement.toString());


        // Сбор параметров и создание редактора выражений
        Map<String, Object> paramsMap = extractParams(requestId, method, args);
        ExpressionTreeEditor editor = new ExpressionTreeEditor(paramsMap);


        // Модификация запроса на основе параметров
        Statement modifiedQuery = modifyQuery(statement, editor);
        logQuery(requestId, "Modified SQL Query", modifiedQuery.toString());

        Class<?> returnType = getReturnType(method);
        boolean isSingleResult = !List.class.isAssignableFrom(method.getReturnType());

        // Выполнение запроса в зависимости от типа БД
        return switch (databaseType) {
            case POSTGRESQL -> postgreSQLDatabaseClient.executeQuery(modifiedQuery, returnType, isSingleResult);
            case MONGODB -> mongoDBDatabaseClient.executeQuery("users", modifiedQuery, returnType, isSingleResult);
            default -> throw new UnsupportedOperationException("Неподдерживаемый тип базы данных");
        };
    }

    /**
     * Определяет тип возвращаемого значения метода.
     */
    private Class<?> getReturnType(Method method) {
        if (List.class.isAssignableFrom(method.getReturnType())) {
            ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
            return (Class<?>) genericReturnType.getActualTypeArguments()[0];
        }

        return method.getReturnType();
    }

    /**
     * Составляет map "имя параметра -> значение".
     */
    private Map<String, Object> extractParams(UUID requestId, Method method, Object[] args) {
        Map<String, Object> paramsMap = new LinkedHashMap<>();
        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        StringJoiner paramInfoJoiner = new StringJoiner("\n");

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = args[i];
            Annotation[] annotations = parameterAnnotations[i];

            String paramName = parameter.getName();
            boolean annotated = false;

            for (Annotation annotation : annotations) {
                if (annotation instanceof Param paramAnn) {
                    paramName = paramAnn.value();
                    annotated = true;
                    break;
                }
            }
            paramsMap.put(paramName, value);

            paramInfoJoiner
                    .add("Parameter #" + (i + 1) + " = '" + value + "'")
                    .add(annotated
                            ? "    ├── @Param: " + paramName + "\n    └── name variable: " + parameter.getName()
                            : "    └── name variable: " + parameter.getName());
        }

        logQuery(requestId, "Parameter details in dynamic query", paramInfoJoiner.toString());

        return paramsMap;
    }

    /**
     * Модифицирует запрос в зависимости от его типа.
     */
    private Statement modifyQuery(Statement query, ExpressionTreeEditor editor) {
        return switch (query) {
            case Select select -> modifySelect(select, editor);
            case Update update -> modifyUpdate(update, editor);
            case Insert insert -> modifyInsert(insert, editor);
            case Delete delete -> modifyDelete(delete, editor);
            default -> query;
        };
    }

    private Select modifySelect(Select select, ExpressionTreeEditor editor) {
        if (!(select instanceof PlainSelect plainSelect)) {
            return select;
        }

        if (plainSelect.getWhere() != null) {
            plainSelect.setWhere(editor.modify(plainSelect.getWhere()));
        }
        return select;
    }

    private Update modifyUpdate(Update update, ExpressionTreeEditor editor) {
        if (update.getWhere() != null) {
            update.setWhere(editor.modify(update.getWhere()));
        }

        List<Expression> modifiedExpressions = update.getExpressions().stream()
                .map(editor::modify)
                .toList();
        update.setExpressions(modifiedExpressions);

        return update;
    }

    private Insert modifyInsert(Insert insert, ExpressionTreeEditor editor) {
        if (insert.getValues() != null) {
            ExpressionList expressionList = insert.getValues().getExpressions();

            List<Expression> modifiedExpressions = ((List<Expression>) expressionList.getExpressions()).stream()
                    .map(editor::modify)
                    .toList();
            expressionList.setExpressions(modifiedExpressions);
        }

        if (insert.getSelect() != null) {
            modifySelect(insert.getSelect(), editor);
        }

        return insert;
    }

    private Delete modifyDelete(Delete delete, ExpressionTreeEditor editor) {
        if (delete.getWhere() != null) {
            delete.setWhere(editor.modify(delete.getWhere()));
        }
        return delete;
    }

    /**
     * Логирует запрос, если логирование включено.
     */
    private void logQuery(UUID requestId, String message, String query) {
        if (!logModifiedQueryEnabled) {
            return;
        }

        LOGGER.info("[requestId={}] {}:\n{}", requestId, message, query);
    }
}
