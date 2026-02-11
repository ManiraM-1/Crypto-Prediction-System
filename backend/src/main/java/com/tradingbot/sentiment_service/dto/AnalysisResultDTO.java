package com.tradingbot.sentiment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class AnalysisResultDTO {
    private String signal;
    private String confidence;
    private String summary;
    private String timestamp;
    private ParametersDTO parameters;
    private QuantitativeAnalysisDTO quantitativeAnalysis;
    private SentimentAnalysisDTO sentimentAnalysis;
}
