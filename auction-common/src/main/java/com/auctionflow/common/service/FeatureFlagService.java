package com.auctionflow.common.service;

import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
public class FeatureFlagService {

    private io.getunleash.Unleash unleash;

    public FeatureFlagService() {
        // Unleash not available, so unleash is null
        this.unleash = null;
    }

    public boolean isEnabled(String featureName) {
        return unleash != null && unleash.isEnabled(featureName);
    }

    public boolean isEnabled(String featureName, UnleashContext context) {
        return unleash != null && unleash.isEnabled(featureName, context);
    }

    public String getVariant(String featureName) {
        return unleash != null ? unleash.getVariant(featureName).getName() : "default";
    }

    public String getVariant(String featureName, UnleashContext context) {
        return unleash != null ? unleash.getVariant(featureName, context).getName() : "default";
    }
}