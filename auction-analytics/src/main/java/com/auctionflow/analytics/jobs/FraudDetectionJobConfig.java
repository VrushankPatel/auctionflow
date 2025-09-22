package com.auctionflow.analytics.jobs;

import com.auctionflow.analytics.MachineLearningService;
import com.auctionflow.analytics.dtos.FraudAlertDTO;
import com.auctionflow.analytics.dtos.FraudScoreRequest;
import com.auctionflow.analytics.dtos.FraudScoreResponse;
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
import java.util.OptionalDouble;

@Configuration
public class FraudDetectionJobConfig {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private MachineLearningService mlService;

    @Bean
    public ItemReader<User> fraudDetectionReader() {
        return new RepositoryItemReaderBuilder<User>()
                .name("fraudDetectionReader")
                .repository(userRepository)
                .methodName("findAll")
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<User, FraudAlertDTO> fraudDetectionProcessor() {
        return user -> {
            List<Bid> bids = bidRepository.findAll().stream()
                    .filter(b -> b.getBidderId().equals(user.getId()))
                    .toList();

            // Simple fraud detection: high bid frequency in last hour
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long recentBids = bids.stream()
                    .filter(b -> b.getServerTs().isAfter(oneHourAgo))
                    .count();

            if (recentBids > 10) { // threshold
                return new FraudAlertDTO(user.getId(), user.getDisplayName(), "High Frequency Bidding",
                        "Bidder placed " + recentBids + " bids in the last hour.");
            }

            // Another pattern: winning too many auctions
            long winningBids = bids.stream()
                    .filter(b -> Boolean.TRUE.equals(b.getAccepted()))
                    .count();

            if (winningBids > bids.size() * 0.8) { // win rate > 80%
                return new FraudAlertDTO(user.getId(), user.getDisplayName(), "High Win Rate",
                        "Bidder has " + winningBids + " wins out of " + bids.size() + " bids.");
            }

            // ML-based fraud scoring
            if (!bids.isEmpty()) {
                OptionalDouble avgBid = bids.stream().mapToDouble(b -> b.getAmount().doubleValue()).average();
                FraudScoreRequest mlRequest = new FraudScoreRequest(
                        user.getId().toString(),
                        bids.get(0).getAuctionId().toString(), // assuming one auction for simplicity
                        bids.get(bids.size() - 1).getAmount().doubleValue(), // latest bid
                        bids.size(),
                        avgBid.orElse(0.0)
                );

                try {
                    FraudScoreResponse mlResponse = mlService.scoreFraud(mlRequest).block(); // synchronous call
                    if (mlResponse != null && mlResponse.getFraudScore() > 0.7) { // high risk
                        return new FraudAlertDTO(user.getId(), user.getDisplayName(), "ML Detected Fraud",
                                "Fraud score: " + mlResponse.getFraudScore() + ", Risk: " + mlResponse.getRiskLevel());
                    }
                } catch (Exception e) {
                    // Log error, but don't fail the job
                    System.err.println("Error calling ML service for user " + user.getId() + ": " + e.getMessage());
                }
            }

            return null; // no fraud
        };
    }

    @Bean
    public ItemWriter<FraudAlertDTO> fraudDetectionWriter() {
        return items -> {
            for (FraudAlertDTO alert : items) {
                if (alert != null) {
                    System.out.println("Fraud Alert: " + alert.getBidderName() +
                            " - Pattern: " + alert.getPattern() +
                            " - Description: " + alert.getDescription());
                }
            }
        };
    }
}