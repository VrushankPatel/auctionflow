package com.auctionflow.api.dtos;

import java.util.List;

public class ActiveAuctionsDTO {
    private List<AuctionSummaryDTO> auctions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    // getters and setters
    public List<AuctionSummaryDTO> getAuctions() { return auctions; }
    public void setAuctions(List<AuctionSummaryDTO> auctions) { this.auctions = auctions; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}