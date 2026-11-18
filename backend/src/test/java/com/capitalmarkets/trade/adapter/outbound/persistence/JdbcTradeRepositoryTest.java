package com.capitalmarkets.trade.adapter.outbound.persistence;

import com.capitalmarkets.trade.application.TradeQuery;
import com.capitalmarkets.trade.application.TradeRepository;
import com.capitalmarkets.trade.application.TradeSortDirection;
import com.capitalmarkets.trade.application.TradeSortField;
import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JdbcTradeRepositoryTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final NamedParameterJdbcTemplate namedJdbc = mock(NamedParameterJdbcTemplate.class);
    private final JdbcTradeRepository repository = new JdbcTradeRepository(jdbc, namedJdbc);

    @Test
    void savesBatchesAndSkipsEmptyWrites() throws Exception {
        var timestamp = Instant.parse("2026-01-01T10:00:00Z");
        var trade = new Trade(UUID.randomUUID(), "MUREX", 12, "IBM", 2, new BigDecimal("10.50"),
                timestamp, TradeSource.FILE, TradeStatus.VALID, null, timestamp);
        var statement = mock(PreparedStatement.class);
        when(jdbc.batchUpdate(anyString(), anyList(), eq(1_000), any(ParameterizedPreparedStatementSetter.class)))
                .thenAnswer(invocation -> {
                    ParameterizedPreparedStatementSetter<Trade> setter = invocation.getArgument(3);
                    setter.setValues(statement, trade);
                    return new int[][]{{1}};
                });

        repository.saveBatch(List.of(trade));
        repository.saveBatch(List.of());

        verify(statement).setString(8, "FILE");
        verify(statement).setString(9, "VALID");
        verify(statement).setString(10, null);
        verify(jdbc, times(1)).batchUpdate(anyString(), anyList(), eq(1_000),
                any(ParameterizedPreparedStatementSetter.class));
    }

    @Test
    void queriesFilteredPageAndMapsRows() throws Exception {
        var row = tradeRow();
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(1L);
        when(namedJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Trade> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(row, 0));
                });
        var from = Instant.parse("2026-01-01T00:00:00Z");

        var result = repository.find(new TradeQuery(
                TradeStatus.INVALID, TradeSource.KAFKA, from, null, 1, 25, null,
                TradeSortField.TRADE_ID, TradeSortDirection.ASC));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(trade -> {
            assertThat(trade.id()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
            assertThat(trade.source()).isEqualTo(TradeSource.KAFKA);
            assertThat(trade.status()).isEqualTo(TradeStatus.INVALID);
        });
        verify(namedJdbc).queryForObject(contains("source_type = :sourceType"),
                any(MapSqlParameterSource.class), eq(Long.class));
        verify(namedJdbc).query(contains("ORDER BY trade_id ASC"),
                any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void searchesAcrossTradeFieldsAndUsesAllowListedSortOrder() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(0L);
        when(namedJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        repository.find(new TradeQuery(null, null, null, null, 0, 50, "ibm",
                TradeSortField.INSTRUMENT_ID, TradeSortDirection.ASC));

        verify(namedJdbc).queryForObject(contains("POSITION(LOWER(:search)"),
                any(MapSqlParameterSource.class), eq(Long.class));
        verify(namedJdbc).query(contains("ORDER BY instrument_id ASC"),
                any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void summarizesAllSourcesAndStatusCountsForTimeRange() throws Exception {
        var row = mock(ResultSet.class);
        when(row.getLong("total")).thenReturn(13L);
        when(row.getLong("valid")).thenReturn(10L);
        when(row.getLong("invalid")).thenReturn(3L);
        when(row.getLong("file")).thenReturn(8L);
        when(row.getLong("kafka")).thenReturn(5L);
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<TradeRepository.TradeSummary> mapper = invocation.getArgument(2);
                    return mapper.mapRow(row, 0);
                });

        var result = repository.summarize(
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"));

        assertThat(result).isEqualTo(new TradeRepository.TradeSummary(13, 10, 3, 8, 5));
        verify(namedJdbc).queryForObject(contains("COUNT(*) FILTER"),
                any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void queriesAndSummarizesWithoutOptionalFilters() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(0L);
        when(namedJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        var page = repository.find(new TradeQuery(null, null, null, null, 0, 50));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        verify(namedJdbc).queryForObject(eq("SELECT COUNT(*) FROM trades"),
                any(MapSqlParameterSource.class), eq(Long.class));
    }

    private ResultSet tradeRow() throws Exception {
        var row = mock(ResultSet.class);
        var now = Timestamp.from(Instant.parse("2026-01-01T10:00:00Z"));
        when(row.getObject("id", UUID.class))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        when(row.getString("booking_system")).thenReturn("MUREX");
        when(row.getLong("trade_id")).thenReturn(42L);
        when(row.getString("instrument_id")).thenReturn("IBM");
        when(row.getLong("quantity")).thenReturn(0L);
        when(row.getBigDecimal("trade_price")).thenReturn(new BigDecimal("10.50"));
        when(row.getTimestamp("trade_timestamp")).thenReturn(now);
        when(row.getString("source_type")).thenReturn("KAFKA");
        when(row.getString("status")).thenReturn("INVALID");
        when(row.getString("validation_reason")).thenReturn("Quantity must not be zero");
        when(row.getTimestamp("received_at")).thenReturn(now);
        return row;
    }
}
