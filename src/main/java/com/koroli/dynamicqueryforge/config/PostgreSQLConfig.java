package com.koroli.dynamicqueryforge.config;

import com.koroli.dynamicqueryforge.executor.PostgreSQLDatabaseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "database.type", havingValue = "POSTGRESQL")
public class PostgreSQLConfig {

    @Bean
    public PostgreSQLDatabaseClient postgreSQLDatabaseClient(
            @Value("${database.postgresql.url}") String url,
            @Value("${database.postgresql.username}") String username,
            @Value("${database.postgresql.password}") String password) {
        return new PostgreSQLDatabaseClient(url, username, password);
    }
}
