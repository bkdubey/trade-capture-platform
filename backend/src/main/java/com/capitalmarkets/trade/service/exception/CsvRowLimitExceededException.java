package com.capitalmarkets.trade.service.exception;

public class CsvRowLimitExceededException extends RuntimeException {
    public CsvRowLimitExceededException(int maxRows) {
        super("CSV files are limited to " + maxRows + " rows");
    }
}
