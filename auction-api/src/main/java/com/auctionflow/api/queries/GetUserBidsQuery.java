package com.auctionflow.api.queries;

public class GetUserBidsQuery {
    private final String userId;
    private final int page;
    private final int size;

    public GetUserBidsQuery(String userId, int page, int size) {
        this.userId = userId;
        this.page = page;
        this.size = size;
    }

    public String getUserId() {
        return userId;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}