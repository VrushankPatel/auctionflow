package com.auctionflow.analytics;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class ABTestingService {

    private final Random random = new Random();
    private final Map<String, String[]> experiments = new HashMap<>();

    public ABTestingService() {
        // Define experiments
        experiments.put("recommendation_algorithm", new String[]{"collaborative", "content_based", "hybrid"});
    }

    public String assignVariant(String experiment, String userId) {
        String[] variants = experiments.get(experiment);
        if (variants == null) return "default";
        int hash = userId.hashCode();
        int index = Math.abs(hash) % variants.length;
        return variants[index];
    }

    public void logEvent(String experiment, String variant, String userId, String eventType, Map<String, Object> data) {
        // Log to DB or Kafka
        // For simplicity, print
        System.out.println("AB Test Event: " + experiment + " variant: " + variant + " user: " + userId + " event: " + eventType + " data: " + data);
    }
}