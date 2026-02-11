package com.tradingbot.sentiment_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GNewsArticle {
    private String title;
    private String description;
    private String url;
    private String image;
    private String publishedAt;
    private GNewsSource source;

    // --- Getters and Setters ---
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // ✅ THIS IS THE NEW GETTER YOU NEED
    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public GNewsSource getSource() {
        return source;
    }

    public void setSource(GNewsSource source) {
        this.source = source;
    }
}