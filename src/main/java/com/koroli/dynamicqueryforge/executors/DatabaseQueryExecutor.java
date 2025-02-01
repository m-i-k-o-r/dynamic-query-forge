package com.koroli.dynamicqueryforge.executors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatabaseQueryExecutor {

    @Autowired(required = false)
    private PostgreSQLDatabaseClient postgreSQLDatabaseClient;

    @Autowired(required = false)
    private MongoDBDatabaseClient mongoDBDatabaseClient;

    @Autowired
    private DatabaseType databaseType;

    public Object executeQuery(String query, Map<String, Object> params) {
        if (databaseType == DatabaseType.POSTGRESQL && postgreSQLDatabaseClient != null) {
            return postgreSQLDatabaseClient.executeQuery(query, params);
        } else if (databaseType == DatabaseType.MONGODB && mongoDBDatabaseClient != null) {
            return mongoDBDatabaseClient.executeQuery("users", Document.parse(query));
        } else {
            throw new UnsupportedOperationException("Тип базы данных не поддерживается или не настроен");
        }
    }
}
