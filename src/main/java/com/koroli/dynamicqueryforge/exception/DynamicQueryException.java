package com.koroli.dynamicqueryforge.exception;

public class DynamicQueryException extends RuntimeException {

    public DynamicQueryException(String message) {
        super(message);
    }

    public DynamicQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
