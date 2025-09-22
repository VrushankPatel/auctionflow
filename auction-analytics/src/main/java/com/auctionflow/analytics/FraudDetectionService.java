package com.auctionflow.analytics;

import com.auctionflow.analytics.dtos.FraudAlertDTO;
import com.auctionflow.analytics.entities.Auction;
import com.auctionflow.analytics.entities.Bid;
import com.auctionflow.analytics.entities.Item;
import com.auctionflow.analytics.entities.Seller;
import com.auctionflow.analytics.entities.User;
import com.auctionflow.analytics.repositories.AuctionRepository;
import com.auctionflow.analytics.repositories.BidRepository;
import com.auctionflow.analytics.repositories.ItemRepository;
import com.auctionflow.analytics.repositories.SellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FraudDetectionService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private MachineLearningService mlService;

    public FraudAlertDTO checkVelocity(User user) {
        LocalDateTime now = LocalDateTime.now();

        // Check bids in last hour
        long bidsLastHour = bidRepository.countBidsByBidderSince(user.getId(), now.minusHours(1));
        if (bidsLastHour > 50) { // threshold
            return new FraudAlertDTO(user.getId(), user.getDisplayName(), "High Velocity Bidding",
                    "Placed " + bidsLastHour + " bids in the last hour.");
        }

        // Check bids in last minute
        long bidsLastMinute = bidRepository.countBidsByBidderSince(user.getId(), now.minusMinutes(1));
        if (bidsLastMinute > 10) {
            return new FraudAlertDTO(user.getId(), user.getDisplayName(), "Burst Bidding",
                    "Placed " + bidsLastMinute + " bids in the last minute.");
        }

        return null;
    }

    public FraudAlertDTO checkBehavioralPatterns(User user) {
        List<Bid> recentBids = bidRepository.findByBidderIdAndServerTsGreaterThanEqual(user.getId(), LocalDateTime.now().minusDays(7));

        if (recentBids.size() < 5) return null; // not enough data

        // Check for sudden increase in bid amounts
        double avgBid = recentBids.stream().mapToDouble(b -> b.getAmount().doubleValue()).average().orElse(0);
        double maxBid = recentBids.stream().mapToDouble(b -> b.getAmount().doubleValue()).max().orElse(0);
        if (maxBid > avgBid * 3) {
            return new FraudAlertDTO(user.getId(), user.getDisplayName(), "Sudden Bid Increase",
                    "Recent max bid " + maxBid + " is 3x average " + avgBid);
        }

        // Check win rate
        long wins = recentBids.stream().filter(b -> Boolean.TRUE.equals(b.getAccepted())).count();
        double winRate = (double) wins / recentBids.size();
        if (winRate > 0.9) {
            return new FraudAlertDTO(user.getId(), user.getDisplayName(), "High Win Rate",
                    "Win rate: " + String.format("%.2f", winRate) + " (" + wins + "/" + recentBids.size() + ")");
        }

        return null;
    }

    public FraudAlertDTO checkPatterns(User user) {
        // Check for shilling: bidding on own auctions
        List<Seller> sellers = sellerRepository.findByUserId(user.getId());
        if (!sellers.isEmpty()) {
            Long sellerId = sellers.get(0).getId(); // assume one seller per user
            List<Item> items = itemRepository.findBySellerId(sellerId);
            List<Long> itemIds = items.stream().map(Item::getId).collect(java.util.stream.Collectors.toList());
            List<Auction> auctions = auctionRepository.findByItemIdIn(itemIds);

            List<Bid> userBids = bidRepository.findByBidderIdAndServerTsGreaterThanEqual(user.getId(), LocalDateTime.now().minusDays(30));
            for (Bid bid : userBids) {
                if (auctions.stream().anyMatch(a -> a.getId().equals(bid.getAuctionId()))) {
                    return new FraudAlertDTO(user.getId(), user.getDisplayName(), "Shilling Detected",
                            "Bidder placed bid on own auction: " + bid.getAuctionId());
                }
            }
        }

        // Check for coordinated bidding: multiple bids at same timestamp
        List<Bid> recentBids = bidRepository.findByBidderIdAndServerTsGreaterThanEqual(user.getId(), LocalDateTime.now().minusHours(1));
        // Group by timestamp and check for bursts
        // Simple check: if more than 5 bids in a minute
        // Already in velocity, but here for pattern

        return null;
    }

    public FraudAlertDTO checkAnomaly(User user) {
        // Use ML for anomaly detection with isolation forest
        List<Bid> bids = bidRepository.findByBidderIdAndServerTsGreaterThanEqual(user.getId(), LocalDateTime.now().minusDays(30));
        if (bids.isEmpty()) return null;

        LocalDateTime now = LocalDateTime.now();
        double avgBid = bids.stream().mapToDouble(b -> b.getAmount().doubleValue()).average().orElse(0);
        DoubleSummaryStatistics stats = bids.stream().mapToDouble(b -> b.getAmount().doubleValue()).summaryStatistics();
        double stdDev = Math.sqrt(bids.stream().mapToDouble(b -> Math.pow(b.getAmount().doubleValue() - avgBid, 2)).average().orElse(0));

        long bidsLastHour = bidRepository.countBidsByBidderSince(user.getId(), now.minusHours(1));
        long bidsLastMinute = bidRepository.countBidsByBidderSince(user.getId(), now.minusMinutes(1));

        long timeSinceLastBid = bids.isEmpty() ? 0 : ChronoUnit.SECONDS.between(bids.get(bids.size() - 1).getServerTs(), now);

        Long lastAuctionId = bids.get(bids.size() - 1).getAuctionId();
        double lastBidAmount = bids.get(bids.size() - 1).getAmount().doubleValue();

        FraudScoreRequest mlRequest = new FraudScoreRequest(
                user.getId().toString(),
                lastAuctionId.toString(),
                lastBidAmount,
                bids.size(),
                avgBid,
                bidsLastHour,
                bidsLastMinute,
                stdDev,
                timeSinceLastBid
        );

        try {
            FraudScoreResponse mlResponse = mlService.scoreFraud(mlRequest).block();
            if (mlResponse != null && mlResponse.getFraudScore() > 0.8) { // higher threshold for anomaly
                return new FraudAlertDTO(user.getId(), user.getDisplayName(), "Anomaly Detected",
                        "Isolation Forest score: " + mlResponse.getFraudScore() + ", Risk: " + mlResponse.getRiskLevel());
            }
        } catch (Exception e) {
            System.err.println("Error calling ML service for anomaly detection on user " + user.getId() + ": " + e.getMessage());
        }

        return null;
    }
}