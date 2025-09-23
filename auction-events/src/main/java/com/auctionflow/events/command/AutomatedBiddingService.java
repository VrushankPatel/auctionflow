package com.auctionflow.events.command;

import com.auctionflow.bidding.strategies.*;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.events.persistence.AutomatedBidStrategyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing automated bidding strategies
 */
@Service
public class AutomatedBiddingService {

    private final Map<StrategyType, BiddingStrategy> strategies = new HashMap<>();
    private final AutomatedBidStrategyRepository strategyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AutomatedBiddingService(AutomatedBidStrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;

        // Register strategies
        strategies.put(StrategyType.SNIPING_PREVENTION, new SnipingPreventionStrategy());
        strategies.put(StrategyType.OPTIMAL_TIMING, new OptimalTimingStrategy());
        strategies.put(StrategyType.BUDGET_OPTIMIZATION, new BudgetOptimizationStrategy());
        strategies.put(StrategyType.REINFORCEMENT_LEARNING, new ReinforcementLearningStrategy());
    }

    /**
     * Create a new automated bidding strategy
     */
    public com.auctionflow.events.persistence.AutomatedBidStrategy createStrategy(AuctionId auctionId, BidderId bidderId,
                                              StrategyType type, Map<String, Object> params, Money maxBid) {
        com.auctionflow.events.persistence.AutomatedBidStrategy strategy = new com.auctionflow.events.persistence.AutomatedBidStrategy(auctionId, bidderId, type, params, maxBid);
        return strategyRepository.save(strategy);
    }

    /**
     * Evaluate all active strategies for an auction and return bidding decisions with strategy info
     */
    public List<StrategyBidDecision> evaluateStrategies(AuctionId auctionId, Money currentHighestBid, Instant auctionEndTime) {
        List<com.auctionflow.events.persistence.AutomatedBidStrategy> activeStrategies = strategyRepository.findActiveByAuctionId(auctionId.toString());

        return activeStrategies.stream()
            .map(strategy -> {
                BiddingStrategy biddingStrategy = strategies.get(strategy.getStrategyType());
                if (biddingStrategy == null) {
                    return null;
                }

                StrategyParameters params = new StrategyParameters(deserializeParameters(strategy.getParametersJson()));
                BidDecision decision = biddingStrategy.decideBid(auctionId, BidderId.fromString(strategy.getBidderId()),
                                                currentHighestBid, auctionEndTime, params);
                if (decision.shouldBid()) {
                    com.auctionflow.bidding.strategies.AutomatedBidStrategy domainStrategy = new com.auctionflow.bidding.strategies.AutomatedBidStrategy(strategy.getId().toString(),
                        AuctionId.fromString(strategy.getAuctionId()), BidderId.fromString(strategy.getBidderId()), strategy.getStrategyType());
                    return new StrategyBidDecision(domainStrategy, decision);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Get strategies for a bidder
     */
    public List<com.auctionflow.events.persistence.AutomatedBidStrategy> getStrategiesForBidder(String bidderId, boolean activeOnly) {
        if (activeOnly) {
            return strategyRepository.findByBidderIdAndIsActive(bidderId, true);
        } else {
            return strategyRepository.findByBidderId(bidderId);
        }
    }

    /**
     * Deactivate a strategy
     */
    public void deactivateStrategy(Long strategyId) {
        com.auctionflow.events.persistence.AutomatedBidStrategy strategy = strategyRepository.findById(strategyId).orElseThrow();
        strategy.setIsActive(false);
        strategyRepository.save(strategy);
    }

    // Helper method
    private Map<String, Object> deserializeParameters(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize parameters", e);
        }
    }
}