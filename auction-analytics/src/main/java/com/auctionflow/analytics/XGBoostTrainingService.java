package com.auctionflow.analytics;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class XGBoostTrainingService {

    private static final Logger logger = LoggerFactory.getLogger(XGBoostTrainingService.class);

    @Autowired
    private FeatureEngineeringService featureEngineeringService;

    private Booster model;

    public void trainModel(List<TrainingData> trainingData) throws Exception {
        logger.info("Starting XGBoost training with {} samples", trainingData.size());

        // Prepare data
        float[][] features = new float[trainingData.size()][];
        float[] labels = new float[trainingData.size()];

        for (int i = 0; i < trainingData.size(); i++) {
            TrainingData data = trainingData.get(i);
            double[] featureVector = featureEngineeringService.extractFeatures(
                    data.auctionId.toString(),
                    data.historicalPrices,
                    data.category,
                    data.daysSinceStart,
                    data.sellerId,
                    data.sellerRating
            );
            features[i] = new float[featureVector.length];
            for (int j = 0; j < featureVector.length; j++) {
                features[i][j] = (float) featureVector[j];
            }
            labels[i] = (float) data.finalPrice;
        }

        // Create DMatrix
        DMatrix dmatrix = new DMatrix(features, labels);

        // Set parameters
        Map<String, Object> params = Map.of(
                "objective", "reg:squarederror",
                "max_depth", 6,
                "eta", 0.1,
                "subsample", 0.8,
                "colsample_bytree", 0.8
        );

        // Train model
        Map<String, DMatrix> watches = Map.of("train", dmatrix);
        int nrounds = 100;
        model = XGBoost.train(dmatrix, params, nrounds, watches, null, null);

        logger.info("XGBoost training completed");
    }

    public Booster getModel() {
        return model;
    }

    public static class TrainingData {
        public Long auctionId;
        public double finalPrice;
        public int daysSinceStart;
        public String category;
        public Long sellerId;
        public double sellerRating;
        public List<Double> historicalPrices;
    }
}