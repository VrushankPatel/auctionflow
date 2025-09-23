package com.auctionflow.analytics;

import com.auctionflow.core.domain.events.*;
import io.opentelemetry.extension.annotations.WithSpan;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RealtimeAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeAnalyticsService.class);

    private static final String TOP_AUCTIONS_BIDS = "top_auctions_bids";
    private static final String AUCTION_PRICES = "auction_prices";
    private static final String TRENDING_CATEGORIES = "trending_categories";
    private static final String AUCTION_CATEGORY = "auction_category";

    private static final long EXPIRATION_SECONDS = 3600; // 1 hour

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zSetOps;

    public RealtimeAnalyticsService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet();
    }

    @KafkaListener(topics = "auction-events", groupId = "analytics-realtime-group")
    @WithSpan("process-auction-event-analytics")
    public void processAuctionEvent(DomainEvent event) {
        if (event instanceof AuctionCreatedEvent) {
            AuctionCreatedEvent auctionEvent = (AuctionCreatedEvent) event;
            String auctionId = auctionEvent.getAggregateId().toString();
            String categoryId = auctionEvent.getCategoryId();
            redisTemplate.opsForHash().put(AUCTION_CATEGORY, auctionId, categoryId);
            redisTemplate.expire(AUCTION_CATEGORY, Duration.ofSeconds(EXPIRATION_SECONDS));
            logger.info("Stored category for auction {}", auctionId);
        }
    }

    @KafkaListener(topics = "bid-events", groupId = "analytics-realtime-group")
    @WithSpan("process-bid-event-analytics")
    public void processBidEvent(DomainEvent event) {
        if (event instanceof BidPlacedEvent) {
            BidPlacedEvent bidEvent = (BidPlacedEvent) event;
            String auctionId = bidEvent.getAggregateId().toString();
            double amount = bidEvent.getAmount().toBigDecimal().doubleValue();

            // Update top auctions by bid count
            zSetOps.incrementScore(TOP_AUCTIONS_BIDS, auctionId, 1);
            redisTemplate.expire(TOP_AUCTIONS_BIDS, Duration.ofSeconds(EXPIRATION_SECONDS));

            // Update auction prices (current highest)
            zSetOps.add(AUCTION_PRICES, auctionId, amount);
            redisTemplate.expire(AUCTION_PRICES, Duration.ofSeconds(EXPIRATION_SECONDS));

            // Update trending categories
            String categoryId = (String) redisTemplate.opsForHash().get(AUCTION_CATEGORY, auctionId);
            if (categoryId != null) {
                zSetOps.incrementScore(TRENDING_CATEGORIES, categoryId, 1);
                redisTemplate.expire(TRENDING_CATEGORIES, Duration.ofSeconds(EXPIRATION_SECONDS));
            }

            logger.info("Updated analytics for bid on auction {}", auctionId);
        }
    }

    // Methods to get data for REST
    public Set<ZSetOperations.TypedTuple<String>> getTopAuctionsByBids(int limit) {
        return zSetOps.reverseRangeWithScores(TOP_AUCTIONS_BIDS, 0, limit - 1);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopAuctionsByPrice(int limit) {
        return zSetOps.reverseRangeWithScores(AUCTION_PRICES, 0, limit - 1);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTrendingCategories(int limit) {
        return zSetOps.reverseRangeWithScores(TRENDING_CATEGORIES, 0, limit - 1);
    }
}