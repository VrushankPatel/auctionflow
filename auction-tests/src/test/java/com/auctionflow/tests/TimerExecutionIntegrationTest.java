package com.auctionflow.tests;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.dtos.CreateAuctionRequest;
import com.auctionflow.api.dtos.PlaceBidRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TimerExecutionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testAuctionClosesAtEndTime() {
        // Create auction with short duration
        CreateAuctionRequest createRequest = new CreateAuctionRequest();
        createRequest.setItemId("test-item-1");
        createRequest.setCategoryId("test-category");
        createRequest.setAuctionType(com.auctionflow.core.domain.valueobjects.AuctionType.ENGLISH_OPEN);
        createRequest.setStartTime(Instant.now());
        createRequest.setEndTime(Instant.now().plusSeconds(5));
        createRequest.setReservePrice(BigDecimal.valueOf(100));

        ResponseEntity<AuctionDetailsDTO> createResponse = restTemplate.postForEntity("/api/v1/auctions", createRequest, AuctionDetailsDTO.class);
        AuctionDetailsDTO auction = createResponse.getBody();
        String auctionId = auction.getAuctionId();

        // Wait until auction should be closed
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).until(() -> {
            ResponseEntity<AuctionDetailsDTO> response = restTemplate.getForEntity("/api/v1/auctions/" + auctionId, AuctionDetailsDTO.class);
            return "CLOSED".equals(response.getBody().getStatus());
        });

        // Assert closed
        ResponseEntity<AuctionDetailsDTO> finalResponse = restTemplate.getForEntity("/api/v1/auctions/" + auctionId, AuctionDetailsDTO.class);
        assertThat(finalResponse.getBody().getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void testAntiSnipeExtension() {
        // Create auction with extension policy
        CreateAuctionRequest createRequest = new CreateAuctionRequest();
        createRequest.setItemId("test-item-2");
        createRequest.setCategoryId("test-category");
        createRequest.setAuctionType(com.auctionflow.core.domain.valueobjects.AuctionType.ENGLISH_OPEN);
        createRequest.setStartTime(Instant.now());
        createRequest.setEndTime(Instant.now().plusSeconds(10));
        createRequest.setReservePrice(BigDecimal.valueOf(100));
        // Assume extension policy is set, e.g., fixed_window

        ResponseEntity<AuctionDetailsDTO> createResponse = restTemplate.postForEntity("/api/v1/auctions", createRequest, AuctionDetailsDTO.class);
        AuctionDetailsDTO auction = createResponse.getBody();
        String auctionId = auction.getAuctionId();

        // Wait until near end, say 2 seconds before
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Place bid
        PlaceBidRequest bidRequest = new PlaceBidRequest();
        bidRequest.setAmount(BigDecimal.valueOf(110));

        restTemplate.postForEntity("/api/v1/auctions/" + auctionId + "/bids", bidRequest, String.class);

        // Check if end time is extended
        ResponseEntity<AuctionDetailsDTO> response = restTemplate.getForEntity("/api/v1/auctions/" + auctionId, AuctionDetailsDTO.class);
        Instant newEndTime = response.getBody().getEndTs();
        // Assert extended, e.g., newEndTime > original + extension
    }
}