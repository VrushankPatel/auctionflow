package com.auctionflow.analytics.jobs;

import com.auctionflow.analytics.FraudDetectionService;
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

@Configuration
public class FraudDetectionJobConfig {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private MachineLearningService mlService;

    @Autowired
    private FraudDetectionService fraudDetectionService;

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
            // Velocity checks
            FraudAlertDTO velocityAlert = fraudDetectionService.checkVelocity(user);
            if (velocityAlert != null) return velocityAlert;

            // Behavioral analysis
            FraudAlertDTO behavioralAlert = fraudDetectionService.checkBehavioralPatterns(user);
            if (behavioralAlert != null) return behavioralAlert;

            // Pattern recognition
            FraudAlertDTO patternAlert = fraudDetectionService.checkPatterns(user);
            if (patternAlert != null) return patternAlert;

            // Anomaly detection
            FraudAlertDTO anomalyAlert = fraudDetectionService.checkAnomaly(user);
            if (anomalyAlert != null) return anomalyAlert;

            // Fallback to ML scoring
            List<Bid> bids = bidRepository.findByBidderIdAndServerTsGreaterThanEqual(user.getId(), LocalDateTime.now().minusDays(30));
            if (!bids.isEmpty()) {
                double avgBid = bids.stream().mapToDouble(b -> b.getAmount().doubleValue()).average().orElse(0.0);
                FraudScoreRequest mlRequest = new FraudScoreRequest(
                        user.getId().toString(),
                        bids.get(bids.size() - 1).getAuctionId().toString(),
                        bids.get(bids.size() - 1).getAmount().doubleValue(),
                        bids.size(),
                        avgBid
                );

                try {
                    FraudScoreResponse mlResponse = mlService.scoreFraud(mlRequest).block();
                    if (mlResponse != null && mlResponse.getFraudScore() > 0.7) {
                        return new FraudAlertDTO(user.getId(), user.getDisplayName(), "ML Detected Fraud",
                                "Fraud score: " + mlResponse.getFraudScore() + ", Risk: " + mlResponse.getRiskLevel());
                    }
                } catch (Exception e) {
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