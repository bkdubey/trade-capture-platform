package com.capitalmarkets.trade.adapter.inbound.web.dto;

public record KafkaPublishResponse(long tradeId, String topic, boolean published) {
}
