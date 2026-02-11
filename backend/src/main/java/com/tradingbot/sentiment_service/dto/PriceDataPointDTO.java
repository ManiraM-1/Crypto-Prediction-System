package com.tradingbot.sentiment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class PriceDataPointDTO {
    private long time;
    private double price;
    private double volume;
}