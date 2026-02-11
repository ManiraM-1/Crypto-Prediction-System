package com.tradingbot.sentiment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class HeadlineDTO {
    private String title;
    private String source;
    private String timestamp;
    private String sentiment;
    private double relevanceScore;
}