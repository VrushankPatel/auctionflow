package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AuctionSummaryDTO {
    private String id;
    private String itemId;
    private Long sellerId;
    private String title;
    private String description;
    private String category;
    private String condition;
    private List<String> images;
    private String auctionType;
    private BigDecimal startingPrice;
    private BigDecimal reservePrice;
    private BigDecimal buyNowPrice;
    private BigDecimal currentHighestBid;
    private Integer bidCount;
    private Boolean hiddenReserve;
    private Instant startTime;
    private Instant endTime;
    private String status;

    public AuctionSummaryDTO() {}

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getAuctionType() { return auctionType; }
    public void setAuctionType(String auctionType) { this.auctionType = auctionType; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public Integer getBidCount() { return bidCount; }
    public void setBidCount(Integer bidCount) { this.bidCount = bidCount; }

    public Boolean getHiddenReserve() { return hiddenReserve; }
    public void setHiddenReserve(Boolean hiddenReserve) { this.hiddenReserve = hiddenReserve; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}