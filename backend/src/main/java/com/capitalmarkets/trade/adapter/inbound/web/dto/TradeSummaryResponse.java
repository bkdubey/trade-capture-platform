package com.capitalmarkets.trade.adapter.inbound.web.dto;

public record TradeSummaryResponse(long total, long valid, long invalid, long file, long kafka) {
}
