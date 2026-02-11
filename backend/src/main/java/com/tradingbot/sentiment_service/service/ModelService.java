package com.tradingbot.sentiment_service.service;

import com.tradingbot.sentiment_service.dto.MlPredictionRequestDTO;
import com.tradingbot.sentiment_service.dto.MlPredictionResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ModelService {

    private final WebClient webClient;

    public ModelService(WebClient.Builder webClientBuilder) {
        // -------------------------------------------------------------------------
        // TODO: PASTE YOUR HUGGING FACE ML SPACE URL HERE
        // It should look like: "https://maniram-crypto-ml-api.hf.space"
        // (Do not include /predict at the end, just the base URL)
        // -------------------------------------------------------------------------
        String hfUrl = "https://maniram-crypto-ml-api.hf.space";

        this.webClient = webClientBuilder.baseUrl(hfUrl).build();

        System.out.println("✅ ModelService initialized with URL: " + hfUrl);
    }

    public Mono<MlPredictionResponseDTO> getPrediction(String symbol, int minutes) {
        System.out.println("🔮 Requesting ML prediction for " + symbol + " (" + minutes + " minutes)");

        MlPredictionRequestDTO requestBody = new MlPredictionRequestDTO(symbol, minutes);

        return this.webClient.post()
                .uri("/predict")  // This appends /predict to the base URL
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(MlPredictionResponseDTO.class)
                .doOnSuccess(response -> {
                    if ("success".equals(response.getStatus())) {
                        System.out.println("✅ ML Prediction: " + response.getPrediction()
                                + " (Confidence: " + String.format("%.2f", response.getConfidenceScore()) + ")");
                    } else {
                        System.out.println("⚠️  ML API returned non-success status");
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("❌ PYTHON API ERROR: " + e.getMessage());
                    System.err.println("   Returning safe HOLD signal");

                    // Create a fallback response
                    MlPredictionResponseDTO fallback = new MlPredictionResponseDTO();
                    fallback.setSymbol(symbol);
                    fallback.setPrediction("HOLD");
                    fallback.setProbabilityUp(0.5);
                    fallback.setProbabilityDown(0.5);
                    fallback.setConfidence(0.0);
                    fallback.setCurrentPrice(0.0);
                    fallback.setStatus("error");
                    fallback.setError("API_DOWN: " + e.getMessage());

                    return Mono.just(fallback);
                });
    }
}