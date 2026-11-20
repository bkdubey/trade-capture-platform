package com.capitalmarkets.trade.adapter.inbound.web.advice;

import jakarta.validation.ConstraintViolationException;
import com.capitalmarkets.trade.service.exception.InvalidTradeFileException;
import com.capitalmarkets.trade.service.exception.InvalidCsvException;
import com.capitalmarkets.trade.service.exception.TradeFileRowLimitExceededException;
import com.capitalmarkets.trade.service.exception.UnsupportedTradeFileFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(UnsupportedTradeFileFormatException.class)
    ProblemDetail handleUnsupportedFileFormat(UnsupportedTradeFileFormatException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
        detail.setTitle("Unsupported file format");
        return detail;
    }

    @ExceptionHandler(InvalidTradeFileException.class)
    ProblemDetail handleInvalidTradeFile(InvalidTradeFileException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid file");
        return detail;
    }

    @ExceptionHandler(InvalidCsvException.class)
    ProblemDetail handleInvalidCsv(InvalidCsvException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid CSV file");
        return detail;
    }

    @ExceptionHandler(TradeFileRowLimitExceededException.class)
    ProblemDetail handleFileRowLimit(TradeFileRowLimitExceededException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage());
        detail.setTitle("File row limit exceeded");
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid request");
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Trade record contains missing fields");
        detail.setTitle("Invalid trade record");
        return detail;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleResponseStatus(ResponseStatusException exception) {
        var detail = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
        detail.setTitle("Request failed");
        return detail;
    }
}
