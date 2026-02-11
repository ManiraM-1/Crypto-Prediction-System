package com.tradingbot.sentiment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class ParametersDTO {
    private String symbol;
    private int minutes;
    private String analysisTime;
}