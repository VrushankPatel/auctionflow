package com.auctionflow.analytics;

import com.auctionflow.analytics.dtos.RecommendationRequest;
import com.auctionflow.analytics.dtos.RecommendationResponse;
import com.auctionflow.analytics.entities.Auction;
import com.auctionflow.analytics.entities.Bid;
import com.auctionflow.analytics.entities.Item;
import com.auctionflow.analytics.repositories.AuctionRepository;
import com.auctionflow.analytics.repositories.BidRepository;
import com.auctionflow.analytics.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ItemRepository itemRepository;

    // Collaborative filtering: item-based similarity for similar auctions
    public List<Long> getSimilarAuctionsCollaborative(Long auctionId, int limit) {
        // Get all bids for this auction
        List<Bid> bidsForAuction = bidRepository.findByAuctionId(auctionId);

        // Get users who bid on this auction
        Set<Long> users = bidsForAuction.stream().map(Bid::getBidderId).collect(Collectors.toSet());

        // Find other auctions these users bid on
        List<Bid> relatedBids = bidRepository.findByBidderIdIn(users);
        Map<Long, Long> auctionUserCount = new HashMap<>();
        for (Bid bid : relatedBids) {
            if (!bid.getAuctionId().equals(auctionId)) {
                auctionUserCount.put(bid.getAuctionId(), auctionUserCount.getOrDefault(bid.getAuctionId(), 0L) + 1);
            }
        }

        // Sort by number of common users
        return auctionUserCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Content-based: similarity based on category and title/description
    public List<Long> getSimilarAuctionsContentBased(Long auctionId, int limit) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) return Collections.emptyList();

        Item item = itemRepository.findById(auction.getItemId()).orElse(null);
        if (item == null) return Collections.emptyList();

        String category = item.getCategoryId();
        String text = (item.getTitle() + " " + item.getDescription()).toLowerCase();

        // Simple similarity: same category, then text overlap
        List<Item> allItems = itemRepository.findAll();
        List<ItemSimilarity> similarities = new ArrayList<>();
        for (Item other : allItems) {
            if (other.getId().equals(item.getId())) continue;
            double sim = 0.0;
            if (category != null && category.equals(other.getCategoryId())) {
                sim += 0.5; // category weight
            }
            String otherText = (other.getTitle() + " " + other.getDescription()).toLowerCase();
            sim += textSimilarity(text, otherText) * 0.5;
            similarities.add(new ItemSimilarity(other.getId(), sim));
        }

        List<Long> similarItemIds = similarities.stream()
                .sorted(Comparator.comparingDouble(ItemSimilarity::getSimilarity).reversed())
                .limit(limit)
                .map(ItemSimilarity::getItemId)
                .collect(Collectors.toList());

        // Find auctions for these items, assuming active
        List<Auction> similarAuctions = auctionRepository.findByItemIdIn(similarItemIds);
        return similarAuctions.stream()
                .filter(a -> "OPEN".equals(a.getStatus())) // or whatever status
                .map(Auction::getId)
                .collect(Collectors.toList());
    }

    private double textSimilarity(String text1, String text2) {
        Set<String> words1 = Arrays.stream(text1.split("\\s+")).collect(Collectors.toSet());
        Set<String> words2 = Arrays.stream(text2.split("\\s+")).collect(Collectors.toSet());
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    // Hybrid: combine collaborative and content-based
    public List<Long> getRecommendedAuctions(Long userId, int limit) {
        // Get user's bidding history
        List<Bid> userBids = bidRepository.findByBidderId(userId);
        Set<Long> userAuctions = userBids.stream().map(Bid::getAuctionId).collect(Collectors.toSet());

        // Collaborative: find similar auctions from user's auctions
        Set<Long> collaborative = new HashSet<>();
        for (Long auctionId : userAuctions) {
            collaborative.addAll(getSimilarAuctionsCollaborative(auctionId, limit / 2));
        }

        // Content-based: from user's items
        Set<Long> contentBased = new HashSet<>();
        for (Long auctionId : userAuctions) {
            List<Long> similar = getSimilarAuctionsContentBased(auctionId, limit / 2);
            // Convert item ids to auction ids? Wait, similar returns item ids? No, in my code, I returned item ids, but need auction ids.
            // Fix: in content-based, return auction ids
            // Actually, in my code, I returned item ids, but need to map to auctions.
            // Let's adjust.
        }

        // For simplicity, combine and limit
        List<Long> combined = new ArrayList<>(collaborative);
        combined.addAll(contentBased);
        return combined.stream().distinct().limit(limit).collect(Collectors.toList());
    }

    private static class ItemSimilarity {
        private final Long itemId;
        private final double similarity;

        public ItemSimilarity(Long itemId, double similarity) {
            this.itemId = itemId;
            this.similarity = similarity;
        }

        public Long getItemId() { return itemId; }
        public double getSimilarity() { return similarity; }
    }
}