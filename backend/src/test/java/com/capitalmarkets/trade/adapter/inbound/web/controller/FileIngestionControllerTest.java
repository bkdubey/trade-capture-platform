package com.capitalmarkets.trade.adapter.inbound.web.controller;

import com.capitalmarkets.trade.service.FileTradeIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FileIngestionControllerTest {
    private final FileTradeIngestionService fileIngestionService = mock(FileTradeIngestionService.class);
    private final FileIngestionController controller = new FileIngestionController(fileIngestionService);

    @Test
    void delegatesCsvContentToFileIngestionService() throws IOException {
        var file = csv("BookingSystem,TradeID,InstrumentID,Quantity,TradePrice,TradeTimestamp\n");
        when(fileIngestionService.ingest(eq("trades.csv"), eq(file.getSize()), any())).thenReturn(2);

        var response = controller.upload(file);

        assertThat(response.accepted()).isEqualTo(2);
        verify(fileIngestionService).ingest(eq("trades.csv"), eq(file.getSize()), any());
    }

    private MockMultipartFile csv(String contents) {
        return new MockMultipartFile("file", "trades.csv", "text/csv",
                contents.getBytes(StandardCharsets.UTF_8));
    }
}
