package com.capitalmarkets.trade.service.dto;

import com.capitalmarkets.trade.domain.Trade;

import java.util.List;

public record TradePage(List<Trade> content, long totalElements, int page, int size) {
}
