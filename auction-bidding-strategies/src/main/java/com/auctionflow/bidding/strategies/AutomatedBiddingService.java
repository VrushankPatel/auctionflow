package com.auctionflow.bidding.strategies;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;
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
    public AutomatedBidStrategy createStrategy(AuctionId auctionId, BidderId bidderId,
                                             StrategyType type, Map<String, Object> params, Money maxBid) {
        AutomatedBidStrategy strategy = new AutomatedBidStrategy(auctionId, bidderId, type, params, maxBid);
        return strategyRepository.save(strategy);
    }

    /**
     * Evaluate all active strategies for an auction and return bidding decisions with strategy info
     */
    public List<StrategyBidDecision> evaluateStrategies(AuctionId auctionId, Money currentHighestBid, Instant auctionEndTime) {
        List<AutomatedBidStrategy> activeStrategies = strategyRepository.findActiveByAuctionId(auctionId.value());

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
                    return new StrategyBidDecision(strategy, decision);
                }
                return null;
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    /**
     * Get strategies for a bidder
     */
    public List<AutomatedBidStrategy> getStrategiesForBidder(String bidderId, boolean activeOnly) {
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
        AutomatedBidStrategy strategy = strategyRepository.findById(strategyId).orElseThrow();
        strategy.setIsActive(false);
        strategyRepository.save(strategy);
    }

    // Helper method
    private Map<String, Object> deserializeParameters(String json) {
        // TODO: Use Jackson ObjectMapper
        return new HashMap<>();
    }
}