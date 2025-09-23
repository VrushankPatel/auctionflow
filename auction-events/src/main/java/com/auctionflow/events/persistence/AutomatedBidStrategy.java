package com.auctionflow.events.persistence;

import com.auctionflow.bidding.strategies.StrategyType;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

/**
 * Entity representing an automated bidding strategy for a user on an auction
 */
@Entity
@Table(name = "automated_bid_strategies")
public class AutomatedBidStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private String auctionId;

    @Column(name = "bidder_id", nullable = false)
    private String bidderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false)
    private StrategyType strategyType;

    @Column(name = "parameters", columnDefinition = "jsonb")
    private String parametersJson; // Store as JSON

    @Column(name = "max_bid", nullable = false)
    private Double maxBid;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Constructors, getters, setters

    public AutomatedBidStrategy() {}

    public AutomatedBidStrategy(AuctionId auctionId, BidderId bidderId, StrategyType strategyType,
                               Map<String, Object> parameters, Money maxBid) {
        this.auctionId = auctionId.toString();
        this.bidderId = bidderId.toString();
        this.strategyType = strategyType;
        this.parametersJson = serializeParameters(parameters);
        this.maxBid = maxBid.toBigDecimal().doubleValue();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Helper method to serialize parameters (implement with Jackson)
    private String serializeParameters(Map<String, Object> parameters) {
        // TODO: Use Jackson ObjectMapper
        return "{}";
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }

    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }

    public Double getMaxBid() { return maxBid; }
    public void setMaxBid(Double maxBid) { this.maxBid = maxBid; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}