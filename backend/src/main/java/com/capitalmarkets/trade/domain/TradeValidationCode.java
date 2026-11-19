package com.capitalmarkets.trade.domain;

public enum TradeValidationCode {
    QUANTITY_NOT_POSITIVE("Quantity must be greater than zero"),
    TRADE_PRICE_NOT_POSITIVE("Trade price must be greater than zero"),
    INSTRUMENT_NOT_SUPPORTED("Instrument is not supported");

    private final String description;

    TradeValidationCode(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
