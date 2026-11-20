package com.capitalmarkets.trade.service.exception;

public class UnsupportedTradeFileFormatException extends RuntimeException {
    public UnsupportedTradeFileFormatException(String message) {
        super(message);
    }
}
