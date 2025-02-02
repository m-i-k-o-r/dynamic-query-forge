package com.koroli.dynamicqueryforge.executor;

import java.sql.*;
import java.util.Map;

public class PostgreSQLDatabaseClient {

    private final String url;
    private final String username;
    private final String password;

    public PostgreSQLDatabaseClient(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public ResultSet executeQuery(String sql, Map<String, Object> params) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int index = 1;
            for (Object param : params.values()) {
                statement.setObject(index++, param);
            }

            return statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при выполнении SQL-запроса", e);
        }
    }
}
