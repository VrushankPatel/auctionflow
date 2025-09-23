package com.auctionflow.api.queries;

public class GetOffersQuery {
    private final String auctionId;

    public GetOffersQuery(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}