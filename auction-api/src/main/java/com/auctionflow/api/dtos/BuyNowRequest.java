package com.auctionflow.api.dtos;

import javax.validation.constraints.NotNull;

public class BuyNowRequest {
    @NotNull
    private String userId;

    public BuyNowRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}