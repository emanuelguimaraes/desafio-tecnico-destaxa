package com.destaxa.api.exception;

public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
