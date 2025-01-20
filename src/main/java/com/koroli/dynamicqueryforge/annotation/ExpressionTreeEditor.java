package com.koroli.dynamicqueryforge.annotation;

import com.koroli.dynamicqueryforge.exception.UnsupportedParameterTypeException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class ExpressionTreeEditor {
    private final Map<String, Object> paramNameToValue;

    public ExpressionTreeEditor(Map<String, Object> paramNameToValue) {
        this.paramNameToValue = paramNameToValue;
    }

    /**
     * Рекурсивно обходит дерево Expression и возвращает новое дерево с учётом условий
     *
     * @param expression исходное выражение
     * @return модифицированное выражение или исходное, если оно не требует изменений
     */
    public Expression modify(Expression expression) {
        return switch (expression) {
            case AndExpression andExpr       -> handleAndExpression(andExpr);
            case OrExpression orExpr         -> handleOrExpression(orExpr);
            case LikeExpression likeExpr     -> handleLikeExpression(likeExpr);
            case Between betweenExpr         -> handleBetweenExpression(betweenExpr);
            case BinaryExpression binaryExpr -> handleBinaryExpression(binaryExpr);
            case InExpression inExpr         -> handleInExpression(inExpr);
            default -> expression;
        };
    }

    /**
     * Обрабатывает логическое AND выражение
     *
     * @param andExpr AND выражение для обработки
     * @return модифицированное выражение или null, если параметр равен null
     */
    private Expression handleAndExpression(AndExpression andExpr) {
        Expression left = modify(andExpr.getLeftExpression());
        Expression right = modify(andExpr.getRightExpression());

        if (left == null && right == null) return null;
        if (left == null) return right;
        if (right == null) return left;

        return new AndExpression(left, right);
    }

    /**
     * Обрабатывает логическое OR выражение
     *
     * @param orExpr OR выражение для обработки
     * @return модифицированное выражение или null, если параметр равен null
     */
    private Expression handleOrExpression(OrExpression orExpr) {
        Expression left = modify(orExpr.getLeftExpression());
        Expression right = modify(orExpr.getRightExpression());

        if (left == null && right == null) return null;
        if (left == null) return right;
        if (right == null) return left;

        return new OrExpression(left, right);
    }

    /**
     * Обрабатывает бинарное выражение (=, >, < и т.п.)
     *
     * @param binaryExpr бинарное выражение для обработки
     * @return модифицированное выражение или null, если параметр равен null
     */
    private Expression handleBinaryExpression(BinaryExpression binaryExpr) {
        Expression left = binaryExpr.getLeftExpression();
        Expression right = binaryExpr.getRightExpression();

        if (right instanceof JdbcNamedParameter param) {
            Object paramValue = paramNameToValue.get(param.getName());

            if (paramValue == null) {
                return null;
            }

            if (binaryExpr instanceof EqualsTo || binaryExpr instanceof GreaterThan ||
                    binaryExpr instanceof GreaterThanEquals || binaryExpr instanceof MinorThan ||
                    binaryExpr instanceof MinorThanEquals) {
                binaryExpr.setRightExpression(convertParameterValue(paramValue));
            }
        }

        if (left instanceof JdbcNamedParameter param) {
            Object paramValue = paramNameToValue.get(param.getName());

            if (paramValue == null) {
                return null;
            }

            if (binaryExpr instanceof EqualsTo || binaryExpr instanceof GreaterThan ||
                    binaryExpr instanceof GreaterThanEquals || binaryExpr instanceof MinorThan ||
                    binaryExpr instanceof MinorThanEquals) {
                binaryExpr.setLeftExpression(convertParameterValue(paramValue));
            }
        }

        return binaryExpr;
    }

    /**
     * Обрабатывает LIKE выражение
     *
     * @param likeExpr LIKE выражение для обработки
     * @return модифицированное выражение или null, если параметр равен null
     */
    private Expression handleLikeExpression(LikeExpression likeExpr) {
        Expression left = likeExpr.getLeftExpression();
        Expression right = likeExpr.getRightExpression();

        if (right instanceof JdbcNamedParameter param) {
            Object paramValue = paramNameToValue.get(param.getName());

            if (paramValue == null) {
                return null;
            }
            likeExpr.setRightExpression(convertParameterValue(paramValue));
        }

        if (left instanceof JdbcNamedParameter param) {
            Object paramValue = paramNameToValue.get(param.getName());

            if (paramValue == null) {
                return null;
            }
            likeExpr.setLeftExpression(convertParameterValue(paramValue));
        }

        return likeExpr;
    }

    /**
     * Обрабатывает IN выражение
     *
     * @param inExpr IN выражение для обработки
     * @return модифицированное выражение или null, если параметр равен null
     */
    private Expression handleInExpression(InExpression inExpr) {
        if (inExpr.getRightExpression() instanceof ExpressionList) {
            ExpressionList exprList = (ExpressionList) inExpr.getRightExpression();
            List<Expression> expressions = exprList.getExpressions();

            if (expressions != null) {
                expressions.removeIf(expr -> {
                    if (expr instanceof JdbcNamedParameter param) {
                        Object value = paramNameToValue.get(param.getName());
                        return value == null;
                    }
                    return false;
                });

                if (expressions.isEmpty()) {
                    return null;
                }

                exprList.setExpressions(expressions);
            }
        }
        return inExpr;
    }

    /**
     * Обрабатывает BETWEEN выражение
     *
     * @param betweenExpr BETWEEN выражение для обработки
     * @return модифицированное выражение или null, если параметр равен null
     */
    private Expression handleBetweenExpression(Between betweenExpr) {
        Expression start = betweenExpr.getBetweenExpressionStart();
        Expression end = betweenExpr.getBetweenExpressionEnd();

        if (start instanceof JdbcNamedParameter param) {
            Object paramValue = paramNameToValue.get(param.getName());

            if (paramValue == null) {
                return null;
            }
            betweenExpr.setBetweenExpressionStart(convertParameterValue(paramValue));
        }

        if (end instanceof JdbcNamedParameter param) {
            Object paramValue = paramNameToValue.get(param.getName());

            if (paramValue == null) {
                return null;
            }
            betweenExpr.setBetweenExpressionEnd(convertParameterValue(paramValue));
        }

        return betweenExpr;
    }

    /**
     * Конвертирует значение параметра в соответствующий тип Expression
     *
     * @param value значение параметра
     * @return объект Expression, соответствующий значению
     */
    private Expression convertParameterValue(Object value) {
        return switch (value) {
            // === Строковые значения ===
            case String str -> new StringValue(str);

            // === Числовые значения ===
            case Byte byteVal    -> new LongValue(byteVal);
            case Short shortVal  -> new LongValue(shortVal);
            case Integer intVal  -> new LongValue(intVal);
            case Long longVal    -> new LongValue(longVal);

            case Float floatVal      -> new DoubleValue(floatVal.toString());
            case Double doubleVal    -> new DoubleValue(doubleVal.toString());

            case BigDecimal decimal  -> new StringValue(decimal.toString());
            case BigInteger bigInt   -> new StringValue(bigInt.toString());

            case Number num -> new LongValue(num.toString());

            // === Булево значение ===
            case Boolean bool -> new BooleanValue(bool);

            // === Дата и время ===
            // java.sql
            case Date sqlDate -> new DateValue(sqlDate.toString());
            case Time sqlTime -> new TimeValue(sqlTime.toString());
            case Timestamp timestamp -> new TimestampValue(timestamp.toString());

            // java.time
            case LocalDate localDate -> new DateValue(localDate.toString());
            case LocalTime localTime -> new TimeValue(localTime.toString());
            case LocalDateTime localDateTime -> new TimestampValue(localDateTime.toString());

            // === Бинарные данные ===
            case byte[] bytes -> new HexValue(bytesToHex(bytes));

            default -> throw new UnsupportedParameterTypeException(
                    "Unsupported parameter type: " + value.getClass().getName()
            );
        };
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
