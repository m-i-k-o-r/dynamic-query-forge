package com.koroli.dynamicqueryforge.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.koroli.dynamicqueryforge.exception.QueryExecutionException;

import java.util.List;
import java.util.Map;

/**
 * Утилитарный класс для преобразования результатов запросов.
 */
public class ResultMappingUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Преобразует результат запроса в объекты указанного типа.
     *
     * @param resultSetData список результатов запроса
     * @param clazz         класс, в который нужно мапить результат
     * @param isSingle      флаг, указывающий, нужно ли вернуть единичный объект
     * @param <T>           тип результата
     * @return объект или список объектов указанного типа
     * @throws RuntimeException если возникает ошибка при преобразовании
     */
    public static <T> Object mapResult(List<Map<String, Object>> resultSetData, Class<T> clazz, boolean isSingle) {
        try {
            String json = objectMapper.writeValueAsString(resultSetData);

            if (resultSetData.isEmpty()) return null;

            if (isSingle) {
                return objectMapper.convertValue(resultSetData.getFirst(), clazz);
            } else {
                CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
                return objectMapper.readValue(json, listType);
            }
        } catch (JsonProcessingException e) {
            throw new QueryExecutionException("Ошибка при преобразовании результата запроса в объект " + clazz.getName(), e);
        }
    }
}

