package com.koroli.dynamicqueryforge.client;

import net.sf.jsqlparser.statement.Statement;

public interface DatabaseClient {

    /**
     * Выполняет SQL-запрос и возвращает результат.
     *
     * @param statement      SQL-запрос в виде объекта {@link Statement}
     * @param resultClass    класс, в который нужно мапить результат
     * @param isSingleResult флаг, указывающий, нужно ли вернуть единичный объект
     * @param <T>            тип результата
     * @return список объектов или единичный объект
     */
    <T> Object execute(Statement statement, Class<T> resultClass, boolean isSingleResult);
}