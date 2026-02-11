package com.tradingbot.sentiment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictionRequestDTO {
    // This MUST match the Python API: { "symbol": "...", "minutes": ... }
    private String symbol;
    private int minutes;
}