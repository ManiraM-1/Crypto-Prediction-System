package com.tradingbot.sentiment_service.service;

import com.tradingbot.sentiment_service.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionEngineService {

    private final NewsService newsService;
    private final SentimentAnalysisService sentimentService;
    private final ChartDataService chartDataService;
    private final ModelService modelService;
    private final CoinGeckoPriceService coinGeckoPriceService; // ✅ NEW SERVICE

    public DecisionEngineService(NewsService newsService,
                                 SentimentAnalysisService sentimentService,
                                 ChartDataService chartDataService,
                                 ModelService modelService,
                                 CoinGeckoPriceService coinGeckoPriceService) { // ✅ INJECT IT
        this.newsService = newsService;
        this.sentimentService = sentimentService;
        this.chartDataService = chartDataService;
        this.modelService = modelService;
        this.coinGeckoPriceService = coinGeckoPriceService; // ✅ ASSIGN IT
    }

    public Mono<AnalysisResultDTO> makeDecision(String symbol, int minutes) {

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎯 STARTING ANALYSIS FOR " + symbol + " (" + minutes + " minutes)");
        System.out.println("=".repeat(70));

        // ✅ FETCH FROM 4 SOURCES (ADDED COINGECKO)
        Mono<GNewsResponse> newsMono = this.newsService.fetchCryptoNews(symbol);
        Mono<List<PriceDataPointDTO>> chartMono = this.chartDataService.getHistoricalData(symbol, 48);
        Mono<MlPredictionResponseDTO> modelMono = this.modelService.getPrediction(symbol, minutes);
        Mono<Double> realPriceMono = this.coinGeckoPriceService.getCurrentPrice(symbol); // ✅ REAL PRICE!

        return Mono.zip(newsMono, chartMono, modelMono, realPriceMono) // ✅ ZIP 4 SOURCES
                .map(tuple -> {

                    GNewsResponse gNewsResponse = tuple.getT1();
                    List<PriceDataPointDTO> historicalData = tuple.getT2();
                    MlPredictionResponseDTO mlResponse = tuple.getT3();
                    Double realCurrentPrice = tuple.getT4(); // ✅ COINGECKO REAL PRICE

                    System.out.println("\n📊 Data Collection Complete:");
                    System.out.println("   - News Articles: " + (gNewsResponse.getArticles() != null ? gNewsResponse.getArticles().size() : 0));
                    System.out.println("   - Chart Data Points: " + historicalData.size());
                    System.out.println("   - ML Status: " + mlResponse.getStatus());
                    System.out.println("   - Real Price (CoinGecko): $" + String.format("%.2f", realCurrentPrice)); // ✅ LOG IT

                    // ============================================================
                    // SENTIMENT ANALYSIS
                    // ============================================================
                    List<GNewsArticle> articles = gNewsResponse.getArticles();
                    int positiveCount = 0, negativeCount = 0, neutralCount = 0;
                    double totalScore = 0;
                    List<HeadlineDTO> topHeadlines = new ArrayList<>();

                    if (articles != null && !articles.isEmpty()) {
                        List<GNewsArticle> articlesToProcess = articles.subList(0, Math.min(articles.size(), 5));
                        for (GNewsArticle article : articlesToProcess) {
                            int score = sentimentService.getSentimentScore(article.getTitle());
                            String sentimentLabel;
                            if (score < 1.5) {
                                sentimentLabel = "Negative";
                                negativeCount++;
                            } else if (score > 2.5) {
                                sentimentLabel = "Positive";
                                positiveCount++;
                            } else {
                                sentimentLabel = "Neutral";
                                neutralCount++;
                            }
                            totalScore += score;

                            String timestamp = article.getPublishedAt() != null
                                    ? article.getPublishedAt()
                                    : Instant.now().toString();

                            topHeadlines.add(new HeadlineDTO(
                                    article.getTitle(),
                                    article.getSource().getName(),
                                    timestamp,
                                    sentimentLabel,
                                    0.9
                            ));
                        }
                    }

                    double averageSentiment = (topHeadlines.isEmpty()) ? 2.0 : totalScore / topHeadlines.size();
                    String sentimentLabel = (averageSentiment > 2.5) ? "Positive"
                            : (averageSentiment < 1.5) ? "Negative"
                            : "Neutral";

                    SentimentStatsDTO sentimentStats = new SentimentStatsDTO(
                            gNewsResponse.getTotalArticles(),
                            positiveCount,
                            negativeCount,
                            neutralCount,
                            "Stable"
                    );

                    System.out.println("\n💭 Sentiment Analysis:");
                    System.out.println("   - Average Score: " + String.format("%.2f", averageSentiment));
                    System.out.println("   - Label: " + sentimentLabel);
                    System.out.println("   - Distribution: +" + positiveCount + " / ○" + neutralCount + " / -" + negativeCount);

                    // ============================================================
                    // ✅ USE COINGECKO PRICE AS THE SINGLE SOURCE OF TRUTH
                    // ============================================================
                    double currentPrice = realCurrentPrice; // ✅ ALWAYS USE COINGECKO!

                    // Fallback to historical data only if CoinGecko fails
                    if (currentPrice <= 0.0 && !historicalData.isEmpty()) {
                        currentPrice = historicalData.get(historicalData.size() - 1).getPrice();
                        System.out.println("⚠️  CoinGecko failed, using historical data: $" + currentPrice);
                    }

                    double supportLevel = historicalData.stream()
                            .mapToDouble(PriceDataPointDTO::getPrice)
                            .min().orElse(currentPrice * 0.95);
                    double resistanceLevel = historicalData.stream()
                            .mapToDouble(PriceDataPointDTO::getPrice)
                            .max().orElse(currentPrice * 1.05);

                    // ============================================================
                    // ML PREDICTION
                    // ============================================================
                    String mlPrediction = mlResponse.getPrediction();
                    double mlConfidence = mlResponse.getConfidenceScore();
                    double probabilityUp = mlResponse.getProbabilityUp();

                    System.out.println("\n🤖 ML Model Prediction:");
                    System.out.println("   - Direction: " + mlPrediction);
                    System.out.println("   - Confidence: " + String.format("%.2f%%", mlConfidence));
                    System.out.println("   - P(UP): " + String.format("%.4f", probabilityUp));
                    System.out.println("   - Model: " + mlResponse.getModelUsed());

                    // ============================================================
                    // DECISION LOGIC
                    // ============================================================
                    String finalSignal;
                    String summary;
                    String confidence;

                    System.out.println("\n🐛 ========== DECISION DEBUG ==========");
                    System.out.println("ML Prediction: '" + mlPrediction + "'");
                    System.out.println("ML Confidence: " + mlConfidence + "%");
                    System.out.println("Probability UP: " + probabilityUp);
                    System.out.println("Average Sentiment: " + averageSentiment);
                    System.out.println("Sentiment Label: " + sentimentLabel);
                    System.out.println("🔍 RULE CHECKS:");
                    System.out.println("Rule 1 - BUY Check:");
                    System.out.println("  - mlPrediction.equals(\"UP\"): " + "UP".equals(mlPrediction));
                    System.out.println("  - mlConfidence > 40: " + (mlConfidence > 40) + " (" + mlConfidence + " > 40)");
                    System.out.println("  - BOTH TRUE? " + ("UP".equals(mlPrediction) && mlConfidence > 40));
                    System.out.println("=====================================\n");

                    // RULE 1: ML says UP with decent confidence
                    if ("UP".equals(mlPrediction) && mlConfidence > 40) {
                        finalSignal = "BUY";
                        if (averageSentiment >= 2.5) {
                            summary = String.format(
                                    "🟢 Strong BUY: ML predicts UP (%.1f%%, P(UP)=%.2f) with Positive sentiment (%.2f).",
                                    mlConfidence, probabilityUp, averageSentiment
                            );
                            confidence = "High";
                        } else {
                            summary = String.format(
                                    "🟡 Moderate BUY: ML predicts UP (%.1f%%, P(UP)=%.2f). Sentiment is %s (%.2f).",
                                    mlConfidence, probabilityUp, sentimentLabel, averageSentiment
                            );
                            confidence = "Medium";
                        }
                        System.out.println("✅ TRIGGERED: BUY (Rule 1)");
                    }
                    // RULE 2: ML says DOWN with decent confidence
                    else if ("DOWN".equals(mlPrediction) && mlConfidence > 40) {
                        finalSignal = "SELL";
                        if (averageSentiment <= 1.5) {
                            summary = String.format(
                                    "🔴 Strong SELL: ML predicts DOWN (%.1f%%, P(DOWN)=%.2f) with Negative sentiment (%.2f).",
                                    mlConfidence, (1 - probabilityUp), averageSentiment
                            );
                            confidence = "High";
                        } else {
                            summary = String.format(
                                    "🟡 Moderate SELL: ML predicts DOWN (%.1f%%, P(DOWN)=%.2f). Sentiment is %s (%.2f).",
                                    mlConfidence, (1 - probabilityUp), sentimentLabel, averageSentiment
                            );
                            confidence = "Medium";
                        }
                        System.out.println("✅ TRIGGERED: SELL (Rule 2)");
                    }
                    // RULE 3: Weak ML but very positive sentiment
                    else if (averageSentiment > 3.0 && probabilityUp >= 0.45) {
                        finalSignal = "BUY";
                        summary = String.format(
                                "💚 Sentiment BUY: Very positive news (%.2f) with neutral-bullish ML (P(UP)=%.2f).",
                                averageSentiment, probabilityUp
                        );
                        confidence = "Medium";
                        System.out.println("✅ TRIGGERED: Sentiment BUY (Rule 3)");
                    }
                    // RULE 4: Weak ML but very negative sentiment
                    else if (averageSentiment < 1.0 && probabilityUp <= 0.55) {
                        finalSignal = "SELL";
                        summary = String.format(
                                "💔 Sentiment SELL: Very negative news (%.2f) with neutral-bearish ML (P(DOWN)=%.2f).",
                                averageSentiment, (1 - probabilityUp)
                        );
                        confidence = "Medium";
                        System.out.println("✅ TRIGGERED: Sentiment SELL (Rule 4)");
                    }
                    // RULE 5: Default HOLD
                    else {
                        finalSignal = "HOLD";
                        summary = String.format(
                                "⚪ HOLD: Weak signals. ML: %s (%.1f%%), Sentiment: %s (%.2f). No clear direction.",
                                mlPrediction, mlConfidence, sentimentLabel, averageSentiment
                        );
                        confidence = "Low";
                        System.out.println("⚠️ TRIGGERED: HOLD (Rule 5 - Default)");
                    }

                    System.out.println("\n" + "=".repeat(70));
                    System.out.println("📢 FINAL DECISION: " + finalSignal + " (" + confidence + " confidence)");
                    System.out.println("=".repeat(70) + "\n");

                    // ============================================================
                    // BUILD RESPONSE DTO
                    // ============================================================
                    ParametersDTO params = new ParametersDTO(symbol, minutes, Instant.now().toString());

                    double priceChangePercent = "UP".equals(mlPrediction)
                            ? (1 + (mlConfidence / 1000))
                            : (1 - (mlConfidence / 1000));
                    double predictedPrice = currentPrice * priceChangePercent;

                    double predictedChange = ((predictedPrice - currentPrice) / currentPrice) * 100.0;

                    // ✅ ALL PRICES NOW USE COINGECKO!
                    MetricsDTO metrics = new MetricsDTO(
                            predictedChange,
                            predictedPrice,
                            currentPrice,        // ✅ CoinGecko real-time price
                            supportLevel,
                            resistanceLevel
                    );

                    List<PriceDataPointDTO> forecastData = List.of(
                            new PriceDataPointDTO(Instant.now().getEpochSecond(), currentPrice, 0),
                            new PriceDataPointDTO(
                                    Instant.now().getEpochSecond() + (minutes * 60),
                                    predictedPrice,
                                    0
                            )
                    );

                    QuantitativeAnalysisDTO quantDTO = new QuantitativeAnalysisDTO(
                            "Hybrid LSTM+XGBoost " + mlResponse.getModelUsed(),
                            historicalData,
                            forecastData,
                            metrics
                    );

                    SentimentAnalysisDTO sentimentDTO = new SentimentAnalysisDTO(
                            averageSentiment,
                            sentimentLabel,
                            topHeadlines,
                            sentimentStats
                    );

                    return new AnalysisResultDTO(
                            finalSignal,
                            confidence,
                            summary,
                            Instant.now().toString(),
                            params,
                            quantDTO,
                            sentimentDTO
                    );
                });
    }
}





