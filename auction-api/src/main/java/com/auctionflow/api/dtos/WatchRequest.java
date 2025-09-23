package com.auctionflow.api.dtos;

import jakarta.validation.constraints.NotNull;

public class WatchRequest {
    @NotNull
    private String userId;

    public WatchRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}