package com.auctionflow.analytics.dtos;

public class FraudAlertDTO {

    private Long bidderId;
    private String bidderName;
    private String pattern;
    private String description;

    public FraudAlertDTO() {}

    public FraudAlertDTO(Long bidderId, String bidderName, String pattern, String description) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.pattern = pattern;
        this.description = description;
    }

    // Getters and setters

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}