package com.auctionflow.analytics.jobs;

import com.auctionflow.analytics.dtos.BidderActivityDTO;
import com.auctionflow.analytics.entities.Bid;
import com.auctionflow.analytics.entities.User;
import com.auctionflow.analytics.repositories.BidRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Configuration
public class BidderActivityJobConfig {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    @Bean
    public ItemReader<User> bidderActivityReader() {
        return new RepositoryItemReaderBuilder<User>()
                .name("bidderActivityReader")
                .repository(userRepository)
                .methodName("findAll")
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<User, BidderActivityDTO> bidderActivityProcessor() {
        return user -> {
            List<Bid> bids = bidRepository.findAll().stream()
                    .filter(b -> b.getBidderId().equals(user.getId()))
                    .toList();

            Long totalBids = (long) bids.size();
            Long winningBids = bids.stream()
                    .filter(b -> Boolean.TRUE.equals(b.getAccepted()))
                    .count();
            Double winRate = totalBids > 0 ? (double) winningBids / totalBids : 0.0;

            LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
            Long bidsToday = bidRepository.countBidsByBidderAndDate(user.getId(), today);

            return new BidderActivityDTO(user.getId(), user.getDisplayName(), totalBids, winRate, bidsToday);
        };
    }

    @Bean
    public ItemWriter<BidderActivityDTO> bidderActivityWriter() {
        return items -> {
            for (BidderActivityDTO activity : items) {
                System.out.println("Bidder Activity: " + activity.getBidderName() +
                        " - Total Bids: " + activity.getTotalBids() +
                        " - Win Rate: " + activity.getWinRate() +
                        " - Bids Today: " + activity.getBidsToday());
            }
        };
    }
}