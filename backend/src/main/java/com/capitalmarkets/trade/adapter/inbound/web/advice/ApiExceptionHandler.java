package com.capitalmarkets.trade.adapter.inbound.web.advice;

import jakarta.validation.ConstraintViolationException;
import com.capitalmarkets.trade.service.exception.CsvRowLimitExceededException;
import com.capitalmarkets.trade.service.exception.InvalidCsvException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidCsvException.class)
    ProblemDetail handleInvalidCsv(InvalidCsvException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid CSV file");
        return detail;
    }

    @ExceptionHandler(CsvRowLimitExceededException.class)
    ProblemDetail handleCsvRowLimit(CsvRowLimitExceededException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage());
        detail.setTitle("CSV row limit exceeded");
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
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "CSV record contains missing fields");
        detail.setTitle("Invalid CSV record");
        return detail;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleResponseStatus(ResponseStatusException exception) {
        var detail = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
        detail.setTitle("Request failed");
        return detail;
    }
}
