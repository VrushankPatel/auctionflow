package com.auctionflow.common.service;

import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class FeatureFlagService {

    private Unleash unleash;

    @Value("${unleash.app-name:auction-flow}")
    private String appName;

    @Value("${unleash.instance-id:default}")
    private String instanceId;

    @Value("${unleash.api-url:http://localhost:4242/api}")
    private String apiUrl;

    @Value("${unleash.api-token:}")
    private String apiToken;

    @PostConstruct
    public void init() {
        UnleashConfig config = UnleashConfig.builder()
                .appName(appName)
                .instanceId(instanceId)
                .unleashAPI(apiUrl)
                .apiKey(apiToken)
                .build();

        this.unleash = new io.getunleash.DefaultUnleash(config);
    }

    public boolean isEnabled(String featureName) {
        return unleash.isEnabled(featureName);
    }

    public boolean isEnabled(String featureName, UnleashContext context) {
        return unleash.isEnabled(featureName, context);
    }

    public String getVariant(String featureName) {
        return unleash.getVariant(featureName).getName();
    }

    public String getVariant(String featureName, UnleashContext context) {
        return unleash.getVariant(featureName, context).getName();
    }
}