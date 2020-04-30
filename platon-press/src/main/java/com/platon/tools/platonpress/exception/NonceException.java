package com.platon.tools.platonpress.exception;

public class NonceException extends RuntimeException {

    public NonceException(String message) {
        super(message);
    }

    public NonceException(String message, Throwable cause) {
        super(message, cause);
    }
}
