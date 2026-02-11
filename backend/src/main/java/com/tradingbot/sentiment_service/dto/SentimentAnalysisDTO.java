package com.tradingbot.sentiment_service.dto;

import com.tradingbot.sentiment_service.dto.HeadlineDTO;
import com.tradingbot.sentiment_service.dto.SentimentStatsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data @AllArgsConstructor
public class SentimentAnalysisDTO {
    private double marketMoodScore;
    private String sentimentLabel;
    private List<HeadlineDTO> topHeadlines;
    private SentimentStatsDTO statistics;
}