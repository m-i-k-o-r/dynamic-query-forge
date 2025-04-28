package com.koroli.dynamicqueryforge.processing;

import com.koroli.dynamicqueryforge.annotation.Query;
import com.koroli.dynamicqueryforge.client.postgres.PostgresClient;
import net.sf.jsqlparser.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс-перехватчик методов репозитория.
 */
@Component
public class RepositoryMethodInterceptor implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryMethodInterceptor.class);

    @Value("${dynamic-query.log-queries:false}")
    private boolean logQueriesEnabled;

    private final QueryProcessor queryProcessor;
    private final PostgresClient queryExecutor;

    @Autowired
    public RepositoryMethodInterceptor(QueryProcessor queryProcessor, PostgresClient queryExecutor) {
        this.queryProcessor = queryProcessor;
        this.queryExecutor = queryExecutor;
    }

    /**
     * Перехватывает вызовы методов и обрабатывает методы с аннотацией @Query.
     *
     * @param proxy  прокси-объект
     * @param method вызываемый метод
     * @param args   аргументы вызова метода
     * @return результат выполнения запроса или null, если метод не аннотирован @Query
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // Генерируем уникальный ID запроса для логирования
        UUID requestId = UUID.randomUUID();

        // Проверяем наличие аннотации Query
        Query queryAnnotation = method.getAnnotation(Query.class);
        if (queryAnnotation == null) return null;

        // Извлекаем оригинальный SQL-запрос из аннотации
        String originalSql = queryAnnotation.value();
        logQuery(requestId, "Original SQL Query", originalSql);

        // Получаем параметры запроса на основе аргументов метода
        Map<String, Object> parameters = queryProcessor.extractParameters(requestId, method, args);

        // Обрабатываем SQL-запрос, применяя динамические параметры
        Statement processedSql = queryProcessor.processQuery(originalSql, parameters);
        logQuery(requestId, "Processed SQL Query", processedSql.toString());

        // Получаем информацию о запрашиваемом типе результата
        Class<?> resultType = getReturnType(method);
        boolean isSingleResult = !List.class.isAssignableFrom(method.getReturnType());

        // Выполняем запрос и возвращаем результат
        return queryExecutor.execute(processedSql, resultType, isSingleResult);
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
     * Логирует запрос, если логирование включено.
     */
    private void logQuery(UUID requestId, String message, String query) {
        if (!logQueriesEnabled) {
            return;
        }
        LOGGER.info("[requestId={}] {}:\n{}", requestId, message, query);
    }
}