package com.auctionflow.api.queries;

public class GetAuctionDetailsQuery {
    private final String auctionId;

    public GetAuctionDetailsQuery(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}