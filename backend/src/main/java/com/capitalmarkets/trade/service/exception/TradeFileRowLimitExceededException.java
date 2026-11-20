package com.capitalmarkets.trade.service.exception;

public class TradeFileRowLimitExceededException extends RuntimeException {
    public TradeFileRowLimitExceededException(int maxRows) {
        super("File contains more than the configured maximum of " + maxRows + " rows");
    }
}
