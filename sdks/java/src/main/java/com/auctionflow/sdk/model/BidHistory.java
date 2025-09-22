package com.auctionflow.sdk.model;

import java.util.List;

public class BidHistory {
    private List<Bid> bids;
    private int page;
    private int size;
    private long total;

    public BidHistory() {}

    // getters and setters
    public List<Bid> getBids() { return bids; }
    public void setBids(List<Bid> bids) { this.bids = bids; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}