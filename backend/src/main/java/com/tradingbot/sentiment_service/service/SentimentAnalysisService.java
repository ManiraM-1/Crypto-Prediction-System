package com.tradingbot.sentiment_service.service;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class SentimentAnalysisService {

    private StanfordCoreNLP pipeline;

    @PostConstruct // This tells Spring to run this method after the service is created
    public void init() {
        // --- This is the one-time setup ---
        System.out.println("Initializing StanfordCoreNLP pipeline...");
        Properties props = new Properties();
        // We only need the "sentiment" analyzer and its prerequisites
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");

        // This line initializes the pipeline.
        // The first time this runs, it may take a minute
        // as it loads the models into memory.
        this.pipeline = new StanfordCoreNLP(props);
        System.out.println("StanfordCoreNLP pipeline initialized successfully.");
    }

    /**
     * Analyzes the sentiment of a given piece of text.
     * @param text The news headline or description.
     * @return An integer sentiment score:
     * 0 = Very Negative, 1 = Negative, 2 = Neutral, 3 = Positive, 4 = Very Positive
     */
    public int getSentimentScore(String text) {
        if (text == null || text.isEmpty()) {
            return 2; // Return Neutral for empty text
        }

        int mainSentiment = 0;
        int longestSentenceLength = 0;

        // Create a document from the text
        CoreDocument document = new CoreDocument(text);

        // Annotate the document (run the analysis)
        this.pipeline.annotate(document);

        // The library analyzes sentiment sentence by sentence.
        // We'll find the longest sentence and use its sentiment as the
        // main sentiment for the whole text (a common strategy).
        for (CoreSentence sentence : document.sentences()) {
            String sentiment = sentence.sentiment(); // e.g., "Negative", "Very Positive"
            int sentimentValue = getNumericSentiment(sentiment);

            if (sentence.text().length() > longestSentenceLength) {
                longestSentenceLength = sentence.text().length();
                mainSentiment = sentimentValue;
            }
        }
        return mainSentiment;
    }

    /**
     * Helper method to convert the text sentiment (e.g., "Positive")
     * into a simple number.
     */
    private int getNumericSentiment(String sentiment) {
        switch (sentiment.toLowerCase()) {
            case "very negative": return 0;
            case "negative": return 1;
            case "neutral": return 2;
            case "positive": return 3;
            case "very positive": return 4;
            default: return 2; // Default to Neutral
        }
    }
}