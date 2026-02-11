package com.tradingbot.sentiment_service.controller;

import com.tradingbot.sentiment_service.dto.AnalysisResultDTO;
import com.tradingbot.sentiment_service.service.DecisionEngineService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

// This MUST match the port your frontend is running on (e.g., 8081)
@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/v1")
public class NewsController {

    private final DecisionEngineService decisionEngineService;

    public NewsController(DecisionEngineService decisionEngineService) {
        this.decisionEngineService = decisionEngineService;
    }

    @GetMapping("/decision")
    public Mono<AnalysisResultDTO> getTradingDecision(
            @RequestParam String symbol,
            @RequestParam int minutes) {

        return this.decisionEngineService.makeDecision(symbol, minutes);
    }
}