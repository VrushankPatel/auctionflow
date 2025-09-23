package com.auctionflow.tests;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.dtos.BidResponse;
import com.auctionflow.api.dtos.CreateAuctionRequest;
import com.auctionflow.api.dtos.PlaceBidRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class BidConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testConcurrentBids() throws InterruptedException {
        // Create auction
        CreateAuctionRequest createRequest = new CreateAuctionRequest();
        createRequest.setItemId("test-item-4");
        createRequest.setCategoryId("test-category");
        createRequest.setAuctionType(com.auctionflow.core.domain.valueobjects.AuctionType.ENGLISH_OPEN);
        createRequest.setStartTime(Instant.now());
        createRequest.setEndTime(Instant.now().plusSeconds(60));
        createRequest.setReservePrice(BigDecimal.valueOf(100));

        ResponseEntity<AuctionDetailsDTO> createResponse = restTemplate.postForEntity("/api/v1/auctions", createRequest, AuctionDetailsDTO.class);
        AuctionDetailsDTO auction = createResponse.getBody();
        String auctionId = auction.getAuctionId();

        // Simulate concurrent bids
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger acceptedCount = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                PlaceBidRequest bidRequest = new PlaceBidRequest();
                bidRequest.setAmount(BigDecimal.valueOf(150)); // Same amount to test tie-breaking

                ResponseEntity<BidResponse> bidResponse = restTemplate.postForEntity("/api/v1/auctions/" + auctionId + "/bids", bidRequest, BidResponse.class);
                if (bidResponse.getBody() != null && bidResponse.getBody().isAccepted()) {
                    acceptedCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert only one bid is accepted
        assertThat(acceptedCount.get()).isEqualTo(1);

        // Check auction current highest bid
        ResponseEntity<AuctionDetailsDTO> auctionResponse = restTemplate.getForEntity("/api/v1/auctions/" + auctionId, AuctionDetailsDTO.class);
        assertThat(auctionResponse.getBody().getCurrentHighestBid()).isEqualTo(BigDecimal.valueOf(150));
    }
}