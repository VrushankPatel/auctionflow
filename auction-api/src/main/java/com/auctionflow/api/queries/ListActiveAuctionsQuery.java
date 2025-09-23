package com.auctionflow.api.queries;

import java.util.Optional;

public class ListActiveAuctionsQuery {
    private final Optional<String> category;
    private final Optional<String> sellerId;
    private final Optional<String> query;
    private final int page;
    private final int size;

    public ListActiveAuctionsQuery(Optional<String> category, Optional<String> sellerId, Optional<String> query, int page, int size) {
        this.category = category;
        this.sellerId = sellerId;
        this.query = query;
        this.page = page;
        this.size = size;
    }

    public Optional<String> getCategory() {
        return category;
    }

    public Optional<String> getSellerId() {
        return sellerId;
    }

    public Optional<String> getQuery() {
        return query;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}