package com.koroli.dynamicqueryforge.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.koroli.dynamicqueryforge.exception.ResultMappingException;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;

/**
 * Утилитарный класс для преобразования результатов запросов.
 */
@UtilityClass
public class ResultMappingUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Преобразует результат запроса в объекты указанного типа.
     *
     * @param resultSetData  список результатов запроса
     * @param resultClass    класс, в который нужно мапить результат
     * @param <T>            тип результата
     * @param isSingleResult флаг, указывающий, нужно ли вернуть единичный объект
     * @return объект или список объектов указанного типа
     * @throws RuntimeException если возникает ошибка при преобразовании
     */
    public static <T> Object mapResult(List<Map<String, Object>> resultSetData, Class<T> resultClass, boolean isSingleResult) {
        try {
            String json = objectMapper.writeValueAsString(resultSetData);

            if (resultSetData.isEmpty()) return null;

            if (isSingleResult) {
                return objectMapper.convertValue(resultSetData.getFirst(), resultClass);
            } else {
                CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, resultClass);
                return objectMapper.readValue(json, listType);
            }
        } catch (JsonProcessingException e) {
            throw new ResultMappingException("Ошибка при преобразовании результата запроса в объект " + resultClass.getName(), e);
        }
    }
}

