package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ActiveAuctionsDTO {
    private List<AuctionSummaryDTO> auctions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static class AuctionSummaryDTO {
        private String auctionId;
        private String title;
        private BigDecimal currentHighestBid;
        private Instant endTs;

        // getters and setters
        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
        public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

        public Instant getEndTs() { return endTs; }
        public void setEndTs(Instant endTs) { this.endTs = endTs; }
    }

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