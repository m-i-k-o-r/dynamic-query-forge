package com.koroli.dynamicqueryforge.exception;

public class QueryProcessingException extends DynamicQueryException {

    public QueryProcessingException(String message) {
        super(message);
    }

    public QueryProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
