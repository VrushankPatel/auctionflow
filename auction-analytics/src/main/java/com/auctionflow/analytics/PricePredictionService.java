package com.auctionflow.analytics;

import com.auctionflow.analytics.dtos.PricePredictionRequest;
import com.auctionflow.analytics.dtos.PricePredictionResponse;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

@Service
public class PricePredictionService {

    private static final Logger logger = LoggerFactory.getLogger(PricePredictionService.class);

    @Autowired
    private FeatureEngineeringService featureEngineeringService;

    @Autowired
    private ModelVersioningService modelVersioningService;

    private Booster model;

    @PostConstruct
    public void init() {
        try {
            model = modelVersioningService.loadLatestModel();
            logger.info("Price prediction model loaded successfully");
        } catch (Exception e) {
            logger.warn("Failed to load model, predictions will not be available", e);
        }
    }

    public CompletableFuture<PricePredictionResponse> predictPrice(PricePredictionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (model == null) {
                throw new RuntimeException("Model not loaded");
            }

            try {
                double[] features = featureEngineeringService.extractFeatures(
                        request.getAuctionId(),
                        request.getHistoricalPrices(),
                        request.getCategory(),
                        request.getDaysSinceStart(),
                        request.getSellerId(),
                        request.getSellerRating()
                );

                float[] featureFloats = new float[features.length];
                for (int i = 0; i < features.length; i++) {
                    featureFloats[i] = (float) features[i];
                }

                DMatrix dmatrix = new DMatrix(new float[][]{featureFloats});
                float[][] predictions = model.predict(dmatrix);

                double predictedPrice = predictions[0][0];
                // For simplicity, confidence is a placeholder
                double confidence = 0.8;

                return new PricePredictionResponse(predictedPrice, confidence);
            } catch (Exception e) {
                logger.error("Error predicting price for auction {}", request.getAuctionId(), e);
                throw new RuntimeException("Prediction failed", e);
            }
        });
    }
}