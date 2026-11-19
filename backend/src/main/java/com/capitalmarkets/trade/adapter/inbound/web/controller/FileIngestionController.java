package com.capitalmarkets.trade.adapter.inbound.web.controller;

import com.capitalmarkets.trade.adapter.inbound.web.dto.FileIngestionResponse;
import com.capitalmarkets.trade.service.FileTradeIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ingestion")
public class FileIngestionController {
    private final FileTradeIngestionService fileIngestionService;

    public FileIngestionController(FileTradeIngestionService fileIngestionService) {
        this.fileIngestionService = fileIngestionService;
    }

    @PostMapping("/files")
    public FileIngestionResponse upload(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file must not be empty");
        }
        if (file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CSV files are supported");
        }
        try {
            return new FileIngestionResponse(fileIngestionService.ingest(file.getInputStream()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read CSV file", exception);
        }
    }
}
