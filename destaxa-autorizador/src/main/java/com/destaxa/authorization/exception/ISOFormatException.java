package com.destaxa.authorization.exception;

public class ISOFormatException extends Exception {

    public ISOFormatException(String message) {
        super(message);
    }

    public ISOFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}