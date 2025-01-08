package com.koroli.dynamicqueryforge.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

public class SQLParser {

    /**
     * Парсит SQL запрос в объект Statement
     *
     * @param sqlQuery строка SQL запроса
     * @return объект Statement (SQL запрос), или null, если запрос некорректен
     */
    public static Statement parse(String sqlQuery) {
        try {
            return CCJSqlParserUtil.parse(sqlQuery);
        } catch (JSQLParserException e) {
            System.err.println("Ошибка парсинга SQL запроса: " + e.getMessage());
            return null;
        }
    }
}
