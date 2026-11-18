package com.stockpulse.ai;

public class UnavailableLlmException extends RuntimeException {

    public UnavailableLlmException(String message) {
        super(message);
    }

    public UnavailableLlmException(String message, Throwable cause) {
        super(message, cause);
    }
}