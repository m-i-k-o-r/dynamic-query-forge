package com.koroli.dynamicqueryforge.config;

import com.koroli.dynamicqueryforge.executor.MongoDBDatabaseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "database.type", havingValue = "MONGODB")
public class MongoDBConfig {

    @Bean
    public MongoDBDatabaseClient mongoDBDatabaseClient(
            @Value("${database.mongodb.connectionString}") String connectionString,
            @Value("${database.mongodb.databaseName}") String databaseName) {
        return new MongoDBDatabaseClient(connectionString, databaseName);
    }
}