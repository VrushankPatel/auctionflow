package com.auctionflow.bidding.strategies;

import java.util.Map;

/**
 * Parameters for bidding strategies
 */
public class StrategyParameters {
    private final Map<String, Object> params;

    public StrategyParameters(Map<String, Object> params) {
        this.params = params;
    }

    public Object get(String key) {
        return params.get(key);
    }

    public String getString(String key) {
        return (String) params.get(key);
    }

    public Double getDouble(String key) {
        return (Double) params.get(key);
    }

    public Integer getInt(String key) {
        return (Integer) params.get(key);
    }

    public Boolean getBoolean(String key) {
        return (Boolean) params.get(key);
    }
}