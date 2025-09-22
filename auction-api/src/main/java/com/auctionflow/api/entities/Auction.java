package com.auctionflow.api.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "auctions")
public class Auction {
    @Id
    private String id;
    private String itemId;
    private String status;
    private Instant startTs;
    private Instant endTs;
    private String encryptedReservePrice;
    private BigDecimal buyNowPrice;
    private boolean hiddenReserve;

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

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
}