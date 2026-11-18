package com.capitalmarkets.trade.application;

public enum TradeSortField {
    TRADE_TIMESTAMP("trade_timestamp"),
    BOOKING_SYSTEM("booking_system"),
    TRADE_ID("trade_id"),
    INSTRUMENT_ID("instrument_id"),
    QUANTITY("quantity"),
    TRADE_PRICE("trade_price"),
    STATUS("status");

    private final String column;

    TradeSortField(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }
}
