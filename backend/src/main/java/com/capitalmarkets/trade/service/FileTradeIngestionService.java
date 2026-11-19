package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class FileTradeIngestionService {
    private final CsvTradeParser parser;
    private final TradeCaptureService captureService;

    public FileTradeIngestionService(CsvTradeParser parser, TradeCaptureService captureService) {
        this.parser = parser;
        this.captureService = captureService;
    }

    public int ingest(InputStream inputStream) {
        List<TradeCaptureData> records = parser.parse(inputStream);
        return captureService.capture(records, TradeSource.FILE);
    }
}
