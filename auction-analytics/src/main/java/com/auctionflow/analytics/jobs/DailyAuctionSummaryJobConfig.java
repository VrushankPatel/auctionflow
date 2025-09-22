package com.auctionflow.analytics.jobs;

import com.auctionflow.analytics.dtos.AuctionSummaryDTO;
import com.auctionflow.analytics.entities.Auction;
import com.auctionflow.analytics.entities.Bid;
import com.auctionflow.analytics.repositories.AuctionRepository;
import com.auctionflow.analytics.repositories.BidRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Configuration
public class DailyAuctionSummaryJobConfig {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Bean
    public ItemReader<String> dailyAuctionSummaryReader() {
        return new ListItemReader<>(Collections.singletonList("dummy"));
    }

    @Bean
    public ItemProcessor<String, AuctionSummaryDTO> dailyAuctionSummaryProcessor() {
        return item -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

            List<Auction> auctions = auctionRepository.findByStatusAndEndTsBetween("CLOSED", startOfDay, endOfDay);
            Long totalAuctions = (long) auctions.size();
            Long totalBids = auctions.stream()
                    .mapToLong(a -> bidRepository.findByAuctionId(a.getId()).size())
                    .sum();
            BigDecimal totalRevenue = auctions.stream()
                    .map(a -> bidRepository.findByAuctionId(a.getId()).stream()
                            .filter(b -> Boolean.TRUE.equals(b.getAccepted()))
                            .map(Bid::getAmount)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new AuctionSummaryDTO(yesterday, totalAuctions, totalBids, totalRevenue);
        };
    }

    @Bean
    public ItemWriter<AuctionSummaryDTO> dailyAuctionSummaryWriter() {
        return items -> {
            for (AuctionSummaryDTO summary : items) {
                System.out.println("Daily Auction Summary: " + summary.getDate() +
                        " - Auctions: " + summary.getTotalAuctions() +
                        " - Bids: " + summary.getTotalBids() +
                        " - Revenue: " + summary.getTotalRevenue());
            }
        };
    }
}