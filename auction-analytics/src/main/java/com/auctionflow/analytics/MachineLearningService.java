package com.auctionflow.analytics;

import com.auctionflow.analytics.dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MachineLearningService {

    private static final Logger logger = LoggerFactory.getLogger(MachineLearningService.class);

    private final WebClient webClient;

    public MachineLearningService(@Value("${ml.service.url}") String mlServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(mlServiceUrl).build();
    }

    public Mono<PricePredictionResponse> predictPrice(PricePredictionRequest request) {
        return webClient.post()
                .uri("/predict-price")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PricePredictionResponse.class)
                .doOnSuccess(response -> logger.info("Price prediction successful for auction {}", request.getAuctionId()))
                .doOnError(error -> logger.error("Error predicting price for auction {}", request.getAuctionId(), error));
    }

    public Mono<EndTimeRecommendationResponse> recommendEndTime(EndTimeRecommendationRequest request) {
        return webClient.post()
                .uri("/recommend-end-time")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EndTimeRecommendationResponse.class)
                .doOnSuccess(response -> logger.info("End time recommendation successful for auction {}", request.getAuctionId()))
                .doOnError(error -> logger.error("Error recommending end time for auction {}", request.getAuctionId(), error));
    }

    public Mono<FraudScoreResponse> scoreFraud(FraudScoreRequest request) {
        return webClient.post()
                .uri("/score-fraud")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FraudScoreResponse.class)
                .doOnSuccess(response -> logger.info("Fraud scoring successful for user {} on auction {}", request.getUserId(), request.getAuctionId()))
                .doOnError(error -> logger.error("Error scoring fraud for user {} on auction {}", request.getUserId(), request.getAuctionId(), error));
    }
}