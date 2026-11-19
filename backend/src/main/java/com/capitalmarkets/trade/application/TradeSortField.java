package com.capitalmarkets.trade.application;

public enum TradeSortField {
    TRADE_TIMESTAMP("trade_timestamp"),
    BOOKING_SYSTEM("booking_system"),
    TRADE_ID("trade_id"),
    INSTRUMENT_ID("instrument_id"),
    QUANTITY("quantity"),
    TRADE_PRICE("trade_price"),
    STATUS("status"),
    SOURCE("source_type"),
    VALIDATION_REASON("validation_reason"),
    RECEIVED_AT("received_at");

    private final String column;

    TradeSortField(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }
}
