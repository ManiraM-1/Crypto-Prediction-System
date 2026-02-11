package com.tradingbot.sentiment_service.dto;

// We add these annotations to tell Spring how to handle JSON
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignores any JSON fields we don't care about
public class GNewsSource {
    private String name;
    private String url;

    // --- Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}