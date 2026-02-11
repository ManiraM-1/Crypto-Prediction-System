package com.tradingbot.sentiment_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GNewsResponse {
    private int totalArticles;
    private List<GNewsArticle> articles;

    // --- Getters and Setters ---
    public int getTotalArticles() { return totalArticles; }
    public void setTotalArticles(int totalArticles) { this.totalArticles = totalArticles; }
    public List<GNewsArticle> getArticles() { return articles; }
    public void setArticles(List<GNewsArticle> articles) { this.articles = articles; }
}
