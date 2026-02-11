package com.tradingbot.sentiment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data @AllArgsConstructor
public class QuantitativeAnalysisDTO {
    private String modelVersion;
    private List<PriceDataPointDTO> historicalData;
    private List<PriceDataPointDTO> forecastData;
    private MetricsDTO metrics;
}