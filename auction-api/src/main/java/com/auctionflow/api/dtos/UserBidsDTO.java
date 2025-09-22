package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class UserBidsDTO {
    private List<UserBidDTO> bids;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static class UserBidDTO {
        private String auctionId;
        private String auctionTitle;
        private BigDecimal amount;
        private Instant serverTs;
        private boolean accepted;
        private String status; // auction status

        // getters and setters
        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

        public String getAuctionTitle() { return auctionTitle; }
        public void setAuctionTitle(String auctionTitle) { this.auctionTitle = auctionTitle; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Instant getServerTs() { return serverTs; }
        public void setServerTs(Instant serverTs) { this.serverTs = serverTs; }

        public boolean isAccepted() { return accepted; }
        public void setAccepted(boolean accepted) { this.accepted = accepted; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // getters and setters
    public List<UserBidDTO> getBids() { return bids; }
    public void setBids(List<UserBidDTO> bids) { this.bids = bids; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}