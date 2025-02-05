package com.koroli.dynamicqueryforge.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import net.sf.jsqlparser.statement.Statement;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgreSQLDatabaseClient {

    private final String url;
    private final String username;
    private final String password;
    private final ObjectMapper objectMapper;

    public PostgreSQLDatabaseClient(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
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
    public <T> Object executeQuery(Statement sql, Class<T> clazz, boolean isSingle) {
        List<Map<String, Object>> resultSet;

        try (PreparedStatement statement = DriverManager
                .getConnection(url, username, password)
                .prepareStatement(sql.toString())
        ) {
            try (ResultSet r = statement.executeQuery()) {
                resultSet = processResultSet(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при выполнении SQL-запроса", e);
        }

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

    /**
     * Обрабатывает ResultSet и возвращает список строк в виде мап.
     */
    private List<Map<String, Object>> processResultSet(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, resultSet.getObject(i));
            }
            results.add(row);
        }
        return results;
    }
}
