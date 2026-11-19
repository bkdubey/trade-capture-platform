package com.capitalmarkets.trade.adapter.inbound.web.controller;

import com.capitalmarkets.trade.service.FileTradeIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FileIngestionControllerTest {
    private final FileTradeIngestionService fileIngestionService = mock(FileTradeIngestionService.class);
    private final FileIngestionController controller = new FileIngestionController(fileIngestionService);

    @Test
    void delegatesCsvContentToFileIngestionService() throws IOException {
        var file = csv("BookingSystem,TradeID,InstrumentID,Quantity,TradePrice,TradeTimestamp\n");
        when(fileIngestionService.ingest(any())).thenReturn(2);

        var response = controller.upload(file);

        assertThat(response.accepted()).isEqualTo(2);
        verify(fileIngestionService).ingest(any());
    }

    @Test
    void rejectsEmptyFilesAndNonCsvFiles() {
        var empty = new MockMultipartFile("file", "trades.csv", "text/csv", new byte[0]);
        var wrongType = new MockMultipartFile("file", "trades.txt", "text/csv",
                "header\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> controller.upload(empty)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.upload(wrongType)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(fileIngestionService);
    }

    private MockMultipartFile csv(String contents) {
        return new MockMultipartFile("file", "trades.csv", "text/csv",
                contents.getBytes(StandardCharsets.UTF_8));
    }
}
