package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import com.capitalmarkets.trade.service.exception.InvalidTradeFileException;
import com.capitalmarkets.trade.service.exception.UnsupportedTradeFileFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FileTradeIngestionServiceTest {
    private final TradeFileParser parser = mock(TradeFileParser.class);
    private final TradeCaptureService captureService = mock(TradeCaptureService.class);
    private FileTradeIngestionService service;

    @BeforeEach
    void setUp() {
        when(parser.format()).thenReturn(TradeFileFormat.CSV);
        service = new FileTradeIngestionService(List.of(parser), captureService);
    }

    @Test
    void parsesUploadThenCapturesAsFileSource() {
        var stream = new ByteArrayInputStream(new byte[0]);
        var trades = List.of(new TradeCaptureData(
                "MUREX", 1L, "IBM", 2L, BigDecimal.TEN, Instant.EPOCH));
        when(parser.parse(stream)).thenReturn(trades);
        when(captureService.capture(trades, TradeSource.FILE)).thenReturn(1);

        assertThat(service.ingest("trades.CSV", 1, stream)).isEqualTo(1);

        verify(parser).parse(stream);
        verify(captureService).capture(trades, TradeSource.FILE);
    }

    @Test
    void rejectsEmptyFilesAndUnsupportedExtensionsBeforeParsing() {
        assertThatThrownBy(() -> service.ingest("trades.csv", 0, new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(InvalidTradeFileException.class);
        assertThatThrownBy(() -> service.ingest("trades.xml", 10, new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(UnsupportedTradeFileFormatException.class)
                .hasMessageContaining(".csv")
                .hasMessageContaining(".txt");
        verify(parser, never()).parse(any());
        verifyNoInteractions(captureService);
    }

    @Test
    void routesTxtUploadsToTheTxtParser() {
        var txtParser = mock(TradeFileParser.class);
        when(txtParser.format()).thenReturn(TradeFileFormat.TXT);
        var txtService = new FileTradeIngestionService(List.of(parser, txtParser), captureService);
        var stream = new ByteArrayInputStream(new byte[0]);
        var trades = List.of(new TradeCaptureData(
                "MUREX", 2L, "IBM", 2L, BigDecimal.TEN, Instant.EPOCH));
        when(txtParser.parse(stream)).thenReturn(trades);
        when(captureService.capture(trades, TradeSource.FILE)).thenReturn(1);

        assertThat(txtService.ingest("trades.txt", 1, stream)).isEqualTo(1);

        verify(txtParser).parse(stream);
        verify(captureService).capture(trades, TradeSource.FILE);
    }

    @Test
    void rejectsDuplicateParserRegistrationForOneFormat() {
        var duplicateParser = mock(TradeFileParser.class);
        when(duplicateParser.format()).thenReturn(TradeFileFormat.CSV);

        assertThatThrownBy(() -> new FileTradeIngestionService(List.of(parser, duplicateParser), captureService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple parsers configured");
    }
}
