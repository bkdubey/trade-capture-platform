package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FileTradeIngestionServiceTest {
    private final CsvTradeParser parser = mock(CsvTradeParser.class);
    private final TradeCaptureService captureService = mock(TradeCaptureService.class);
    private final FileTradeIngestionService service = new FileTradeIngestionService(parser, captureService);

    @Test
    void parsesUploadThenCapturesAsFileSource() {
        var stream = new ByteArrayInputStream(new byte[0]);
        var trades = List.of(new TradeCaptureData(
                "MUREX", 1L, "IBM", 2L, BigDecimal.TEN, Instant.EPOCH));
        when(parser.parse(stream)).thenReturn(trades);
        when(captureService.capture(trades, TradeSource.FILE)).thenReturn(1);

        assertThat(service.ingest(stream)).isEqualTo(1);

        verify(parser).parse(stream);
        verify(captureService).capture(trades, TradeSource.FILE);
    }
}
