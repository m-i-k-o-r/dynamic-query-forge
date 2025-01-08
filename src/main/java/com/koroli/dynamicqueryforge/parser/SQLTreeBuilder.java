package com.koroli.dynamicqueryforge.parser;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.List;

import static com.koroli.dynamicqueryforge.utils.StringUtils.wrapInBrackets;
import static com.koroli.dynamicqueryforge.utils.StringUtils.wrapInCurlyBrackets;

public class SQLTreeBuilder {
    public static final String TAB =        "    ";
    public static final String LINE =       "│   ";
    public static final String BRANCH_OUT = "├── ";
    public static final String BRANCH_END = "└── ";

    /**
     * Создает дерево в виде строки для SQL-выражения
     *
     * @param statement SQL выражение
     * @return строковое представление дерева SQL-выражения
     */
    public static String buildTree(Statement statement) {
        PlainSelect plainSelect = (PlainSelect) statement;

        StringBuilder output = new StringBuilder();
        output.append(wrapInBrackets(plainSelect.toString())).append("\n");

        appendColumns(plainSelect.getSelectItems(), 1, output);
        appendFrom(plainSelect.getFromItem(), 1, output);
        appendWhere(plainSelect.getWhere(), 1, output);
        appendOrderBy(plainSelect.getOrderByElements(), 1, output);

        return output.toString();
    }

    /**
     * @param selectItems список колонок SELECT
     * @param indentLevel уровень отступа
     * @param sb          объект StringBuilder для вывода
     */
    private static void appendColumns(
            List<SelectItem<?>> selectItems,
            int indentLevel,
            StringBuilder sb
    ) {
        sb.append(TAB.repeat(indentLevel)).append("Columns ");
        sb.append(wrapInBrackets(selectItems));
        sb.append("\n");
    }

    /**
     * @param fromItem    элемент FROM части
     * @param indentLevel уровень отступа
     * @param sb          объект StringBuilder для вывода
     */
    private static void appendFrom(
            FromItem fromItem,
            int indentLevel,
            StringBuilder sb
    ) {
        sb.append(TAB.repeat(indentLevel)).append("FROM ");
        sb.append(wrapInBrackets(fromItem.toString()));
        sb.append("\n");
    }

    /**
     * @param expression  выражение WHERE части
     * @param indentLevel уровень отступа
     * @param sb          объект StringBuilder для вывода
     */
    private static void appendWhere(
            Expression expression,
            int indentLevel,
            StringBuilder sb
    ) {
        if (expression == null) return;

        String prefix = TAB.repeat(indentLevel);
        sb.append(prefix)
                .append("WHERE ")
                .append("\n");
        appendExpression(expression, prefix, true, sb);
    }

    /**
     * Добавляет выражение SQL запроса в вывод с учетом дерева
     *
     * @param expression выражение для добавления
     * @param prefix     текущий префикс строки
     * @param isLast     является ли это последним элементом в ветке
     * @param sb         объект StringBuilder для вывода
     */
    private static void appendExpression(
            Expression expression,
            String prefix,
            boolean isLast,
            StringBuilder sb
    ) {
        if (expression instanceof BinaryExpression binaryExpr) {
            // Добавляем оператор
            String operator = wrapInBrackets(binaryExpr.getStringExpression());
            appendIndented(operator, prefix, isLast, sb);

            // Добавляем левое поддерево
            appendExpression(binaryExpr.getLeftExpression(), prefix + (isLast ? TAB : LINE), false, sb);

            // Добавляем правое поддерево
            appendExpression(binaryExpr.getRightExpression(), prefix + (isLast ? TAB : LINE), true, sb);
        } else if (expression instanceof JdbcNamedParameter parameter) {
            // Добавляем именованный параметр
            appendIndented(wrapInCurlyBrackets(parameter.getName()), prefix, isLast, sb);
        } else {
            // Добавляем прочие выражения
            appendIndented(expression.toString(), prefix, isLast, sb);
        }
    }

    /**
     * Добавляет отформатированную строку в вывод с учетом отступов
     *
     * @param text   текст для добавления
     * @param prefix текущий префикс строки
     * @param isLast является ли это последним элементом в ветке
     * @param output объект StringBuilder для вывода
     */
    private static void appendIndented(
            String text,
            String prefix,
            boolean isLast,
            StringBuilder output
    ) {
        output.append(prefix);
        output.append(isLast ? BRANCH_END : BRANCH_OUT);
        output.append(text).append("\n");
    }

    /**
     * @param orderByElements список элементов ORDER BY
     * @param indentLevel     уровень отступа
     * @param sb              объект StringBuilder для вывода
     */
    private static void appendOrderBy(
            List<OrderByElement> orderByElements,
            int indentLevel,
            StringBuilder sb
    ) {
        if (orderByElements == null || orderByElements.isEmpty()) return;

        String prefix = TAB.repeat(indentLevel);

        sb.append(prefix)
                .append("ORDER BY")
                .append("\n");

        for (int i = 0; i < orderByElements.size(); i++) {
            OrderByElement orderBy = orderByElements.get(i);

            sb.append(prefix)
                    .append(i == orderByElements.size() - 1 ? BRANCH_END : BRANCH_OUT)
                    .append(orderBy)
                    .append("\n");
        }
    }
}
