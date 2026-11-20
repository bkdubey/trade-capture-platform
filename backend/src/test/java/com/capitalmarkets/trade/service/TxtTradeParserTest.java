package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.config.properties.TradeIngestionProperties;
import com.capitalmarkets.trade.service.exception.InvalidTradeFileException;
import com.capitalmarkets.trade.service.exception.TradeFileRowLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxtTradeParserTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final TradeIngestionProperties properties = new TradeIngestionProperties();
    private final TxtTradeParser parser = new TxtTradeParser(VALIDATOR_FACTORY.getValidator(), properties);

    @AfterAll
    static void closeValidator() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void parsesPipeDelimitedRowsAndRetainsBusinessInvalidValues() {
        var result = parser.parse(txt("BookingSystem|TradeID|InstrumentID|Quantity|TradePrice|TradeTimestamp\n"
                + "MUREX|100|IBM|0|10.25|2026-01-01T10:00:00Z\n"
                + "CALYPSO|101|AAPL|-10|11.50|2026-01-01T10:01:00+05:30\n"));

        assertThat(result).extracting(record -> record.tradeId()).containsExactly(100L, 101L);
        assertThat(result.get(0).quantity()).isZero();
    }

    @Test
    void rejectsWrongHeaderMalformedValuesMissingFieldsAndExcessRows() {
        assertThatThrownBy(() -> parser.parse(txt("wrong|header\n")))
                .isInstanceOf(InvalidTradeFileException.class)
                .hasMessageContaining("TXT header");
        assertThatThrownBy(() -> parser.parse(txt(
                "BookingSystem|TradeID|InstrumentID|Quantity|TradePrice|TradeTimestamp\n"
                        + "MUREX|not-a-number|IBM|1|1.0|2026-01-01T10:00:00Z\n")))
                .isInstanceOf(InvalidTradeFileException.class)
                .hasMessageContaining("row 1");
        assertThatThrownBy(() -> parser.parse(txt(
                "BookingSystem|TradeID|InstrumentID|Quantity|TradePrice|TradeTimestamp\n"
                        + "MUREX|100|IBM|1|1.0|2026-01-01T10:00:00Z|EXTRA\n")))
                .isInstanceOf(InvalidTradeFileException.class)
                .hasMessageContaining("exactly 6 fields");
        assertThatThrownBy(() -> parser.parse(txt(
                "BookingSystem|TradeID|InstrumentID|Quantity|TradePrice|TradeTimestamp\n"
                        + "|100|IBM|1|1.0|2026-01-01T10:00:00Z\n")))
                .isInstanceOf(ConstraintViolationException.class);

        properties.setMaxFileRows(1);
        assertThatThrownBy(() -> parser.parse(txt(
                "BookingSystem|TradeID|InstrumentID|Quantity|TradePrice|TradeTimestamp\n"
                        + "MUREX|100|IBM|1|1.0|2026-01-01T10:00:00Z\n"
                        + "MUREX|101|IBM|1|1.0|2026-01-01T10:01:00Z\n")))
                .isInstanceOf(TradeFileRowLimitExceededException.class);
    }

    private ByteArrayInputStream txt(String contents) {
        return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    }
}
