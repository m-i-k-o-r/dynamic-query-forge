package com.koroli.dynamicqueryforge.exception;

public class UnsupportedParameterTypeException extends RuntimeException {
    public UnsupportedParameterTypeException(String message) {
        super(message);
    }

    public UnsupportedParameterTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
