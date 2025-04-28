package com.koroli.dynamicqueryforge.processing;

import com.koroli.dynamicqueryforge.cache.QueryCache;
import com.koroli.dynamicqueryforge.expression.ExpressionModifier;
import com.koroli.dynamicqueryforge.parser.SqlParser;
import com.koroli.dynamicqueryforge.util.DeepCloningUtils;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.koroli.dynamicqueryforge.annotation.Param;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Класс для обработки динамических SQL-запросов.
 */
@Component
public class QueryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryProcessor.class);

    @Value("${dynamic-query.log-queries:false}")
    private boolean logQueriesEnabled;

    private final QueryCache queryCache;

    @Autowired
    public QueryProcessor(QueryCache queryCache) {
        this.queryCache = queryCache;
    }

    /**
     * Обрабатывает SQL-запрос с учетом переданных параметров.
     * Парсит запрос, применяет кеширование и модифицирует запрос на основе параметров.
     *
     * @param sql        исходный SQL-запрос
     * @param parameters карта параметров и их значений
     * @return обработанный SQL-запрос в виде строки
     */
    public Statement processQuery(String sql, Map<String, Object> parameters) {
        // Нормализуем SQL для кеширования
        String normalizedSql = normalizeSql(sql);
        String cacheKey = generateCacheKey(normalizedSql);

        // Пытаемся получить запрос из кеша
        Statement statement = queryCache.get(cacheKey);

        // Если запроса нет в кеше, парсим его и сохраняем
        if (statement == null) {
            statement = SqlParser.parse(normalizedSql);
            queryCache.put(cacheKey, statement);
        }

        // Модификация запроса на основе параметров
        Statement modifiedStatement = applyParameters(statement, parameters);

        // Возвращаем модифицированный запрос в виде строки
        return modifiedStatement;
    }

    /**
     * Извлекает параметры из аргументов метода.
     *
     * @param requestId уникальный ID запроса для логирования
     * @param method    метод репозитория
     * @param args      аргументы метода
     * @return карта имен параметров и их значений
     */
    public Map<String, Object> extractParameters(UUID requestId, Method method, Object[] args) {
        Map<String, Object> paramsMap = new LinkedHashMap<>();

        if (args == null || args.length == 0) {
            return paramsMap;
        }

        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        StringJoiner paramInfoJoiner = new StringJoiner("\n");

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = args[i];
            Annotation[] annotations = parameterAnnotations[i];

            // По умолчанию используем имя параметра
            String paramName = parameter.getName();
            boolean annotated = false;

            // Ищем аннотацию @Param
            for (Annotation annotation : annotations) {
                if (annotation instanceof Param paramAnn) {
                    paramName = paramAnn.value();
                    annotated = true;
                    break;
                }
            }

            // Добавляем параметр в карту
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
     * Применяет параметры к SQL-запросу, модифицируя его при необходимости.
     *
     * @param statement  запрос в виде объекта Statement
     * @param parameters карта параметров
     * @return модифицированный запрос
     */
    private Statement applyParameters(Statement statement, Map<String, Object> parameters) {
        // Создаем редактор выражений для указанных параметров
        ExpressionModifier editor = new ExpressionModifier(parameters);

        // Создаем копию запроса для модификаций
        Statement cloneStatement = DeepCloningUtils.clone(statement);

        // Модифицируем запрос в зависимости от его типа
        return switch (cloneStatement) {
            case Select select -> modifySelect(select, editor);
            case Update update -> modifyUpdate(update, editor);
            case Insert insert -> modifyInsert(insert, editor);
            case Delete delete -> modifyDelete(delete, editor);
            default -> cloneStatement;
        };
    }

    /**
     * Модифицирует SELECT-запрос на основе параметров.
     */
    private Select modifySelect(Select select, ExpressionModifier editor) {
        if (select instanceof PlainSelect plainSelect && plainSelect.getWhere() != null) {
            Expression where = plainSelect.getWhere();
            plainSelect.setWhere(editor.modify(where));
        }
        return select;
    }

    /**
     * Модифицирует UPDATE-запрос на основе параметров.
     */
    private Update modifyUpdate(Update update, ExpressionModifier editor) {
        if (update.getWhere() != null) {
            update.setWhere(editor.modify(update.getWhere()));
        }

        List<Expression> modifiedExpressions = update.getExpressions().stream()
                .map(editor::modify)
                .toList();
        update.setExpressions(modifiedExpressions);

        return update;
    }

    /**
     * Модифицирует INSERT-запрос на основе параметров.
     */
    private Insert modifyInsert(Insert insert, ExpressionModifier editor) {
        if (insert.getValues() != null) {
            ExpressionList expressions = insert.getValues().getExpressions();
            List<Expression> modifiedExpressions = ((List<Expression>) expressions.getExpressions()).stream()
                    .map(editor::modify)
                    .toList();
            expressions.setExpressions(modifiedExpressions);
        }

        if (insert.getSelect() != null) {
            modifySelect(insert.getSelect(), editor);
        }

        return insert;
    }

    /**
     * Модифицирует DELETE-запрос на основе параметров.
     */
    private Delete modifyDelete(Delete delete, ExpressionModifier editor) {
        if (delete.getWhere() != null) {
            delete.setWhere(editor.modify(delete.getWhere()));
        }
        return delete;
    }

    /**
     * Нормализует SQL-запрос для кеширования.
     */
    private String normalizeSql(String sql) {
        return sql.trim().replaceAll("\\s+", " ");
    }

    /**
     * Генерирует ключ для кеширования SQL-запроса.
     */
    private String generateCacheKey(String sql) {
        return "sql:" + sql.hashCode();
    }

    /**
     * Логирует запрос, если логирование включено.
     */
    private void logQuery(UUID requestId, String message, String query) {
        if (!logQueriesEnabled) {
            return;
        }
        LOGGER.info("[requestId={}] {}:\n{}", requestId, message, query);
    }
}