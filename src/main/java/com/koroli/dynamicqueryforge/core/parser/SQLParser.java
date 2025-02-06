package com.koroli.dynamicqueryforge.core.parser;

import com.koroli.dynamicqueryforge.exception.QueryExecutionException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

/**
 * Класс для парсинга SQL-запросов с использованием библиотеки {@code JSqlParser}.
 */
public class SQLParser {

    /**
     * Разбирает переданный SQL-запрос и возвращает объект SQL-выражения.
     *
     * @param sqlQuery строка с SQL-запросом
     * @return объект {@link Statement}, представляющий SQL-запрос
     * @throws QueryExecutionException если возникает ошибка при разборе SQL-запроса
     */
    public static Statement parse(String sqlQuery) {
        try {
            return CCJSqlParserUtil.parse(sqlQuery);
        } catch (JSQLParserException e) {
            throw new QueryExecutionException("Ошибка парсинга SQL-запроса: " + sqlQuery, e);
        }
    }
}
