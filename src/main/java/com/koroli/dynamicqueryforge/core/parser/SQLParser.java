package com.koroli.dynamicqueryforge.core.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

public class SQLParser {

    /**
     * Разбирает переданный SQL запрос и возвращает объект SQL выражения
     *
     * @param sqlQuery строка SQL запроса
     * @return объект Statement (SQL запрос)
     * @throws RuntimeException если возникает ошибка при разборе SQL запроса
     */
    public static Statement parse(String sqlQuery) {
        try {
            return CCJSqlParserUtil.parse(sqlQuery);
        } catch (JSQLParserException e) {
            throw new RuntimeException("Ошибка парсинга SQL-запроса", e);
        }
    }
}
