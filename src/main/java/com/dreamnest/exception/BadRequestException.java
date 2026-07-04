package com.dreamnest.exception;

/**
 * Thrown when a request is malformed or violates business rules.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
