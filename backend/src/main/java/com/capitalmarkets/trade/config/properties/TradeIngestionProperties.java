package com.capitalmarkets.trade.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ingestion")
public class TradeIngestionProperties {
    @Min(1)
    private int writeBatchSize = 1_000;

    @Min(1)
    private int maxCsvRows = 1_000_000;

    public int getWriteBatchSize() {
        return writeBatchSize;
    }

    public void setWriteBatchSize(int writeBatchSize) {
        this.writeBatchSize = writeBatchSize;
    }

    public int getMaxCsvRows() {
        return maxCsvRows;
    }

    public void setMaxCsvRows(int maxCsvRows) {
        this.maxCsvRows = maxCsvRows;
    }
}
