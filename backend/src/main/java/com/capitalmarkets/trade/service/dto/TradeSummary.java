package com.capitalmarkets.trade.service.dto;

public record TradeSummary(long total, long valid, long invalid, long file, long kafka) {
}
