package com.koroli.dynamicqueryforge.utils;

import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Утилитарный класс для работы со строками,
 * предоставляющий методы для ...
 * (оборачивания текста и списков в квадратные или фигурные скобки)
 */
@UtilityClass
public class StringUtils {
    private static final String LEFT_BRACKET = "[";
    private static final String RIGHT_BRACKET = "]";

    /**
     * Оборачивает текст в квадратные скобки
     *
     * @param text текст для оборачивания
     * @return строка, обернутая в квадратные скобки
     */
    public static String wrapInBrackets(String text) {
        return LEFT_BRACKET + text + RIGHT_BRACKET;
    }

    /**
     * Оборачивает элементы списка в квадратные скобки, разделяя их запятыми
     *
     * @param list список элементов для оборачивания
     * @return строка, представляющая список, обернутый в квадратные скобки
     */
    public static String wrapInBrackets(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(LEFT_BRACKET);
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(", ");
            } else {
                sb.append(RIGHT_BRACKET);
            }
        }
        return sb.toString();
    }

    private static final String LEFT_CURLY_BRACKET = "{";
    private static final String RIGHT_CURLY_BRACKET = "}";

    /**
     * Оборачивает текст в фигурные скобки
     *
     * @param text текст для оборачивания
     * @return строка, обернутая в фигурные скобки
     */
    public static String wrapInCurlyBrackets(String text) {
        return LEFT_CURLY_BRACKET + text + RIGHT_CURLY_BRACKET;
    }
}
