package com.tradingbot.sentiment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FullDecisionResponseDTO {
    private String symbol; // <-- THE MISSING FIELD THAT CAUSED THE CRASH
    private String signal;
    private String summary;
    private QuantitativeAnalysisDTO quantitativeAnalysis;
    private SentimentAnalysisDTO sentimentAnalysis;
}