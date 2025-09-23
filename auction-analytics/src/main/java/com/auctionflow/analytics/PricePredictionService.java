package com.auctionflow.analytics;

import com.auctionflow.analytics.dtos.PricePredictionRequest;
import com.auctionflow.analytics.dtos.PricePredictionResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

@Service
public class PricePredictionService {

    private static final Logger logger = LoggerFactory.getLogger(PricePredictionService.class);

    @Autowired
    private FeatureEngineeringService featureEngineeringService;





    public CompletableFuture<PricePredictionResponse> predictPrice(PricePredictionRequest request) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                double[] features = featureEngineeringService.extractFeatures(
                        request.getAuctionId(),
                        request.getHistoricalPrices(),
                        request.getCategory(),
                        request.getDaysSinceStart(),
                        request.getSellerId(),
                        request.getSellerRating()
                );

                // Dummy prediction: average of item price and reserve
                double predictedPrice = (request.getItemPrice() + request.getReservePrice()) / 2.0;
                double confidence = 0.5;

                return new PricePredictionResponse(predictedPrice, confidence);
            } catch (Exception e) {
                logger.error("Error predicting price for auction {}", request.getAuctionId(), e);
                throw new RuntimeException("Prediction failed", e);
            }
        });
    }
}