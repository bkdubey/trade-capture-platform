package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.config.properties.TradeIngestionProperties;
import com.capitalmarkets.trade.service.dto.TradeFileRow;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import com.capitalmarkets.trade.service.exception.InvalidTradeFileException;
import com.capitalmarkets.trade.service.exception.TradeFileRowLimitExceededException;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TxtTradeParser implements TradeFileParser {
    private static final List<String> HEADERS = List.of(
            "BookingSystem", "TradeID", "InstrumentID", "Quantity", "TradePrice", "TradeTimestamp");

    private final Validator validator;
    private final TradeIngestionProperties properties;

    public TxtTradeParser(Validator validator, TradeIngestionProperties properties) {
        this.validator = validator;
        this.properties = properties;
    }

    @Override
    public TradeFileFormat format() {
        return TradeFileFormat.TXT;
    }

    @Override
    public List<TradeCaptureData> parse(InputStream inputStream) {
        try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setDelimiter('|')
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {
            if (!parser.getHeaderNames().equals(HEADERS)) {
                throw new InvalidTradeFileException("TXT header must be exactly: " + String.join("|", HEADERS));
            }

            List<TradeCaptureData> records = new ArrayList<>();
            for (CSVRecord row : parser) {
                if (records.size() >= properties.getMaxFileRows()) {
                    throw new TradeFileRowLimitExceededException(properties.getMaxFileRows());
                }
                if (row.size() != HEADERS.size()) {
                    throw new InvalidTradeFileException(
                            "TXT row " + row.getRecordNumber() + " must contain exactly " + HEADERS.size() + " fields");
                }
                try {
                    var txtRow = new TradeFileRow(
                            row.get("BookingSystem"),
                            Long.parseLong(row.get("TradeID")),
                            row.get("InstrumentID"),
                            Long.parseLong(row.get("Quantity")),
                            new BigDecimal(row.get("TradePrice")),
                            OffsetDateTime.parse(row.get("TradeTimestamp")).toInstant());
                    var violations = validator.validate(txtRow);
                    if (!violations.isEmpty()) {
                        throw new jakarta.validation.ConstraintViolationException(violations);
                    }
                    records.add(txtRow.toCaptureData());
                } catch (IllegalArgumentException | java.time.DateTimeException exception) {
                    throw new InvalidTradeFileException("Invalid TXT value on row " + row.getRecordNumber(), exception);
                }
            }
            return records;
        } catch (IOException exception) {
            throw new InvalidTradeFileException("Unable to read TXT file", exception);
        }
    }
}
