package com.koroli.dynamicqueryforge.utils;

import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Утилитарный класс для работы со строками, предоставляющий методы
 * для оборачивания текста и списков в различные типы скобок.
 */
@UtilityClass
public class StringUtils {
    private static final String BRACKET_LEFT        = "[";
    private static final String BRACKET_RIGHT       = "]";
    private static final String CURLY_BRACKET_LEFT  = "{";
    private static final String CURLY_BRACKET_RIGHT = "}";

    /**
     * Оборачивает текст в квадратные скобки.
     *
     * @param text текст для оборачивания
     * @return строка, обернутая в квадратные скобки
     */
    public static String wrapInBrackets(String text) {
        return wrapText(text, BRACKET_LEFT, BRACKET_RIGHT);
    }

    /**
     * Оборачивает элементы списка в квадратные скобки, разделяя их запятыми.
     *
     * @param list список элементов для оборачивания
     * @return строка, представляющая список элементов, обернутый в квадратные скобки
     */
    public static String wrapInBrackets(List<?> list) {
        StringBuilder sb = new StringBuilder(BRACKET_LEFT);
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(BRACKET_RIGHT);
        return sb.toString();
    }

    /**
     * Оборачивает текст в фигурные скобки.
     *
     * @param text текст для оборачивания
     * @return строка, обернутая в фигурные скобки
     */
    public static String wrapInCurlyBrackets(String text) {
        return wrapText(text, CURLY_BRACKET_LEFT, CURLY_BRACKET_RIGHT);
    }

    /**
     * Оборачивает текст в указанные скобки.
     *
     * @param text  текст для оборачивания
     * @param left  строка, представляющая открывающую скобку
     * @param right строка, представляющая закрывающую скобку
     * @return строка, обернутая в указанные скобки
     */
    private static String wrapText(String text, String left, String right) {
        return left + text + right;
    }
}
