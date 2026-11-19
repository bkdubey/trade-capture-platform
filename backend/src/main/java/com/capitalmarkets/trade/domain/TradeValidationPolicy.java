package com.capitalmarkets.trade.domain;

import java.util.List;
import java.util.Objects;

public class TradeValidationPolicy {
    private final List<TradeValidationRule> rules;

    public TradeValidationPolicy(List<TradeValidationRule> rules) {
        this.rules = List.copyOf(rules);
        if (this.rules.isEmpty()) {
            throw new IllegalArgumentException("At least one trade validation rule is required");
        }
    }

    public ValidationResult validate(TradeValidationContext trade) {
        for (TradeValidationRule rule : rules) {
            var failure = Objects.requireNonNull(rule.validate(trade),
                    "Validation rule must return an Optional");
            if (failure.isPresent()) {
                return new ValidationResult(TradeStatus.INVALID, failure.get());
            }
        }
        return new ValidationResult(TradeStatus.VALID, null);
    }

    public record ValidationResult(TradeStatus status, TradeValidationCode code) {
        public String reason() {
            return code == null ? null : code.description();
        }
    }
}
