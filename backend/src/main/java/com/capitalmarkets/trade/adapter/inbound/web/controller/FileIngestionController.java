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

@RestController
@RequestMapping("/api/v1/ingestion")
public class FileIngestionController {
    private final FileTradeIngestionService fileIngestionService;

    public FileIngestionController(FileTradeIngestionService fileIngestionService) {
        this.fileIngestionService = fileIngestionService;
    }

    @PostMapping("/files")
    public FileIngestionResponse upload(@RequestPart("file") MultipartFile file) {
        try {
            return new FileIngestionResponse(fileIngestionService.ingest(
                    file.getOriginalFilename(), file.getSize(), file.getInputStream()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file", exception);
        }
    }
}
