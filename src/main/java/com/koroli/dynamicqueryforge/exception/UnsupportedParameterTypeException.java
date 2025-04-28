package com.koroli.dynamicqueryforge.exception;

public class UnsupportedParameterTypeException extends DynamicQueryException {

    public UnsupportedParameterTypeException(String message) {
        super(message);
    }

    public UnsupportedParameterTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
