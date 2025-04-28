package com.koroli.dynamicqueryforge.exception;

public class QueryParsingException extends DynamicQueryException {

    public QueryParsingException(String message) {
        super(message);
    }

    public QueryParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
