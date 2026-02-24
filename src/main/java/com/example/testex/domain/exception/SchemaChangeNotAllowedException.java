package com.example.testex.domain.exception;

public class SchemaChangeNotAllowedException extends RuntimeException {

    public SchemaChangeNotAllowedException(String message) {
        super(message);
    }
}
