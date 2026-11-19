package com.capitalmarkets.trade.adapter.outbound.persistence;

import com.capitalmarkets.trade.application.TradeQuery;
import com.capitalmarkets.trade.application.TradeRepository;
import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcTradeRepository implements TradeRepository {
    private static final String SELECT_COLUMNS = """
            id, booking_system, trade_id, instrument_id, quantity, trade_price, trade_timestamp,
            source_type, status, validation_reason, received_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final RowMapper<Trade> rowMapper = (rs, row) -> new Trade(
            rs.getObject("id", UUID.class),
            rs.getString("booking_system"),
            rs.getLong("trade_id"),
            rs.getString("instrument_id"),
            rs.getLong("quantity"),
            rs.getBigDecimal("trade_price"),
            rs.getTimestamp("trade_timestamp").toInstant(),
            TradeSource.valueOf(rs.getString("source_type")),
            TradeStatus.valueOf(rs.getString("status")),
            rs.getString("validation_reason"),
            rs.getTimestamp("received_at").toInstant()
    );

    public JdbcTradeRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public void saveBatch(List<Trade> trades) {
        if (trades.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO trades (id, booking_system, trade_id, instrument_id, quantity, trade_price,
                    trade_timestamp, source_type, status, validation_reason, received_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_type, booking_system, trade_id, trade_timestamp) DO NOTHING
                """, trades, 1_000, (statement, trade) -> {
            statement.setObject(1, trade.id());
            statement.setString(2, trade.bookingSystem());
            statement.setLong(3, trade.tradeId());
            statement.setString(4, trade.instrumentId());
            statement.setLong(5, trade.quantity());
            statement.setBigDecimal(6, trade.tradePrice());
            statement.setTimestamp(7, Timestamp.from(trade.tradeTimestamp()));
            statement.setString(8, trade.source().name());
            statement.setString(9, trade.status().name());
            statement.setString(10, trade.validationReason());
            statement.setTimestamp(11, Timestamp.from(trade.receivedAt()));
        });
    }

    @Override
    public TradePage find(TradeQuery query) {
        var parameters = new MapSqlParameterSource();
        List<String> conditions = addTimeConditions(query.from(), query.to(), parameters);
        if (query.status() != null) {
            conditions.add("status = :status");
            parameters.addValue("status", query.status().name());
        }
        if (query.source() != null) {
            conditions.add("source_type = :sourceType");
            parameters.addValue("sourceType", query.source().name());
        }
        if (query.search() != null) {
            conditions.add("""
                    (POSITION(LOWER(:search) IN LOWER(booking_system)) > 0
                     OR POSITION(LOWER(:search) IN LOWER(CAST(trade_id AS TEXT))) > 0
                     OR POSITION(LOWER(:search) IN LOWER(instrument_id)) > 0
                     OR POSITION(LOWER(:search) IN LOWER(COALESCE(validation_reason, ''))) > 0)
                    """);
            parameters.addValue("search", query.search());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        Long total = namedJdbcTemplate.queryForObject("SELECT COUNT(*) FROM trades" + where, parameters, Long.class);
        parameters.addValue("limit", query.size());
        parameters.addValue("offset", (long) query.page() * query.size());
        List<Trade> rows = namedJdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM trades" + where
                        + " ORDER BY " + query.sortBy().column() + " " + query.sortDirection().name()
                        + ", id " + query.sortDirection().name() + " LIMIT :limit OFFSET :offset",
                parameters, rowMapper);
        return new TradePage(rows, total == null ? 0 : total, query.page(), query.size());
    }

    @Override
    public TradeSummary summarize(Instant from, Instant to) {
        var parameters = new MapSqlParameterSource();
        List<String> conditions = addTimeConditions(from, to, parameters);
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return namedJdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = 'VALID') AS valid,
                       COUNT(*) FILTER (WHERE status = 'INVALID') AS invalid,
                       COUNT(*) FILTER (WHERE source_type = 'FILE') AS file,
                       COUNT(*) FILTER (WHERE source_type = 'KAFKA') AS kafka
                FROM trades
                """ + where, parameters, (rs, row) -> new TradeSummary(
                rs.getLong("total"), rs.getLong("valid"), rs.getLong("invalid"),
                rs.getLong("file"), rs.getLong("kafka")));
    }

    private List<String> addTimeConditions(
            Instant from, Instant to, MapSqlParameterSource parameters) {
        List<String> conditions = new ArrayList<>();
        if (from != null) {
            conditions.add("trade_timestamp >= :from");
            parameters.addValue("from", Timestamp.from(from));
        }
        if (to != null) {
            conditions.add("trade_timestamp <= :to");
            parameters.addValue("to", Timestamp.from(to));
        }
        return conditions;
    }
}
