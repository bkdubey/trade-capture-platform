package com.capitalmarkets.trade.adapter.inbound.web;

import com.capitalmarkets.trade.application.TradeCaptureService;
import com.capitalmarkets.trade.application.TradeRecord;
import com.capitalmarkets.trade.domain.TradeSource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class FileIngestionControllerTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    private final TradeCaptureService captureService = mock(TradeCaptureService.class);
    private final FileIngestionController controller = new FileIngestionController(captureService, VALIDATOR);

    @AfterAll
    static void closeValidator() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void parsesCsvRowsAndSendsThemToCommonCaptureUseCase() {
        var file = csv("BookingSystem,TradeID,InstrumentID,Quantity,TradePrice,TradeTimestamp\n"
                + "MUREX,100,IBM,0,10.25,2026-01-01T10:00:00Z\n"
                + "CALYPSO,101,AAPL,-10,11.50,2026-01-01T10:01:00+05:30\n");
        when(captureService.capture(anyList(), eq(TradeSource.FILE))).thenReturn(2);

        var response = controller.upload(file);

        assertThat(response.accepted()).isEqualTo(2);
        var records = org.mockito.ArgumentCaptor.<List<TradeRecord>>captor();
        verify(captureService).capture(records.capture(), eq(TradeSource.FILE));
        assertThat(records.getValue()).extracting(TradeRecord::tradeId).containsExactly(100L, 101L);
        assertThat(records.getValue().get(0).quantity()).isZero();
    }

    @Test
    void acceptsHeaderOnlyCsvAndPersistsNoRecords() {
        when(captureService.capture(anyList(), eq(TradeSource.FILE))).thenReturn(0);

        assertThat(controller.upload(csv("BookingSystem,TradeID,InstrumentID,Quantity,TradePrice,TradeTimestamp\n"))
                .accepted()).isZero();
        verify(captureService).capture(List.of(), TradeSource.FILE);
    }

    @Test
    void rejectsEmptyFilesAndNonCsvFiles() {
        var empty = new MockMultipartFile("file", "trades.csv", "text/csv", new byte[0]);
        var wrongType = new MockMultipartFile("file", "trades.txt", "text/csv",
                "header\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> controller.upload(empty)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.upload(wrongType)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(captureService);
    }

    @Test
    void rejectsUnexpectedHeaderAndMalformedValues() {
        assertThatThrownBy(() -> controller.upload(csv("wrong,header\n")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CSV header");
        assertThatThrownBy(() -> controller.upload(csv(
                "BookingSystem,TradeID,InstrumentID,Quantity,TradePrice,TradeTimestamp\n"
                        + "MUREX,not-a-number,IBM,1,1.0,2026-01-01T10:00:00Z\n")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("row 1");
    }

    @Test
    void rejectsMissingRequiredFieldsButDoesNotRejectBusinessInvalidValues() {
        assertThatThrownBy(() -> controller.upload(csv(
                "BookingSystem,TradeID,InstrumentID,Quantity,TradePrice,TradeTimestamp\n"
                        + ",100,IBM,1,1.0,2026-01-01T10:00:00Z\n")))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        verifyNoInteractions(captureService);
    }

    private MockMultipartFile csv(String contents) {
        return new MockMultipartFile("file", "trades.csv", "text/csv",
                contents.getBytes(StandardCharsets.UTF_8));
    }
}
