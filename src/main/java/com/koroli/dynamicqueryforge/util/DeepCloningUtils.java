package com.koroli.dynamicqueryforge.util;

import com.rits.cloning.Cloner;
import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.statement.Statement;

/**
 * Утилитарный класс для выполнения глубокого клонирования объектов.
 * Использует библиотеку Cloner для создания глубоких копий.
 */
@UtilityClass
public class DeepCloningUtils {

    private static final Cloner cloner = new Cloner();

    /**
     * Создает глубокую копию объекта Statement.
     *
     * @param statement объект Statement, который требуется клонировать
     * @return глубокая копия переданного объекта Statement
     */
    public static Statement clone(Statement statement) {
        return cloner.deepClone(statement);
    }
}
