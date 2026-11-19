UPDATE trades
SET status = 'INVALID',
    validation_reason = CASE
        WHEN quantity <= 0 THEN 'Quantity must be greater than zero'
        ELSE 'Trade price must be greater than zero'
    END
WHERE quantity <= 0 OR trade_price <= 0;
