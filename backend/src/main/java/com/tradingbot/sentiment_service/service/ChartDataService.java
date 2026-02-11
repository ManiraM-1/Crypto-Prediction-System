package com.tradingbot.sentiment_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradingbot.sentiment_service.dto.PriceDataPointDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChartDataService {

    private final WebClient coinDcxClient;
    private final WebClient coinGeckoClient;

    private static final Map<String, String> SYMBOL_TO_COINGECKO_ID = new HashMap<>();

    static {
        SYMBOL_TO_COINGECKO_ID.put("BTC/USDT", "bitcoin");
        SYMBOL_TO_COINGECKO_ID.put("ETH/USDT", "ethereum");
        SYMBOL_TO_COINGECKO_ID.put("SOL/USDT", "solana");
        SYMBOL_TO_COINGECKO_ID.put("XRP/USDT", "ripple");
        SYMBOL_TO_COINGECKO_ID.put("ADA/USDT", "cardano");
        SYMBOL_TO_COINGECKO_ID.put("DOGE/USDT", "dogecoin");
        SYMBOL_TO_COINGECKO_ID.put("BNB/USDT", "binancecoin");
        SYMBOL_TO_COINGECKO_ID.put("MATIC/USDT", "matic-network");
        SYMBOL_TO_COINGECKO_ID.put("DOT/USDT", "polkadot");
    }

    public ChartDataService(WebClient.Builder webClientBuilder) {
        // CoinDCX client
        this.coinDcxClient = webClientBuilder
                .baseUrl("https://public.coindcx.com")
                .build();

        // CoinGecko client
        this.coinGeckoClient = webClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
    }

    /**
     * ✅ PRIMARY METHOD: Fetches accurate historical data from CoinGecko
     * This matches the real-time price shown at the top
     */
    public Mono<List<PriceDataPointDTO>> getHistoricalData(String symbol, int hours) {
        System.out.println("📊 Fetching historical data for: " + symbol + " (" + hours + " hours)");

        // Try CoinGecko first (more accurate)
        return getHistoricalDataFromCoinGecko(symbol, hours)
                .onErrorResume(error -> {
                    System.err.println("⚠️  CoinGecko historical failed: " + error.getMessage());
                    System.out.println("🔄 Falling back to CoinDCX...");
                    // Fallback to CoinDCX if CoinGecko fails
                    return getHistoricalDataFromCoinDCX(symbol, hours);
                });
    }

    /**
     * Fetches from CoinGecko (ACCURATE - same as top section price)
     */
    private Mono<List<PriceDataPointDTO>> getHistoricalDataFromCoinGecko(String symbol, int hours) {
        final String coinGeckoId = SYMBOL_TO_COINGECKO_ID.getOrDefault(
                symbol,
                symbol.split("/")[0].toLowerCase()
        );

        // ✅ FIX: Make days final
        final int days = hours <= 24 ? 1 : Math.max(1, hours / 24);
        final String interval = hours <= 24 ? "hourly" : "daily";

        System.out.println("🔍 Fetching from CoinGecko: " + coinGeckoId + " (last " + days + " days)");

        return this.coinGeckoClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/{id}/market_chart")
                        .queryParam("vs_currency", "usd")
                        .queryParam("days", days)
                        .queryParam("interval", interval)
                        .build(coinGeckoId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    List<PriceDataPointDTO> priceData = new ArrayList<>();

                    // CoinGecko format: { "prices": [[timestamp_ms, price], ...] }
                    JsonNode prices = response.get("prices");
                    if (prices != null && prices.isArray()) {
                        // Take only the requested number of hours
                        int totalPoints = prices.size();
                        int startIndex = Math.max(0, totalPoints - hours);

                        for (int i = startIndex; i < totalPoints; i++) {
                            JsonNode point = prices.get(i);
                            long timestamp = point.get(0).asLong() / 1000L; // Convert ms to seconds
                            double price = point.get(1).asDouble();

                            // CoinGecko doesn't provide volume in this endpoint, use 0
                            priceData.add(new PriceDataPointDTO(timestamp, price, 0));
                        }
                    }

                    System.out.println("✅ CoinGecko: Fetched " + priceData.size() + " data points");
                    return priceData;
                });
    }

    /**
     * FALLBACK: Fetches from CoinDCX (may have price differences)
     */
    private Mono<List<PriceDataPointDTO>> getHistoricalDataFromCoinDCX(String symbol, int hours) {
        String apiSymbol = "B-" + symbol.replace("/", "_");
        String interval = "1h";
        String limit = String.valueOf(hours);

        System.out.println("🔍 Fetching from CoinDCX: " + apiSymbol);

        return this.coinDcxClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/market_data/candles")
                        .queryParam("pair", apiSymbol)
                        .queryParam("interval", interval)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    List<PriceDataPointDTO> priceData = new ArrayList<>();

                    if (response != null && response.isArray()) {
                        for (JsonNode item : response) {
                            long time = item.get("time").asLong() / 1000L;
                            double price = item.get("close").asDouble();
                            double volume = item.get("volume").asDouble();
                            priceData.add(new PriceDataPointDTO(time, price, volume));
                        }
                    }

                    System.out.println("✅ CoinDCX: Fetched " + priceData.size() + " data points");
                    return priceData;
                })
                .doOnError(error -> System.err.println("❌ CoinDCX error: " + error.getMessage()));
    }

    /**
     * Get detailed OHLCV candles for charting (if needed for future features)
     */
    public Mono<List<CandleDataDTO>> getCandleData(String symbol, String interval, int limit) {
        final String coinGeckoId = SYMBOL_TO_COINGECKO_ID.getOrDefault(
                symbol,
                symbol.split("/")[0].toLowerCase()
        );

        // ✅ FIX: Make days final with switch expression
        final int days = switch (interval) {
            case "1h" -> Math.max(1, limit / 24);
            case "4h" -> Math.max(1, limit / 6);
            case "1d" -> limit;
            default -> 7;
        };

        return this.coinGeckoClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/{id}/ohlc")
                        .queryParam("vs_currency", "usd")
                        .queryParam("days", days)
                        .build(coinGeckoId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    List<CandleDataDTO> candles = new ArrayList<>();

                    if (response.isArray()) {
                        for (JsonNode candle : response) {
                            // Format: [timestamp, open, high, low, close]
                            long timestamp = candle.get(0).asLong() / 1000L;
                            double open = candle.get(1).asDouble();
                            double high = candle.get(2).asDouble();
                            double low = candle.get(3).asDouble();
                            double close = candle.get(4).asDouble();

                            candles.add(new CandleDataDTO(timestamp, open, high, low, close, 0));
                        }
                    }

                    return candles;
                })
                .onErrorReturn(new ArrayList<>());
    }

    // DTO for candlestick data
    public static class CandleDataDTO {
        public final long timestamp;
        public final double open;
        public final double high;
        public final double low;
        public final double close;
        public final double volume;

        public CandleDataDTO(long timestamp, double open, double high, double low, double close, double volume) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }
}