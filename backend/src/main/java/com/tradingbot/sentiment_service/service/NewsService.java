package com.tradingbot.sentiment_service.service;

// Import our new DTOs
import com.tradingbot.sentiment_service.dto.GNewsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono; // Import Mono

@Service
public class NewsService {

    private final WebClient webClient;
    private final String apiKey;

    public NewsService(WebClient.Builder webClientBuilder,
                       @Value("${gnews.api.key}") String apiKey,
                       @Value("${gnews.api.url}") String apiUrl) {

        this.apiKey = apiKey;
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
    }

    /**
     * Fetches live crypto news and maps it to our Java objects.
     */
    /**
     * Fetches coin-specific crypto news
     * @param symbol The trading pair (e.g., "BTC/USDT")
     */
    public Mono<GNewsResponse> fetchCryptoNews(String symbol) {
        // Extract coin name (BTC/USDT -> BTC)
        String coinName = symbol.split("/")[0];

        // Map to full name for better news results
        String searchTerm = getCoinSearchTerm(coinName);

        String query = searchTerm + " cryptocurrency";
        System.out.println("📰 Fetching news for: " + query);

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("q", query)
                        .queryParam("lang", "en")
                        .queryParam("sortby", "publishedAt")
                        .queryParam("max", 10)
                        .queryParam("apikey", this.apiKey)
                        .build())
                .retrieve()
                .bodyToMono(GNewsResponse.class)
                .doOnSuccess(response -> {
                    if (response != null && response.getArticles() != null) {
                        System.out.println("✅ Found " + response.getArticles().size() + " articles for " + searchTerm);
                    }
                })
                .doOnError(error -> System.err.println("❌ News error: " + error.getMessage()));
    }

    /**
     * Map coin symbols to full names
     */
    private String getCoinSearchTerm(String symbol) {
        switch (symbol.toUpperCase()) {
            case "BTC": return "Bitcoin";
            case "ETH": return "Ethereum";
            case "BNB": return "Binance Coin";
            case "XRP": return "Ripple";
            case "ADA": return "Cardano";
            case "SOL": return "Solana";
            case "DOGE": return "Dogecoin";
            case "MATIC": return "Polygon";
            case "DOT": return "Polkadot";
            default: return symbol;
        }
    }
}

// ====================================================================
// FIXED NewsService - Returns coin-specific news
// ====================================================================

/*package com.tradingbot.sentiment_service.service;

import com.tradingbot.sentiment_service.dto.GNewsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class NewsService {

    private final WebClient webClient;

    @Value("${gnews.api.key}")
    private String apiKey;

    public NewsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://gnews.io/api/v4/")
                .build();
    }

    /**
     * Fetch news for a SPECIFIC cryptocurrency
     * @param symbol The coin symbol (e.g., "BTC/USDT")
     * @return News articles about that specific coin
     *//*
    public Mono<GNewsResponse> fetchCryptoNews(String symbol) {

        // Extract coin name from symbol (BTC/USDT -> BTC)
        String coinName = symbol.split("/")[0];

        // Map common symbols to full names for better news results
        String searchTerm = getCoinSearchTerm(coinName);

        System.out.println("📰 Fetching news for: " + searchTerm);

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("search")
                        .queryParam("q", searchTerm + " cryptocurrency") // Coin-specific search
                        .queryParam("lang", "en")
                        .queryParam("max", "10")
                        .queryParam("token", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(GNewsResponse.class)
                .doOnSuccess(response -> {
                    if (response != null && response.getArticles() != null) {
                        System.out.println("✅ Found " + response.getArticles().size() + " articles for " + searchTerm);
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("❌ News fetch failed for " + searchTerm + ": " + e.getMessage());
                    // Return empty response instead of failing
                    return Mono.just(new GNewsResponse());
                });
    }

    /**
     * Helper method to map coin symbols to better search terms
     *//*
    private String getCoinSearchTerm(String symbol) {
        switch (symbol.toUpperCase()) {
            case "BTC":
                return "Bitcoin";
            case "ETH":
                return "Ethereum";
            case "BNB":
                return "Binance Coin";
            case "XRP":
                return "Ripple XRP";
            case "ADA":
                return "Cardano";
            case "SOL":
                return "Solana";
            case "DOT":
                return "Polkadot";
            case "DOGE":
                return "Dogecoin";
            case "MATIC":
                return "Polygon MATIC";
            case "LINK":
                return "Chainlink";
            case "UNI":
                return "Uniswap";
            case "LTC":
                return "Litecoin";
            case "AVAX":
                return "Avalanche";
            case "ATOM":
                return "Cosmos";
            case "TRX":
                return "TRON";
            default:
                return symbol; // Return as-is if not mapped
        }
    }
}
*/