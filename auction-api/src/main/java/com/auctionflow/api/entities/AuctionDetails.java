package com.auctionflow.api.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "auction_details")
public class AuctionDetails {

    @Id
    @Column(name = "auction_id")
    private String auctionId;

    @Column(name = "item_id")
    private String itemId;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "images", columnDefinition = "jsonb")
    private String images;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "auction_type")
    private String auctionType;

    @Column(name = "start_ts")
    private Instant startTs;

    @Column(name = "end_ts")
    private Instant endTs;

    @Column(name = "status")
    private String status;

    @Column(name = "reserve_price")
    private BigDecimal reservePrice;

    @Column(name = "buy_now_price")
    private BigDecimal buyNowPrice;

    @Column(name = "increment_strategy")
    private String incrementStrategy;

    @Column(name = "extension_policy")
    private String extensionPolicy;

    @Column(name = "current_highest_bid")
    private BigDecimal currentHighestBid;

    @Column(name = "highest_bidder_id")
    private String highestBidderId;

    @Column(name = "bid_count")
    private Integer bidCount;

    @Column(name = "created_at")
    private Instant createdAt;

    // Constructors
    public AuctionDetails() {}

    public AuctionDetails(String auctionId, String itemId, String sellerId, String title,
                         String description, String category, String auctionType,
                         Instant startTs, Instant endTs, String status,
                         BigDecimal reservePrice, BigDecimal buyNowPrice,
                         String incrementStrategy, String extensionPolicy,
                         BigDecimal currentHighestBid, String highestBidderId,
                         Integer bidCount, Instant createdAt) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.auctionType = auctionType;
        this.startTs = startTs;
        this.endTs = endTs;
        this.status = status;
        this.reservePrice = reservePrice;
        this.buyNowPrice = buyNowPrice;
        this.incrementStrategy = incrementStrategy;
        this.extensionPolicy = extensionPolicy;
        this.currentHighestBid = currentHighestBid;
        this.highestBidderId = highestBidderId;
        this.bidCount = bidCount;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getAuctionType() { return auctionType; }
    public void setAuctionType(String auctionType) { this.auctionType = auctionType; }

    public Instant getStartTs() { return startTs; }
    public void setStartTs(Instant startTs) { this.startTs = startTs; }

    public Instant getEndTs() { return endTs; }
    public void setEndTs(Instant endTs) { this.endTs = endTs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public String getIncrementStrategy() { return incrementStrategy; }
    public void setIncrementStrategy(String incrementStrategy) { this.incrementStrategy = incrementStrategy; }

    public String getExtensionPolicy() { return extensionPolicy; }
    public void setExtensionPolicy(String extensionPolicy) { this.extensionPolicy = extensionPolicy; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public Integer getBidCount() { return bidCount; }
    public void setBidCount(Integer bidCount) { this.bidCount = bidCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}