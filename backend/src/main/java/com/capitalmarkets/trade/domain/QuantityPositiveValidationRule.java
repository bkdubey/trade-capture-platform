package com.capitalmarkets.trade.domain;

import java.util.Optional;

public class QuantityPositiveValidationRule implements TradeValidationRule {
    @Override
    public Optional<TradeValidationCode> validate(TradeValidationContext trade) {
        return trade.quantity() <= 0
                ? Optional.of(TradeValidationCode.QUANTITY_NOT_POSITIVE)
                : Optional.empty();
    }
}
