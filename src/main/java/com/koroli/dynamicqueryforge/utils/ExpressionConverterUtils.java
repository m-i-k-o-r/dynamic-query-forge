package com.koroli.dynamicqueryforge.utils;

import com.koroli.dynamicqueryforge.exception.UnsupportedParameterTypeException;
import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.expression.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@UtilityClass
public class ExpressionConverterUtils {

    /**
     * Конвертирует значение параметра в соответствующий тип Expression
     *
     * @param value значение параметра
     * @return объект Expression, соответствующий значению
     */
    public static Expression convertParameterValue(Object value) {
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
