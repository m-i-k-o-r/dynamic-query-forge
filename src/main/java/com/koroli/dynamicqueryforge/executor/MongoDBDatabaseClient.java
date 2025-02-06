package com.koroli.dynamicqueryforge.executor;

import com.koroli.dynamicqueryforge.exception.QueryExecutionException;
import com.koroli.dynamicqueryforge.utils.ResultMappingUtils;
import com.koroli.queryconverter.converters.QueryConverter;
import com.koroli.queryconverter.exceptions.QueryConversionException;
import com.mongodb.client.*;
import net.sf.jsqlparser.statement.Statement;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Клиент для выполнения запросов к базе данных MongoDB.
 */
public class MongoDBDatabaseClient {

    @Autowired
    private QueryConverter queryConverter;

    private final MongoClient mongoClient;
    private final String databaseName;

    public MongoDBDatabaseClient(String connectionString, String databaseName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.databaseName = databaseName;
    }

    /**
     * Выполняет запрос к MongoDB и возвращает результат.
     *
     * @param collectionName имя коллекции в MongoDB
     * @param queryStatement SQL-запрос в виде объекта Statement
     * @param clazz          класс, в который нужно мапить результат
     * @param <T>            тип результата
     * @param isSingle       ожидается ли единичный результат
     * @return список объектов или единичный объект
     */
    public <T> Object executeQuery(String collectionName, Statement queryStatement, Class<T> clazz, boolean isSingle) {
        Document query = Document.parse(convertToMongoQuery(queryStatement));

        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        FindIterable<Document> documents = collection.find(query);

        List<Map<String, Object>> resultSetData = processMongoResults(documents);

        return ResultMappingUtils.mapResult(resultSetData, clazz, isSingle);
    }

    /**
     * Конвертирует SQL-запрос в MongoDB-запрос.
     */
    private String convertToMongoQuery(Statement statement) {
        try {
            return queryConverter.convert(statement);
        } catch (QueryConversionException e) {
            throw new QueryExecutionException("Ошибка при конвертации SQL в MongoDB запрос", e);
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
