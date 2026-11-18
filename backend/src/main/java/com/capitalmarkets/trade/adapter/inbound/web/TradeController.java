package com.capitalmarkets.trade.adapter.inbound.web;

import com.capitalmarkets.trade.application.TradeQuery;
import com.capitalmarkets.trade.application.TradeReportingService;
import com.capitalmarkets.trade.application.TradeSortDirection;
import com.capitalmarkets.trade.application.TradeSortField;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@RequestMapping("/api/v1")
public class TradeController {
    private final TradeReportingService reportingService;

    public TradeController(TradeReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/trades")
    public TradePageResponse trades(
            @RequestParam(required = false) TradeStatus status,
            @RequestParam(required = false) TradeSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size,
            @RequestParam(required = false) @Size(max = 128) String search,
            @RequestParam(defaultValue = "TRADE_TIMESTAMP") TradeSortField sortBy,
            @RequestParam(defaultValue = "DESC") TradeSortDirection sortDirection) {
        var result = reportingService.find(
                new TradeQuery(status, source, from, to, page, size, search, sortBy, sortDirection));
            var content = result.content().stream().map(TradeResponse::from).toList();
            return new TradePageResponse(content, result.totalElements(), result.page(), result.size());
    }

    @GetMapping("/reports/summary")
    public TradeSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) Instant to) {
        var result = reportingService.summarize(from, to);
        return new TradeSummaryResponse(result.total(), result.valid(), result.invalid(), result.file(), result.kafka());
    }

    public record TradePageResponse(List<TradeResponse> content, long totalElements, int page, int size) {
    }

    public record TradeSummaryResponse(long total, long valid, long invalid, long file, long kafka) {
    }
}
