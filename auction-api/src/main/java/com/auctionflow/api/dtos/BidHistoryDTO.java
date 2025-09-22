package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BidHistoryDTO {
    private List<BidDTO> bids;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static class BidDTO {
        private String bidderId;
        private BigDecimal amount;
        private Instant serverTs;
        private long seqNo;
        private boolean accepted;

        // getters and setters
        public String getBidderId() { return bidderId; }
        public void setBidderId(String bidderId) { this.bidderId = bidderId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Instant getServerTs() { return serverTs; }
        public void setServerTs(Instant serverTs) { this.serverTs = serverTs; }

        public long getSeqNo() { return seqNo; }
        public void setSeqNo(long seqNo) { this.seqNo = seqNo; }

        public boolean isAccepted() { return accepted; }
        public void setAccepted(boolean accepted) { this.accepted = accepted; }
    }

    // getters and setters
    public List<BidDTO> getBids() { return bids; }
    public void setBids(List<BidDTO> bids) { this.bids = bids; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}