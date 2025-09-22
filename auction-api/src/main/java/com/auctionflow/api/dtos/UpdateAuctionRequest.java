package com.auctionflow.api.dtos;

public class UpdateAuctionRequest {
    private String title;
    private String description;

    // getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}