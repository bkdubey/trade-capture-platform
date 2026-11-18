package com.capitalmarkets.trade.adapter.inbound.web;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsIllegalArgumentsToBadRequest() {
        var response = handler.handleBadRequest(new IllegalArgumentException("Invalid range"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("Invalid range");
    }

    @Test
    void mapsCsvValidationFailuresToSafeError() {
        var response = handler.handleConstraintViolation(new ConstraintViolationException(Set.of()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Invalid CSV record");
    }

    @Test
    void preservesExpectedHttpStatusForFileErrors() {
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Too many rows"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(response.getDetail()).isEqualTo("Too many rows");
    }
}
