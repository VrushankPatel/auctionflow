package com.auctionflow.analytics;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeatureEngineeringService {

    private final Map<String, Integer> categoryEncoder = new HashMap<>();
    private int nextCategoryId = 0;

    public double[] extractFeatures(String auctionId, List<Double> historicalPrices, String category,
                                   int daysSinceStart, Long sellerId, Double sellerRating) {
        // Features: historical_avg, historical_std, category_encoded, daysSinceStart, sellerRating, sellerId_hash
        double historicalAvg = historicalPrices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double historicalStd = calculateStdDev(historicalPrices, historicalAvg);

        int categoryId = categoryEncoder.computeIfAbsent(category, k -> nextCategoryId++);

        // Simple hash for sellerId
        int sellerIdHash = sellerId != null ? sellerId.hashCode() % 1000 : 0; // Mod to keep small

        double sellerRatingVal = sellerRating != null ? sellerRating : 0.0;

        return new double[] {
            historicalAvg,
            historicalStd,
            categoryId,
            daysSinceStart,
            sellerRatingVal,
            sellerIdHash
        };
    }

    private double calculateStdDev(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;
        double sumSquaredDiffs = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiffs / (values.size() - 1));
    }
}