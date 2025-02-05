package com.koroli.dynamicqueryforge.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.koroli.queryconverter.converters.QueryConverter;
import com.koroli.queryconverter.exceptions.QueryConversionException;
import com.mongodb.client.*;
import net.sf.jsqlparser.statement.Statement;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MongoDBDatabaseClient {

    @Autowired
    private QueryConverter queryConverter;

    private final MongoClient mongoClient;
    private final String databaseName;
    private final ObjectMapper objectMapper;

    public MongoDBDatabaseClient(String connectionString, String databaseName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.databaseName = databaseName;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Универсальный метод для выполнения запроса.
     * Если метод возвращает список, возвращается `List<T>`.
     * Если метод ожидает единичный объект, возвращается `T`.
     *
     * @param sql      SQL-запрос
     * @param clazz    класс, в который нужно мапить результат
     * @param <T>      тип результата
     * @param isSingle ожидается ли единичный результат
     * @return список объектов или единичный объект
     */
    public <T> Object executeQuery(String collectionName, Statement sql, Class<T> clazz, boolean isSingle) {
        Document query = Document.parse(convertToMongoQuery(sql));

        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        FindIterable<Document> documents = collection.find(query);

        List<Map<String, Object>> resultSet = processMongoResults(documents);

        try {
            String json = objectMapper.writeValueAsString(resultSet);

            if (resultSet.isEmpty()) return null;

            if (isSingle) {
                return objectMapper.convertValue(resultSet.getFirst(), clazz);
            } else {
                CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
                return objectMapper.readValue(json, listType);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при преобразовании результата запроса в объект " + clazz.getName(), e);
        }
    }

    private String convertToMongoQuery(Statement statement) {
        try {
            return queryConverter.convert(statement);
        } catch (QueryConversionException e) {
            throw new RuntimeException("Ошибка при конвертации SQL в MongoDB запрос", e);
        }
    }

    /**
     * Преобразует результаты Mongo-запроса в список мап.
     */
    private List<Map<String, Object>> processMongoResults(Iterable<Document> documents) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Document document : documents) {
            results.add(document);
        }
        return results;
    }
}
