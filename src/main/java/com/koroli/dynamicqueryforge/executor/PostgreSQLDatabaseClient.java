package com.koroli.dynamicqueryforge.executor;

import com.koroli.dynamicqueryforge.exception.QueryExecutionException;
import com.koroli.dynamicqueryforge.utils.ResultMappingUtils;
import lombok.AllArgsConstructor;
import net.sf.jsqlparser.statement.Statement;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Клиент для выполнения SQL-запросов к базе данных PostgreSQL.
 */
@AllArgsConstructor
public class PostgreSQLDatabaseClient {

    private final String url;
    private final String username;
    private final String password;

    /**
     * Выполняет SQL-запрос и возвращает результат.
     *
     * @param queryStatement SQL-запрос в виде объекта Statement
     * @param clazz          класс, в который нужно мапить результат
     * @param <T>            тип результата
     * @param isSingle       ожидается ли единичный результат
     * @return список объектов или единичный объект
     */
    public <T> Object executeQuery(Statement queryStatement, Class<T> clazz, boolean isSingle) {
        List<Map<String, Object>> resultSetData;

        try (PreparedStatement statement = DriverManager
                .getConnection(url, username, password)
                .prepareStatement(queryStatement.toString())
        ) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSetData = processResultSet(resultSet);
            }
        } catch (SQLException e) {
            throw new QueryExecutionException("Ошибка при выполнении SQL-запроса", e);
        }

        return ResultMappingUtils.mapResult(resultSetData, clazz, isSingle);
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
