package com.capitalmarkets.trade.service.exception;

public class InvalidTradeFileException extends RuntimeException {
    public InvalidTradeFileException(String message) {
        super(message);
    }

    public InvalidTradeFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
