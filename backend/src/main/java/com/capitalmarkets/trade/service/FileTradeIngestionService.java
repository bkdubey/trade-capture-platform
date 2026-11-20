package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import com.capitalmarkets.trade.service.exception.InvalidTradeFileException;
import com.capitalmarkets.trade.service.exception.UnsupportedTradeFileFormatException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class FileTradeIngestionService {
    private final Map<TradeFileFormat, TradeFileParser> parsers;
    private final TradeCaptureService captureService;

    public FileTradeIngestionService(List<TradeFileParser> parsers, TradeCaptureService captureService) {
        Map<TradeFileFormat, TradeFileParser> parserRegistry = new EnumMap<>(TradeFileFormat.class);
        for (TradeFileParser parser : parsers) {
            TradeFileParser previous = parserRegistry.putIfAbsent(parser.format(), parser);
            if (previous != null) {
                throw new IllegalStateException("Multiple parsers configured for " + parser.format());
            }
        }
        this.parsers = Map.copyOf(parserRegistry);
        this.captureService = captureService;
    }

    public int ingest(String filename, long fileSize, InputStream inputStream) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidTradeFileException("Uploaded file must have a filename");
        }
        if (fileSize <= 0) {
            throw new InvalidTradeFileException("Uploaded file must not be empty");
        }
        TradeFileFormat format = TradeFileFormat.fromFilename(filename)
                .orElseThrow(() -> new UnsupportedTradeFileFormatException(
                        "Unsupported file format. Supported formats: " + TradeFileFormat.supportedExtensions()));
        TradeFileParser parser = parsers.get(format);
        if (parser == null) {
            throw new UnsupportedTradeFileFormatException(
                    "No parser is configured for ." + format.extension() + " files");
        }
        List<TradeCaptureData> records = parser.parse(inputStream);
        return captureService.capture(records, TradeSource.FILE);
    }
}
