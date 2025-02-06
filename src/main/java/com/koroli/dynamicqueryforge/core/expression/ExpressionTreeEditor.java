package com.koroli.dynamicqueryforge.core.expression;

import com.koroli.dynamicqueryforge.utils.ExpressionConverterUtils;
import lombok.AllArgsConstructor;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Класс для редактирования дерева выражений {@code Expression}.
 * Позволяет модифицировать выражения с учётом значений параметров.
 */
@AllArgsConstructor
public class ExpressionTreeEditor {
    private final Map<String, Object> paramNameToValue;

    /**
     * Рекурсивно обходит дерево {@code Expression} и возвращает новое модифицированное выражение.
     *
     * @param expression исходное выражение
     * @return модифицированное выражение или исходное, если оно не требует изменений
     */
    public Expression modify(Expression expression) {
        return switch (expression) {
            case AndExpression andExpr          -> handleLogicalExpression(andExpr, AndExpression::new);
            case OrExpression orExpr            -> handleLogicalExpression(orExpr, OrExpression::new);
            case LikeExpression likeExpr        -> handleLikeExpression(likeExpr);
            case Between betweenExpr            -> handleBetweenExpression(betweenExpr);
            case BinaryExpression binaryExpr    -> handleBinaryExpression(binaryExpr);
            case InExpression inExpr            -> handleInExpression(inExpr);

            case ExpressionList exprList        -> modifyExpressionList(exprList);
            case JdbcNamedParameter jdbcParam   -> replaceParameter(jdbcParam);
            default -> expression;
        };
    }

    /**
     * Обрабатывает логическое выражение (AND или OR).
     *
     * @param expr        логическое выражение
     * @param constructor функция для создания нового выражения
     * @return модифицированное выражение или одно из подвыражений, если другое равно null
     */
    private Expression handleLogicalExpression(
            BinaryExpression expr,
            BiFunction<Expression, Expression, BinaryExpression> constructor
    ) {
        Expression left = modify(expr.getLeftExpression());
        Expression right = modify(expr.getRightExpression());

        if (left == null && right == null) return null;

        if (left == null) return right;
        if (right == null) return left;

        return constructor.apply(left, right);
    }

    /**
     * Обрабатывает выражение типа LIKE.
     *
     * @param likeExpr LIKE выражение
     * @return модифицированное выражение или null, если параметры отсутствуют
     */
    private Expression handleLikeExpression(LikeExpression likeExpr) {
        try {
            processParameter(
                    likeExpr::getRightExpression,
                    likeExpr::setRightExpression,
                    null);

            processParameter(
                    likeExpr::getLeftExpression,
                    likeExpr::setLeftExpression,
                    null);

        } catch (IllegalArgumentException e) {
            return null;
        }
        return likeExpr;
    }

    /**
     * Обрабатывает выражение типа BETWEEN.
     *
     * @param betweenExpr BETWEEN выражение
     * @return модифицированное выражение или null, если параметры отсутствуют
     */
    private Expression handleBetweenExpression(Between betweenExpr) {
        try {
            processParameter(
                    betweenExpr::getBetweenExpressionStart,
                    betweenExpr::setBetweenExpressionStart,
                    null);

            processParameter(
                    betweenExpr::getBetweenExpressionEnd,
                    betweenExpr::setBetweenExpressionEnd,
                    null);

        } catch (IllegalArgumentException e) {
            return null;
        }
        return betweenExpr;
    }

    /**
     * Обрабатывает бинарное выражение (=, >, < и т.п.).
     *
     * @param binaryExpr бинарное выражение
     * @return модифицированное выражение или null, если параметры отсутствуют
     */
    private Expression handleBinaryExpression(BinaryExpression binaryExpr) {
        try {
            processParameter(
                    binaryExpr::getRightExpression,
                    binaryExpr::setRightExpression,
                    this::isSupportedBinaryExpression);

            processParameter(
                    binaryExpr::getLeftExpression,
                    binaryExpr::setLeftExpression,
                    this::isSupportedBinaryExpression);

        } catch (IllegalArgumentException e) {
            return null;
        }
        return binaryExpr;
    }

    /**
     * Проверяет, относится ли бинарное выражение к поддерживаемым типам (=, >, < и т.п.).
     *
     * @param expression бинарное выражение
     * @return true, если выражение поддерживается, иначе false
     */
    private boolean isSupportedBinaryExpression(Expression expression) {
        return expression instanceof EqualsTo
                || expression instanceof GreaterThan
                || expression instanceof GreaterThanEquals
                || expression instanceof MinorThan
                || expression instanceof MinorThanEquals;
    }

    /**
     * Обрабатывает выражение типа IN.
     *
     * @param inExpr IN выражение
     * @return модифицированное выражение или null, если параметры отсутствуют
     */
    private Expression handleInExpression(InExpression inExpr) {
        if (inExpr.getRightExpression() instanceof ExpressionList exprList) {
            List<Expression> expressions = exprList.getExpressions();

            if (expressions != null) {
                expressions.removeIf(expr -> expr instanceof JdbcNamedParameter param
                        && paramNameToValue.get(param.getName()) == null);

                if (expressions.isEmpty()) return null;

                exprList.setExpressions(expressions);
            }
        }
        return inExpr;
    }

    /**
     * Обрабатывает список выражений, модифицируя каждое из них.
     *
     * @param exprList список выражений
     * @return модифицированный список выражений
     */
    private Expression modifyExpressionList(ExpressionList exprList) {
        List<Expression> expressions = (List<Expression>) exprList.getExpressions();

        List<Expression> modifiedExpressions = expressions.stream()
                .map(this::modify)
                .toList();

        exprList.setExpressions(modifiedExpressions);
        return exprList;
    }

    /**
     * Заменяет параметр {@code JdbcNamedParameter} на его значение.
     *
     * @param jdbcParam параметр
     * @return выражение с подставленным значением или исходный параметр, если значение отсутствует
     */
    private Expression replaceParameter(JdbcNamedParameter jdbcParam) {
        Object paramValue = paramNameToValue.get(jdbcParam.getName());
        return paramValue != null ? ExpressionConverterUtils.convertParameterValue(paramValue) : jdbcParam;
    }

    /**
     * Универсальный метод для обработки параметров в выражениях с дополнительной проверкой.
     *
     * @param getter      функция для получения текущего выражения
     * @param setter      функция для установки нового значения выражения
     * @param checkMethod функция для проверки выражения (может быть null)
     * @throws IllegalArgumentException если параметр отсутствует
     */
    private void processParameter(
            Supplier<Expression> getter,
            Consumer<Expression> setter,
            Predicate<Expression> checkMethod
    ) throws IllegalArgumentException {

        Expression expr = getter.get();
        if (expr instanceof JdbcNamedParameter param) {
            Object paramValue = paramNameToValue.get(param.getName());
            if (paramValue == null) {
                throw new IllegalArgumentException("paramValue is empty");
            }

            if (checkMethod == null || checkMethod.test((Expression) param.getParent())) {
                setter.accept(ExpressionConverterUtils.convertParameterValue(paramValue));
            }
        }
    }
}
