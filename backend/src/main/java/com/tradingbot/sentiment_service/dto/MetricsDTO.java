package com.tradingbot.sentiment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class MetricsDTO {
    private double predictedChange;
    private double predictedPrice;
    private double currentPrice;
    private double supportLevel;
    private double resistanceLevel;
}