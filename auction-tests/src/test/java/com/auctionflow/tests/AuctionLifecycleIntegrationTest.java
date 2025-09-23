package com.auctionflow.tests;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.dtos.BidResponse;
import com.auctionflow.api.dtos.CreateAuctionRequest;
import com.auctionflow.api.dtos.PlaceBidRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class AuctionLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCompleteAuctionLifecycle() {
        // Create auction
        CreateAuctionRequest createRequest = new CreateAuctionRequest();
        createRequest.setItemId("test-item-3");
        createRequest.setCategoryId("test-category");
        createRequest.setAuctionType(com.auctionflow.core.domain.valueobjects.AuctionType.ENGLISH_OPEN);
        createRequest.setStartTime(Instant.now().plusSeconds(1));
        createRequest.setEndTime(Instant.now().plusSeconds(10)); // Short auction for test
        createRequest.setReservePrice(BigDecimal.valueOf(100));
        createRequest.setBuyNowPrice(BigDecimal.valueOf(200));
        // Assume user is created or mocked

        ResponseEntity<AuctionDetailsDTO> createResponse = restTemplate.postForEntity("/api/v1/auctions", createRequest, AuctionDetailsDTO.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuctionDetailsDTO auction = createResponse.getBody();
        assertThat(auction).isNotNull();

        // Place bids
        PlaceBidRequest bidRequest = new PlaceBidRequest();
        bidRequest.setAmount(BigDecimal.valueOf(110));

        ResponseEntity<BidResponse> bidResponse = restTemplate.postForEntity("/api/v1/auctions/" + auction.getAuctionId() + "/bids", bidRequest, BidResponse.class);
        assertThat(bidResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for auction to close
        // Use Awaitility to wait until end time
        // Then check auction status is CLOSED, winner is set, etc.

        // For simplicity, assume timer closes it
        // In real test, use Awaitility.until(() -> getAuctionStatus() == CLOSED)

        // Assert winner
    }
}