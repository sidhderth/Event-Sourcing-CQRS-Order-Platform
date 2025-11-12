package com.orderplatform.domain;

/**
 * Exception thrown when an operation is attempted on an order in an invalid state.
 */
public class InvalidOrderStateException extends RuntimeException {
    
    public InvalidOrderStateException(String message) {
        super(message);
    }
    
    public InvalidOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
