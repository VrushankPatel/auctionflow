package com.auctionflow.api.dtos;

import java.util.List;

public class MobileAuctionsDTO {
    private List<MobileAuctionDTO> auctions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public MobileAuctionsDTO() {}

    public MobileAuctionsDTO(List<MobileAuctionDTO> auctions, int page, int size, long totalElements, int totalPages) {
        this.auctions = auctions;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    // Getters and setters
    public List<MobileAuctionDTO> getAuctions() { return auctions; }
    public void setAuctions(List<MobileAuctionDTO> auctions) { this.auctions = auctions; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}