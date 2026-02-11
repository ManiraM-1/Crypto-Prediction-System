package com.tradingbot.sentiment_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlPredictionResponseDTO {

    // Python API returns (based on your console output):
    // {
    //   "symbol": "BTC/USDT",
    //   "timeframe_minutes": 60,
    //   "prediction": "UP",
    //   "probability_up": 0.533,
    //   "probability_down": 0.467,
    //   "confidence": 0.533,
    //   "current_price": 105000.0,
    //   "status": "success"
    // }

    private String symbol;

    @JsonProperty("timeframe_minutes")
    private Integer timeframeMinutes;

    @JsonProperty("timeframe_hours")
    private Double timeframeHours;

    @JsonProperty("model_used")
    private String modelUsed;

    private String prediction; // "UP" or "DOWN"

    @JsonProperty("probability_up")
    private Double probabilityUp;

    @JsonProperty("probability_down")
    private Double probabilityDown;

    private Double confidence; // 0.0 to 1.0 (e.g., 0.533 = 53.3%)

    @JsonProperty("current_price")
    private Double currentPrice;

    private String status; // "success" or "failed"
    private String error;  // Only present if error occurred

    // ============================================================
    // HELPER METHODS - These are what DecisionEngineService uses
    // ============================================================

    /**
     * Returns "UP" or "DOWN" - already in correct format
     */
    public String getPrediction() {
        // Add safety check
        if (this.prediction == null || this.prediction.trim().isEmpty()) {
            System.err.println("⚠️ WARNING: ML prediction is null or empty!");
            return "HOLD";
        }
        return this.prediction.trim().toUpperCase(); // Ensure uppercase and no whitespace
    }

    /**
     * Converts 0.0-1.0 to 0-100 percentage
     */
    public double getConfidenceScore() {
        if (this.confidence == null) {
            System.err.println("⚠️ WARNING: ML confidence is null!");
            return 0.0;
        }
        return this.confidence * 100.0; // 0.533 → 53.3%
    }

    /**
     * Returns probability as 0.0-1.0
     */
    public double getProbabilityUp() {
        if (this.probabilityUp == null) {
            System.err.println("⚠️ WARNING: ML probability_up is null!");
            return 0.5;
        }
        return this.probabilityUp;
    }

    /**
     * Returns current price, with fallback to 0
     */
    public double getCurrentPrice() {
        if (this.currentPrice == null) {
            System.err.println("⚠️ WARNING: ML current_price is null!");
            return 0.0;
        }
        return this.currentPrice;
    }

    /**
     * Returns model name with fallback
     */
    public String getModelUsed() {
        if (this.modelUsed == null || this.modelUsed.trim().isEmpty()) {
            return "Unknown";
        }
        return this.modelUsed;
    }

    /**
     * Legacy method for compatibility (converts "UP"/"DOWN" to 1/0)
     * @deprecated Use getPrediction() instead
     */
    @Deprecated
    public int getPredictedSignal() {
        String pred = getPrediction();
        return "UP".equals(pred) ? 1 : 0;
    }
}