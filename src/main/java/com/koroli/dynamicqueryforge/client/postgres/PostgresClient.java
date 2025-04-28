package com.koroli.dynamicqueryforge.client.postgres;

import com.koroli.dynamicqueryforge.client.DatabaseClient;
import com.koroli.dynamicqueryforge.exception.QueryProcessingException;
import com.koroli.dynamicqueryforge.util.ResultMappingUtils;
import net.sf.jsqlparser.statement.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Клиент для выполнения SQL-запросов к базе данных PostgreSQL.
 */
@Component
public class PostgresClient implements DatabaseClient {

    private final DataSource dataSource;

    @Autowired
    public PostgresClient(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> Object execute(Statement queryStatement, Class<T> resultClass, boolean isSingleResult) {
        List<Map<String, Object>> resultSetData;
        try (PreparedStatement statement = dataSource.getConnection()
                .prepareStatement(queryStatement.toString());
        ) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSetData = processResultSet(resultSet);
            }
        } catch (SQLException e) {
            throw new QueryProcessingException("Ошибка при выполнении SQL-запроса", e);
        }
        return ResultMappingUtils.mapResult(resultSetData, resultClass, isSingleResult);
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
