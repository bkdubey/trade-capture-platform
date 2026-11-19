package com.capitalmarkets.trade.domain;

import java.util.Optional;

public interface TradeValidationRule {
    Optional<TradeValidationCode> validate(TradeValidationContext trade);
}
