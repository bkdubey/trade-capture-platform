package com.capitalmarkets.trade.domain;

import java.util.Optional;

public class TradePricePositiveValidationRule implements TradeValidationRule {
    @Override
    public Optional<TradeValidationCode> validate(TradeValidationContext trade) {
        return trade.tradePrice().signum() <= 0
                ? Optional.of(TradeValidationCode.TRADE_PRICE_NOT_POSITIVE)
                : Optional.empty();
    }
}
