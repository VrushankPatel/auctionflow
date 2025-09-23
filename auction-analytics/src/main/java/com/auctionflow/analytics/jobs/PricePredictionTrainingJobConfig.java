package com.auctionflow.analytics.jobs;

import com.auctionflow.analytics.entities.Auction;
import com.auctionflow.analytics.entities.Bid;
import com.auctionflow.analytics.entities.Item;
import com.auctionflow.analytics.entities.Seller;
import com.auctionflow.analytics.repositories.AuctionRepository;
import com.auctionflow.analytics.repositories.BidRepository;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import com.auctionflow.analytics.repositories.ItemRepository;
import com.auctionflow.analytics.repositories.SellerRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class PricePredictionTrainingJobConfig {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Bean
    public ItemReader<String> pricePredictionTrainingReader() {
        return new ListItemReader<>(Collections.singletonList("dummy"));
    }

    @Bean
    public ItemProcessor<String, List<TrainingData>> pricePredictionTrainingProcessor() {
        return item -> {
            // Fetch all closed auctions
            List<Auction> closedAuctions = auctionRepository.findByStatusAndEndTsBetween("CLOSED", LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now());

            return closedAuctions.stream()
                    .map(this::extractTrainingData)
                    .filter(data -> data.finalPrice > 0) // Only include auctions with bids
                    .collect(Collectors.toList());
        };
    }

    private TrainingData extractTrainingData(Auction auction) {
        TrainingData data = new TrainingData();

        // Final price: max accepted bid
        List<Bid> bids = bidRepository.findByAuctionId(auction.getId());
        BigDecimal finalPrice = bids.stream()
                .filter(b -> Boolean.TRUE.equals(b.getAccepted()))
                .map(Bid::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        data.finalPrice = finalPrice.doubleValue();

        // Features
        data.auctionId = auction.getId();
        data.daysSinceStart = (int) ChronoUnit.DAYS.between(auction.getStartTs(), LocalDateTime.now());
        data.auctionDurationHours = (int) ChronoUnit.HOURS.between(auction.getStartTs(), auction.getEndTs());
        data.reservePrice = auction.getReservePrice() != null ? auction.getReservePrice().doubleValue() : 0.0;
        data.buyNowPrice = auction.getBuyNowPrice() != null ? auction.getBuyNowPrice().doubleValue() : 0.0;

        // Item and category
        Item item = itemRepository.findById(auction.getItemId()).orElse(null);
        if (item != null) {
            data.category = item.getCategoryId() != null ? item.getCategoryId().toString() : "unknown";
        } else {
            data.category = "unknown";
        }

        // Seller
        if (item != null && item.getSellerId() != null) {
            Seller seller = sellerRepository.findById(item.getSellerId()).orElse(null);
            if (seller != null) {
                data.sellerId = seller.getId();
                data.sellerRating = seller.getRating() != null ? seller.getRating().doubleValue() : 0.0;
            }
        }

        // Historical prices: for simplicity, use reserve and buy now as historical
        data.historicalPrices = List.of(data.reservePrice, data.buyNowPrice);

        return data;
    }

    @Bean
    public ItemWriter<List<TrainingData>> pricePredictionTrainingWriter() {
        return items -> {
            for (List<TrainingData> dataList : items) {
                // Save to CSV file for training
                try (PrintWriter writer = new PrintWriter(new FileWriter("training_data.csv", true))) {
                    for (TrainingData data : dataList) {
                        writer.println(data.auctionId + "," + data.finalPrice);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to save training data: " + e.getMessage());
                }
                System.out.println("Collected " + dataList.size() + " training samples");
            }
        };
    }

    public static class TrainingData {
        public Long auctionId;
        public double finalPrice;
        public int daysSinceStart;
        public int auctionDurationHours;
        public double reservePrice;
        public double buyNowPrice;
        public String category;
        public Long sellerId;
        public double sellerRating;
        public List<Double> historicalPrices;
    }
}