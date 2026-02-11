package com.tradingbot.sentiment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class SentimentStatsDTO {
    private int totalArticles;
    private int positiveCount;
    private int negativeCount;
    private int neutralCount;
    private String sentimentTrend;
}