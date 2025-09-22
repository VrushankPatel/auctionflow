package com.auctionflow.analytics.config;

import com.auctionflow.analytics.entities.Seller;
import com.auctionflow.analytics.entities.User;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public BatchConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    // Daily Auction Summary Job
    @Bean
    public Job dailyAuctionSummaryJob(Step dailyAuctionSummaryStep) {
        return jobBuilderFactory.get("dailyAuctionSummaryJob")
                .incrementer(new RunIdIncrementer())
                .start(dailyAuctionSummaryStep)
                .build();
    }

    @Bean
    public Step dailyAuctionSummaryStep(ItemReader<String> dailyAuctionSummaryReader,
                                        ItemProcessor<String, ?> dailyAuctionSummaryProcessor,
                                        ItemWriter<?> dailyAuctionSummaryWriter) {
        return stepBuilderFactory.get("dailyAuctionSummaryStep")
                .<String, Object>chunk(10)
                .reader(dailyAuctionSummaryReader)
                .processor(dailyAuctionSummaryProcessor)
                .writer(dailyAuctionSummaryWriter)
                .build();
    }

    // Seller Performance Reports Job
    @Bean
    public Job sellerPerformanceJob(Step sellerPerformanceStep) {
        return jobBuilderFactory.get("sellerPerformanceJob")
                .incrementer(new RunIdIncrementer())
                .start(sellerPerformanceStep)
                .build();
    }

    @Bean
    public Step sellerPerformanceStep(ItemReader<Seller> sellerPerformanceReader,
                                      ItemProcessor<Seller, ?> sellerPerformanceProcessor,
                                      ItemWriter<?> sellerPerformanceWriter) {
        return stepBuilderFactory.get("sellerPerformanceStep")
                .<Seller, Object>chunk(10)
                .reader(sellerPerformanceReader)
                .processor(sellerPerformanceProcessor)
                .writer(sellerPerformanceWriter)
                .build();
    }

    // Bidder Activity Analysis Job
    @Bean
    public Job bidderActivityJob(Step bidderActivityStep) {
        return jobBuilderFactory.get("bidderActivityJob")
                .incrementer(new RunIdIncrementer())
                .start(bidderActivityStep)
                .build();
    }

    @Bean
    public Step bidderActivityStep(ItemReader<User> bidderActivityReader,
                                   ItemProcessor<User, ?> bidderActivityProcessor,
                                   ItemWriter<?> bidderActivityWriter) {
        return stepBuilderFactory.get("bidderActivityStep")
                .<User, Object>chunk(10)
                .reader(bidderActivityReader)
                .processor(bidderActivityProcessor)
                .writer(bidderActivityWriter)
                .build();
    }

    // Fraud Detection Patterns Job
    @Bean
    public Job fraudDetectionJob(Step fraudDetectionStep) {
        return jobBuilderFactory.get("fraudDetectionJob")
                .incrementer(new RunIdIncrementer())
                .start(fraudDetectionStep)
                .build();
    }

    @Bean
    public Step fraudDetectionStep(ItemReader<User> fraudDetectionReader,
                                   ItemProcessor<User, FraudAlertDTO> fraudDetectionProcessor,
                                   ItemWriter<FraudAlertDTO> fraudDetectionWriter) {
        return stepBuilderFactory.get("fraudDetectionStep")
                .<User, FraudAlertDTO>chunk(10)
                .reader(fraudDetectionReader)
                .processor(fraudDetectionProcessor)
                .writer(fraudDetectionWriter)
                .build();
    }
}