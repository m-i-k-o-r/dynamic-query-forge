package com.koroli.dynamicqueryforge.annotation;

import com.koroli.dynamicqueryforge.parser.SQLParser;
import com.koroli.dynamicqueryforge.parser.SQLTreeBuilder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс Proxy для обработки аннотации @Query на методах
 */
public class RepositoryProxy implements InvocationHandler {

    /**
     * Метод, который перехватывает вызовы методов интерфейса
     * <br/>
     * Если метод аннотирован @Query, обрабатывает SQL запрос
     *
     * @param proxy  прокси объект
     * @param method вызываемый метод
     * @param args   аргументы вызова метода
     * @return результат обработки (пока что результат это финальный SQL запрос)
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        Query queryAnnotation = method.getAnnotation(Query.class);
        if (queryAnnotation == null) {
            return null;
        }

        String originalQuery = queryAnnotation.value();
        Statement statement = SQLParser.parse(originalQuery);

        // Вывод "оригинального" дерева (временно здесь)
        System.out.println(SQLTreeBuilder.buildTree(statement));

        Map<String, Object> paramNameToValue = collectParamNameToValue(method, args);

        // Удаляем условия с параметрами, значение которых равно null
        Statement modifiedStatement = removeNullParameterConditions(statement, paramNameToValue);

        // Вывод "измененного" дерева (временно здесь)
        System.out.println(SQLTreeBuilder.buildTree(modifiedStatement));

        // Здесь должен выполняться запрос к бд, но пока что просто возвращаем его
        return modifiedStatement.toString();
    }


    /**
     * Собирает значения параметров метода в map "имя параметра -> значение"
     *
     * @param method метод, параметры которого анализируются
     * @param args   аргументы, переданные в метод
     * @return карта параметров
     */
    private Map<String, Object> collectParamNameToValue(Method method, Object[] args) {
        Map<String, Object> paramNameToValue = new HashMap<>();

        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object argumentValue = args[i];

            System.out.println("Parameter #" + (i + 1) + " = " + argumentValue);

            // Проверяем, есть ли у параметра аннотация @Param
            boolean hasParamAnnotation = false;
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof Param paramAnn) {
                    String paramName = paramAnn.value();
                    paramNameToValue.put(paramName, argumentValue);
                    hasParamAnnotation = true;

                    System.out.println("    ├── @Param: " + paramAnn.value());
                    break;
                }
            }

            // Если аннотации @Param нет, используем имя параметра как ключ
            if (!hasParamAnnotation) {
                paramNameToValue.put(parameter.getName(), argumentValue);
            }
            System.out.println("    └── name variable: " + parameter.getName());
        }

        return paramNameToValue;
    }

    /**
     * Удаляет условия из WHERE, если соответствующие параметры равны null или отсутствуют в map
     *
     * @param statement        исходный SQL запрос
     * @param paramNameToValue карта параметров
     * @return модифицированный SQL запрос
     */
    private Statement removeNullParameterConditions(Statement statement, Map<String, Object> paramNameToValue) {
        if (!(statement instanceof Select select)) {
            return statement;
        }

        if (!(select instanceof PlainSelect plainSelect)) {
            return statement;
        }

        Expression whereExp = plainSelect.getWhere();

        if (whereExp == null) {
            return statement;
        }

        // Удаляем условия, ссылающиеся на null-параметры
        Expression filteredWhere = filterWhereExpression(whereExp, paramNameToValue);
        plainSelect.setWhere(filteredWhere);

        return statement;
    }

    /**
     * Рекурсивно обрабатывает выражение WHERE для удаления подвыражений,
     * ссылающихся на null или отсутствующие параметры
     *
     * @param expression       выражение WHERE
     * @param paramNameToValue карта параметров
     * @return модифицированное выражение WHERE
     */
    private Expression filterWhereExpression(Expression expression, Map<String, Object> paramNameToValue) {
        ExpressionTreeEditor editor = new ExpressionTreeEditor(paramNameToValue);
        return editor.modify(expression);
    }
}
