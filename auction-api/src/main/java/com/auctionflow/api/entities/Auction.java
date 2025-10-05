package com.auctionflow.api.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "auctions")
@Where(clause = "deleted_at IS NULL")
public class Auction {
    @Id
    private String id;
    @Column(name = "item_id")
    private String itemId;
    private Long sellerId;
    private String status;
    @Column(name = "start_ts")
    private Instant startTs;
    @Column(name = "end_ts")
    private Instant endTs;
    private String encryptedReservePrice;
    @Column(name = "buy_now_price")
    private BigDecimal buyNowPrice;
    @Column(name = "hidden_reserve")
    private boolean hiddenReserve;
    @Column(name = "current_highest_bid")
    private BigDecimal currentHighestBid;
    @Column(name = "current_highest_bidder")
    private Long currentHighestBidder;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Column(name = "deleted_by")
    private Long deletedBy;

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartTs() { return startTs; }
    public void setStartTs(Instant startTs) { this.startTs = startTs; }

    public Instant getEndTs() { return endTs; }
    public void setEndTs(Instant endTs) { this.endTs = endTs; }

    public String getEncryptedReservePrice() { return encryptedReservePrice; }
    public void setEncryptedReservePrice(String encryptedReservePrice) { this.encryptedReservePrice = encryptedReservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public boolean isHiddenReserve() { return hiddenReserve; }
    public void setHiddenReserve(boolean hiddenReserve) { this.hiddenReserve = hiddenReserve; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public Long getCurrentHighestBidder() { return currentHighestBidder; }
    public void setCurrentHighestBidder(Long currentHighestBidder) { this.currentHighestBidder = currentHighestBidder; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Long getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Long deletedBy) { this.deletedBy = deletedBy; }
}