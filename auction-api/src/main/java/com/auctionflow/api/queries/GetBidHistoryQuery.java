package com.auctionflow.api.queries;

public class GetBidHistoryQuery {
    private final String auctionId;
    private final int page;
    private final int size;

    public GetBidHistoryQuery(String auctionId, int page, int size) {
        this.auctionId = auctionId;
        this.page = page;
        this.size = size;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}