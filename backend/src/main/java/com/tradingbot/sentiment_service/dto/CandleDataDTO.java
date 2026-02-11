package com.tradingbot.sentiment_service.dto;

public class CandleDataDTO {
    public final long timestamp;
    public final double open;
    public final double high;
    public final double low;
    public final double close;
    public final double volume;

    public CandleDataDTO(long timestamp, double open, double high, double low, double close, double volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
}