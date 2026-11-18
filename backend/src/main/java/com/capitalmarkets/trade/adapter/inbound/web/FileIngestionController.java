package com.capitalmarkets.trade.adapter.inbound.web;

import com.capitalmarkets.trade.application.TradeCaptureService;
import com.capitalmarkets.trade.application.TradeRecord;
import com.capitalmarkets.trade.domain.TradeSource;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ingestion")
public class FileIngestionController {
    private static final List<String> HEADERS = List.of(
            "BookingSystem", "TradeID", "InstrumentID", "Quantity", "TradePrice", "TradeTimestamp");
    private static final int MAX_ROWS = 1_000_000;

    private final TradeCaptureService captureService;
    private final Validator validator;

    public FileIngestionController(TradeCaptureService captureService, Validator validator) {
        this.captureService = captureService;
        this.validator = validator;
    }

    @PostMapping("/files")
    public IngestionResponse upload(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file must not be empty");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CSV files are supported");
        }
        List<TradeRecord> records = parse(file);
        return new IngestionResponse(captureService.capture(records, TradeSource.FILE));
    }

    private List<TradeRecord> parse(MultipartFile file) {
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                     .setTrim(true).setIgnoreEmptyLines(true).get().parse(reader)) {
            if (!parser.getHeaderNames().equals(HEADERS)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "CSV header must be exactly: " + String.join(",", HEADERS));
            }
            List<TradeRecord> records = new ArrayList<>();
            for (CSVRecord row : parser) {
                if (records.size() >= MAX_ROWS) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "CSV files are limited to " + MAX_ROWS + " rows");
                }
                TradeRecord record;
                try {
                    record = new TradeRecord(
                            row.get("BookingSystem"),
                            Long.parseLong(row.get("TradeID")),
                            row.get("InstrumentID"),
                            Long.parseLong(row.get("Quantity")),
                            new BigDecimal(row.get("TradePrice")),
                            parseTimestamp(row.get("TradeTimestamp")));
                } catch (IllegalArgumentException | java.time.DateTimeException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid CSV value on row " + row.getRecordNumber(), exception);
                }
                var violations = validator.validate(record);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
                records.add(record);
            }
            return records;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read CSV file", exception);
        }
    }

    private Instant parseTimestamp(String value) {
        return OffsetDateTime.parse(value).toInstant();
    }

    public record IngestionResponse(int accepted) {
    }
}
