package com.capitalmarkets.trade.adapter.inbound.web.dto;

import java.util.List;

public record TradePageResponse(List<TradeResponse> content, long totalElements, int page, int size) {
}
