package com.tradingbot.sentiment_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class CoinGeckoPriceService {

    private final WebClient webClient;

    // Map trading symbols to CoinGecko IDs
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
        SYMBOL_TO_COINGECKO_ID.put("AVAX/USDT", "avalanche-2");
        SYMBOL_TO_COINGECKO_ID.put("LINK/USDT", "chainlink");
        SYMBOL_TO_COINGECKO_ID.put("UNI/USDT", "uniswap");
        SYMBOL_TO_COINGECKO_ID.put("LTC/USDT", "litecoin");
    }

    public CoinGeckoPriceService(WebClient.Builder webClientBuilder) {
        // CoinGecko Free API (no key needed!)
        this.webClient = webClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
    }

    /**
     * Fetches the REAL-TIME current price from CoinGecko
     * This is the SAME price shown in your frontend CoinInfoPanel
     *
     * @param symbol Trading pair (e.g., "BTC/USDT")
     * @return Current price in USD
     */
    public Mono<Double> getCurrentPrice(String symbol) {
        // ✅ FIX: Make it final by using ternary operator instead of if-else
        final String coinGeckoId = SYMBOL_TO_COINGECKO_ID.getOrDefault(
                symbol,
                symbol.split("/")[0].toLowerCase()
        );

        if (!SYMBOL_TO_COINGECKO_ID.containsKey(symbol)) {
            System.err.println("⚠️  Symbol " + symbol + " not mapped. Using fallback: " + coinGeckoId);
        }

        System.out.println("🔍 Fetching real-time price for " + symbol + " from CoinGecko...");

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/simple/price")
                        .queryParam("ids", coinGeckoId)
                        .queryParam("vs_currencies", "usd")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    // Response format: { "bitcoin": { "usd": 101958.0 } }
                    if (response.has(coinGeckoId)) {
                        double price = response.get(coinGeckoId).get("usd").asDouble();
                        System.out.println("✅ CoinGecko price for " + symbol + ": $" + String.format("%.2f", price));
                        return price;
                    } else {
                        System.err.println("❌ CoinGecko didn't return data for: " + coinGeckoId);
                        return 0.0;
                    }
                })
                .onErrorResume(error -> {
                    System.err.println("❌ Error fetching price from CoinGecko: " + error.getMessage());
                    return Mono.just(0.0); // Return 0 as fallback
                });
    }

    /**
     * Gets market data including 24h change, volume, etc.
     * This matches what your frontend CoinInfoPanel shows
     */
    public Mono<CoinGeckoMarketData> getMarketData(String symbol) {
        // ✅ FIX: Make it final
        final String coinGeckoId = SYMBOL_TO_COINGECKO_ID.getOrDefault(
                symbol,
                symbol.split("/")[0].toLowerCase()
        );

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/markets")
                        .queryParam("vs_currency", "usd")
                        .queryParam("ids", coinGeckoId)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    if (response.isArray() && response.size() > 0) {
                        JsonNode coin = response.get(0);
                        return new CoinGeckoMarketData(
                                coin.get("current_price").asDouble(),
                                coin.get("price_change_24h").asDouble(),
                                coin.get("price_change_percentage_24h").asDouble(),
                                coin.get("market_cap").asLong(),
                                coin.get("total_volume").asLong()
                        );
                    }
                    return new CoinGeckoMarketData(0, 0, 0, 0, 0);
                })
                .onErrorResume(error -> {
                    System.err.println("Error fetching market data: " + error.getMessage());
                    return Mono.just(new CoinGeckoMarketData(0, 0, 0, 0, 0));
                });
    }

    // Simple data class for market info
    public static class CoinGeckoMarketData {
        public final double currentPrice;
        public final double priceChange24h;
        public final double priceChangePercentage24h;
        public final long marketCap;
        public final long volume24h;

        public CoinGeckoMarketData(double currentPrice, double priceChange24h,
                                   double priceChangePercentage24h, long marketCap, long volume24h) {
            this.currentPrice = currentPrice;
            this.priceChange24h = priceChange24h;
            this.priceChangePercentage24h = priceChangePercentage24h;
            this.marketCap = marketCap;
            this.volume24h = volume24h;
        }
    }
}