package com.auctionflow.analytics.jobs;

import com.auctionflow.analytics.dtos.SellerPerformanceDTO;
import com.auctionflow.analytics.entities.Auction;
import com.auctionflow.analytics.entities.Bid;
import com.auctionflow.analytics.entities.Item;
import com.auctionflow.analytics.entities.Seller;
import com.auctionflow.analytics.entities.User;
import com.auctionflow.analytics.repositories.AuctionRepository;
import com.auctionflow.analytics.repositories.BidRepository;
import com.auctionflow.analytics.repositories.ItemRepository;
import com.auctionflow.analytics.repositories.SellerRepository;
import com.auctionflow.analytics.repositories.UserRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Configuration
public class SellerPerformanceJobConfig {

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @Bean
    public ItemReader<Seller> sellerPerformanceReader() {
        return new RepositoryItemReaderBuilder<Seller>()
                .name("sellerPerformanceReader")
                .repository(sellerRepository)
                .methodName("findAll")
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Seller, SellerPerformanceDTO> sellerPerformanceProcessor() {
        return seller -> {
            User user = userRepository.findById(seller.getUserId()).orElse(null);
            String sellerName = user != null ? user.getDisplayName() : "Unknown";

            List<Item> items = itemRepository.findBySellerId(seller.getId());
            List<Long> itemIds = items.stream().map(Item::getId).toList();

            List<Auction> auctions = auctionRepository.findAll().stream()
                    .filter(a -> itemIds.contains(a.getItemId()))
                    .toList();

            Long totalAuctions = (long) auctions.size();
            Long successfulAuctions = auctions.stream()
                    .filter(a -> "CLOSED".equals(a.getStatus()))
                    .count();

            BigDecimal totalRevenue = auctions.stream()
                    .filter(a -> "CLOSED".equals(a.getStatus()))
                    .map(a -> bidRepository.findByAuctionId(a.getId()).stream()
                            .filter(b -> Boolean.TRUE.equals(b.getAccepted()))
                            .map(Bid::getAmount)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal averageRating = seller.getRating();

            return new SellerPerformanceDTO(seller.getId(), sellerName, totalAuctions, successfulAuctions, totalRevenue, averageRating);
        };
    }

    @Bean
    public ItemWriter<SellerPerformanceDTO> sellerPerformanceWriter() {
        return items -> {
            for (SellerPerformanceDTO performance : items) {
                System.out.println("Seller Performance: " + performance.getSellerName() +
                        " - Total Auctions: " + performance.getTotalAuctions() +
                        " - Successful: " + performance.getSuccessfulAuctions() +
                        " - Revenue: " + performance.getTotalRevenue() +
                        " - Rating: " + performance.getAverageRating());
            }
        };
    }
}