/*package com.tradingbot.sentiment_service.service;

import com.tradingbot.sentiment_service.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DecisionEngineService {

    private final NewsService newsService;
    private final SentimentAnalysisService sentimentService;
    private final ChartDataService chartDataService;
    private final ModelService modelService; // <-- 1. INJECT THE NEW MODEL SERVICE

    // 2. ADD THE MODEL SERVICE TO THE CONSTRUCTOR
    public DecisionEngineService(NewsService newsService,
                                 SentimentAnalysisService sentimentService,
                                 ChartDataService chartDataService,
                                 ModelService modelService) {
        this.newsService = newsService;
        this.sentimentService = sentimentService;
        this.chartDataService = chartDataService;
        this.modelService = modelService; // <-- 3. ASSIGN IT
    }

    public Mono<AnalysisResultDTO> makeDecision(String symbol, int minutes) {

        // --- We now have THREE asynchronous calls to make ---
        // 1. Fetch REAL news
        Mono<GNewsResponse> newsMono = this.newsService.fetchCryptoNews();
        // 2. Fetch REAL chart data (we'll fetch 48 hours for the chart)
        Mono<List<PriceDataPointDTO>> chartMono = this.chartDataService.getHistoricalData(symbol, 48);
        // 3. Fetch REAL ML prediction from our Colab API
        Mono<MlPredictionResponseDTO> modelMono = this.modelService.getPrediction(symbol, minutes);


        // --- We use Mono.zip to wait for ALL THREE calls to finish ---
        return Mono.zip(newsMono, chartMono, modelMono)
                .map(tuple -> {

                    GNewsResponse gNewsResponse = tuple.getT1();
                    List<PriceDataPointDTO> historicalData = tuple.getT2();
                    MlPredictionResponseDTO mlResponse = tuple.getT3(); // <-- 4. GET REAL MODEL RESULT

                    // --- 1. SENTIMENT ANALYSIS (REAL DATA) ---
                    List<GNewsArticle> articles = gNewsResponse.getArticles();
                    int positiveCount = 0, negativeCount = 0, neutralCount = 0;
                    double totalScore = 0;
                    List<HeadlineDTO> topHeadlines = new ArrayList<>();

                    if (articles != null && !articles.isEmpty()) {
                        List<GNewsArticle> articlesToProcess = articles.subList(0, Math.min(articles.size(), 5));
                        for (GNewsArticle article : articlesToProcess) {
                            int score = sentimentService.getSentimentScore(article.getTitle());
                            String sentimentLabel;
                            if (score < 1.5) { sentimentLabel = "Negative"; negativeCount++; }
                            else if (score > 2.5) { sentimentLabel = "Positive"; positiveCount++; }
                            else { sentimentLabel = "Neutral"; neutralCount++; }
                            totalScore += score;
                            topHeadlines.add(new HeadlineDTO(article.getTitle(), article.getSource().getName(), article.getUrl(), sentimentLabel, 0.9));
                        }
                    }
                    double averageSentiment = (topHeadlines.isEmpty()) ? 2.0 : totalScore / topHeadlines.size();
                    String sentimentLabel = (averageSentiment > 2.5) ? "Positive" : (averageSentiment < 1.5) ? "Negative" : "Neutral";
                    SentimentStatsDTO sentimentStats = new SentimentStatsDTO(gNewsResponse.getTotalArticles(), positiveCount, negativeCount, neutralCount, "Stable");


                    // --- 2. QUANTITATIVE ANALYSIS (REAL DATA) ---
                    double currentPrice = historicalData.isEmpty() ? 0 : historicalData.get(historicalData.size() - 1).getPrice();
                    double supportLevel = historicalData.stream().mapToDouble(PriceDataPointDTO::getPrice).min().orElse(0);
                    double resistanceLevel = historicalData.stream().mapToDouble(PriceDataPointDTO::getPrice).max().orElse(0);

                    // --- 5. GET REAL PREDICTION (NO MORE STUB) ---
                    double mlPredictionSignal = mlResponse.getPredicted_signal(); // 1.0 for UP, 0.0 for DOWN
                    double mlConfidence = mlResponse.getConfidence_score();

                    // --- 6. DECISION LOGIC (NOW 100% REAL) ---
                    String finalSignal, summary, confidence = "Medium";

                    System.out.println("--- Making Decision ---");
                    System.out.println("ML Model Prediction: " + (mlPredictionSignal == 1 ? "UP" : "DOWN") + " (Conf: " + mlConfidence + "%)");
                    System.out.println("Average Sentiment Score: " + averageSentiment);

                    if (mlPredictionSignal == 1 && mlConfidence > 55 && averageSentiment > 2.5) {
                        finalSignal = "BUY";
                        summary = String.format("High Confidence BUY: Model predicts UP (%.1f%%) and Sentiment is Positive (%.2f).", mlConfidence, averageSentiment);
                        confidence = "High";
                    } else if (mlPredictionSignal == 0 && mlConfidence > 55 && averageSentiment < 1.5) {
                        finalSignal = "SELL";
                        summary = String.format("High Confidence SELL: Model predicts DOWN (%.1f%%) and Sentiment is Negative (%.2f).", mlConfidence, averageSentiment);
                        confidence = "High";
                    } else {
                        finalSignal = "HOLD";
                        summary = String.format("Neutral/Conflict: Model (%.1f%%) and Sentiment (%.2f) do not agree. HOLD recommended.", mlConfidence, averageSentiment);
                    }
                    System.out.println("DECISION: " + finalSignal);

                    // --- 7. BUILD THE FULL RESPONSE DTO (ALL REAL/CALCULATED DATA) ---
                    ParametersDTO params = new ParametersDTO(symbol, minutes, Instant.now().toString());

                    // We use the REAL mlConfidence as the predicted change (this is a proxy)
                    MetricsDTO metrics = new MetricsDTO(mlConfidence, mlResponse.getCurrent_price(), currentPrice, supportLevel, resistanceLevel);

                    // We create a simple forecast line based on the signal
                    double predictedPrice = (mlPredictionSignal == 1) ? currentPrice * 1.02 : currentPrice * 0.98; // A simple 2% change
                    List<PriceDataPointDTO> forecastData = List.of(
                            new PriceDataPointDTO(Instant.now().getEpochSecond(), currentPrice, 0),
                            // We use the REAL 'minutes' from the user to draw the line
                            new PriceDataPointDTO(Instant.now().getEpochSecond() + (minutes * 60), predictedPrice, 0)
                    );

                    QuantitativeAnalysisDTO quantDTO = new QuantitativeAnalysisDTO("Hybrid LSTM+XGB v1", historicalData, forecastData, metrics);
                    SentimentAnalysisDTO sentimentDTO = new SentimentAnalysisDTO(averageSentiment, sentimentLabel, topHeadlines, sentimentStats);

                    return new AnalysisResultDTO(
                            finalSignal, confidence, summary, Instant.now().toString(),
                            params, quantDTO, sentimentDTO
                    );
                });
    }
}*/
/*package com.tradingbot.sentiment_service.service;

import com.tradingbot.sentiment_service.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionEngineService {

    private final NewsService newsService;
    private final SentimentAnalysisService sentimentService;
    private final ChartDataService chartDataService;
    private final ModelService modelService;

    public DecisionEngineService(NewsService newsService,
                                 SentimentAnalysisService sentimentService,
                                 ChartDataService chartDataService,
                                 ModelService modelService) {
        this.newsService = newsService;
        this.sentimentService = sentimentService;
        this.chartDataService = chartDataService;
        this.modelService = modelService;
    }

    public Mono<AnalysisResultDTO> makeDecision(String symbol, int minutes) {

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎯 STARTING ANALYSIS FOR " + symbol + " (" + minutes + " minutes)");
        System.out.println("=".repeat(70));

        // Fetch data from all sources (now symbol-specific for news)
        Mono<GNewsResponse> newsMono = this.newsService.fetchCryptoNews(symbol);
        Mono<List<PriceDataPointDTO>> chartMono = this.chartDataService.getHistoricalData(symbol, 48);
        Mono<MlPredictionResponseDTO> modelMono = this.modelService.getPrediction(symbol, minutes);

        return Mono.zip(newsMono, chartMono, modelMono)
                .map(tuple -> {

                    GNewsResponse gNewsResponse = tuple.getT1();
                    List<PriceDataPointDTO> historicalData = tuple.getT2();
                    MlPredictionResponseDTO mlResponse = tuple.getT3();

                    System.out.println("\n📊 Data Collection Complete:");
                    System.out.println("   - News Articles: " + (gNewsResponse.getArticles() != null ? gNewsResponse.getArticles().size() : 0));
                    System.out.println("   - Chart Data Points: " + historicalData.size());
                    System.out.println("   - ML Status: " + mlResponse.getStatus());

                    // ============================================================
                    // SENTIMENT ANALYSIS
                    // ============================================================
                    List<GNewsArticle> articles = gNewsResponse.getArticles();
                    int positiveCount = 0, negativeCount = 0, neutralCount = 0;
                    double totalScore = 0;
                    List<HeadlineDTO> topHeadlines = new ArrayList<>();

                    if (articles != null && !articles.isEmpty()) {
                        List<GNewsArticle> articlesToProcess = articles.subList(0, Math.min(articles.size(), 5));
                        for (GNewsArticle article : articlesToProcess) {
                            int score = sentimentService.getSentimentScore(article.getTitle());
                            String sentimentLabel;
                            if (score < 1.5) {
                                sentimentLabel = "Negative";
                                negativeCount++;
                            } else if (score > 2.5) {
                                sentimentLabel = "Positive";
                                positiveCount++;
                            } else {
                                sentimentLabel = "Neutral";
                                neutralCount++;
                            }
                            totalScore += score;
                            topHeadlines.add(new HeadlineDTO(
                                    article.getTitle(),
                                    article.getSource().getName(),
                                    article.getUrl(),
                                    sentimentLabel,
                                    0.9
                            ));
                        }
                    }

                    double averageSentiment = (topHeadlines.isEmpty()) ? 2.0 : totalScore / topHeadlines.size();
                    String sentimentLabel = (averageSentiment > 2.5) ? "Positive"
                            : (averageSentiment < 1.5) ? "Negative"
                            : "Neutral";

                    SentimentStatsDTO sentimentStats = new SentimentStatsDTO(
                            gNewsResponse.getTotalArticles(),
                            positiveCount,
                            negativeCount,
                            neutralCount,
                            "Stable"
                    );

                    System.out.println("\n💭 Sentiment Analysis:");
                    System.out.println("   - Average Score: " + String.format("%.2f", averageSentiment));
                    System.out.println("   - Label: " + sentimentLabel);
                    System.out.println("   - Distribution: +" + positiveCount + " / ○" + neutralCount + " / -" + negativeCount);

                    // ============================================================
                    // QUANTITATIVE ANALYSIS
                    // ============================================================
                    double currentPrice = historicalData.isEmpty() ? mlResponse.getCurrentPrice()
                            : historicalData.get(historicalData.size() - 1).getPrice();
                    double supportLevel = historicalData.stream()
                            .mapToDouble(PriceDataPointDTO::getPrice)
                            .min().orElse(currentPrice * 0.95);
                    double resistanceLevel = historicalData.stream()
                            .mapToDouble(PriceDataPointDTO::getPrice)
                            .max().orElse(currentPrice * 1.05);

                    // ============================================================
                    // ML PREDICTION
                    // ============================================================
                    String mlPrediction = mlResponse.getPrediction(); // "UP" or "DOWN"
                    double mlConfidence = mlResponse.getConfidenceScore(); // 0-100%
                    double probabilityUp = mlResponse.getProbabilityUp(); // 0.0-1.0

                    System.out.println("\n🤖 ML Model Prediction:");
                    System.out.println("   - Direction: " + mlPrediction);
                    System.out.println("   - Confidence: " + String.format("%.2f%%", mlConfidence));
                    System.out.println("   - P(UP): " + String.format("%.4f", probabilityUp));
                    System.out.println("   - Model: " + mlResponse.getModelUsed());

                    // ============================================================
                    // DECISION LOGIC
                    // ============================================================
                    String finalSignal;
                    String summary;
                    String confidence;

                    System.out.println("\n🐛 ========== DECISION DEBUG ==========");
                    System.out.println("ML Prediction: '" + mlPrediction + "'");
                    System.out.println("ML Confidence: " + mlConfidence + "%");
                    System.out.println("Probability UP: " + probabilityUp);
                    System.out.println("Average Sentiment: " + averageSentiment);
                    System.out.println("Sentiment Label: " + sentimentLabel);
                    System.out.println("Check UP: " + "UP".equals(mlPrediction));
                    System.out.println("Check Conf>40: " + (mlConfidence > 40));
                    System.out.println("Check Sent>=2: " + (averageSentiment >= 2.0));
                    System.out.println("=====================================\n");

                    // RULE 1: ML says UP with decent confidence
                    if ("UP".equals(mlPrediction) && mlConfidence > 40) {
                        finalSignal = "BUY";
                        if (averageSentiment >= 2.5) {
                            summary = String.format(
                                    "🟢 Strong BUY: ML predicts UP (%.1f%%, P(UP)=%.2f) with Positive sentiment (%.2f).",
                                    mlConfidence, probabilityUp, averageSentiment
                            );
                            confidence = "High";
                        } else {
                            summary = String.format(
                                    "🟡 Moderate BUY: ML predicts UP (%.1f%%, P(UP)=%.2f). Sentiment is %s (%.2f).",
                                    mlConfidence, probabilityUp, sentimentLabel, averageSentiment
                            );
                            confidence = "Medium";
                        }
                        System.out.println("✅ TRIGGERED: BUY (Rule 1)");
                    }
                    // RULE 2: ML says DOWN with decent confidence
                    else if ("DOWN".equals(mlPrediction) && mlConfidence > 40) {
                        finalSignal = "SELL";
                        if (averageSentiment <= 1.5) {
                            summary = String.format(
                                    "🔴 Strong SELL: ML predicts DOWN (%.1f%%, P(DOWN)=%.2f) with Negative sentiment (%.2f).",
                                    mlConfidence, (1 - probabilityUp), averageSentiment
                            );
                            confidence = "High";
                        } else {
                            summary = String.format(
                                    "🟡 Moderate SELL: ML predicts DOWN (%.1f%%, P(DOWN)=%.2f). Sentiment is %s (%.2f).",
                                    mlConfidence, (1 - probabilityUp), sentimentLabel, averageSentiment
                            );
                            confidence = "Medium";
                        }
                        System.out.println("✅ TRIGGERED: SELL (Rule 2)");
                    }
                    // RULE 3: Weak ML but very positive sentiment
                    else if (averageSentiment > 3.0 && probabilityUp >= 0.45) {
                        finalSignal = "BUY";
                        summary = String.format(
                                "💚 Sentiment BUY: Very positive news (%.2f) with neutral-bullish ML (P(UP)=%.2f).",
                                averageSentiment, probabilityUp
                        );
                        confidence = "Medium";
                        System.out.println("✅ TRIGGERED: Sentiment BUY (Rule 3)");
                    }
                    // RULE 4: Weak ML but very negative sentiment
                    else if (averageSentiment < 1.0 && probabilityUp <= 0.55) {
                        finalSignal = "SELL";
                        summary = String.format(
                                "💔 Sentiment SELL: Very negative news (%.2f) with neutral-bearish ML (P(DOWN)=%.2f).",
                                averageSentiment, (1 - probabilityUp)
                        );
                        confidence = "Medium";
                        System.out.println("✅ TRIGGERED: Sentiment SELL (Rule 4)");
                    }
                    // RULE 5: Default HOLD
                    else {
                        finalSignal = "HOLD";
                        summary = String.format(
                                "⚪ HOLD: Weak signals. ML: %s (%.1f%%), Sentiment: %s (%.2f). No clear direction.",
                                mlPrediction, mlConfidence, sentimentLabel, averageSentiment
                        );
                        confidence = "Low";
                        System.out.println("⚠️ TRIGGERED: HOLD (Rule 5)");
                    }

                    System.out.println("\n" + "=".repeat(70));
                    System.out.println("📢 FINAL DECISION: " + finalSignal + " (" + confidence + " confidence)");
                    System.out.println("=".repeat(70) + "\n");

                    // ============================================================
                    // BUILD RESPONSE DTO
                    // ============================================================
                    ParametersDTO params = new ParametersDTO(symbol, minutes, Instant.now().toString());

                    MetricsDTO metrics = new MetricsDTO(
                            mlConfidence,
                            mlResponse.getCurrentPrice(),
                            currentPrice,
                            supportLevel,
                            resistanceLevel
                    );

                    // Forecast line based on ML prediction
                    double priceChangePercent = "UP".equals(mlPrediction)
                            ? (1 + (mlConfidence / 1000)) // Subtle increase based on confidence
                            : (1 - (mlConfidence / 1000)); // Subtle decrease
                    double predictedPrice = currentPrice * priceChangePercent;

                    List<PriceDataPointDTO> forecastData = List.of(
                            new PriceDataPointDTO(Instant.now().getEpochSecond(), currentPrice, 0),
                            new PriceDataPointDTO(
                                    Instant.now().getEpochSecond() + (minutes * 60),
                                    predictedPrice,
                                    0
                            )
                    );

                    QuantitativeAnalysisDTO quantDTO = new QuantitativeAnalysisDTO(
                            "Hybrid LSTM+XGBoost " + mlResponse.getModelUsed(),
                            historicalData,
                            forecastData,
                            metrics
                    );

                    SentimentAnalysisDTO sentimentDTO = new SentimentAnalysisDTO(
                            averageSentiment,
                            sentimentLabel,
                            topHeadlines,
                            sentimentStats
                    );

                    return new AnalysisResultDTO(
                            finalSignal,
                            confidence,
                            summary,
                            Instant.now().toString(),
                            params,
                            quantDTO,
                            sentimentDTO
                    );
                });
    }
}*/