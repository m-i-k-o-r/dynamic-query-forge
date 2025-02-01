package com.koroli.dynamicqueryforge.executors;

import com.mongodb.client.*;
import org.bson.Document;

public class MongoDBDatabaseClient {

    private final MongoClient mongoClient;
    private final String databaseName;

    public MongoDBDatabaseClient(String connectionString, String databaseName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.databaseName = databaseName;
    }

    public FindIterable<Document> executeQuery(String collectionName, Document query) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        return collection.find(query);
    }
}