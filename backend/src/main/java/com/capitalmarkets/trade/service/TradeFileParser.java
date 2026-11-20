package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.service.dto.TradeCaptureData;

import java.io.InputStream;
import java.util.List;

public interface TradeFileParser {
    TradeFileFormat format();

    List<TradeCaptureData> parse(InputStream inputStream);
}
