package com.auctionflow.tests;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.dtos.BidResponse;
import com.auctionflow.api.dtos.CreateAuctionRequest;
import com.auctionflow.api.dtos.PlaceBidRequest;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ChaosIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Container
    static ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0");

    static Proxy postgresProxy;

    @DynamicPropertySource
    static void configureChaosProperties(DynamicPropertyRegistry registry) throws Exception {
        // Create proxy for Postgres
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        postgresProxy = toxiproxyClient.createProxy("postgres", "0.0.0.0:8666", postgres.getHost() + ":" + postgres.getFirstMappedPort());

        // Override datasource to use proxy
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(8666) + "/auctionflow_test");
    }

    @BeforeEach
    void resetToxics() throws IOException {
        postgresProxy.toxics().getAll().forEach(toxic -> {
            try {
                toxic.remove();
            } catch (IOException e) {
                // ignore
            }
        });
    }

    @Test
    void testNetworkDelayResilience() throws IOException, InterruptedException {
        // Create auction
        CreateAuctionRequest createRequest = new CreateAuctionRequest();
        createRequest.setItemId("test-item-5");
        createRequest.setCategoryId("test-category");
        createRequest.setAuctionType(com.auctionflow.core.domain.valueobjects.AuctionType.ENGLISH_OPEN);
        createRequest.setStartTime(Instant.now().plusSeconds(1));
        createRequest.setEndTime(Instant.now().plusSeconds(300));
        createRequest.setReservePrice(BigDecimal.valueOf(100));

        ResponseEntity<AuctionDetailsDTO> createResponse = restTemplate.postForEntity("/api/v1/auctions", createRequest, AuctionDetailsDTO.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuctionDetailsDTO auction = createResponse.getBody();
        assertThat(auction).isNotNull();

        // Add 500ms latency to downstream
        Latency latency = postgresProxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 500);
        latency.setJitter(50);

        // Place bid during delay
        PlaceBidRequest bidRequest = new PlaceBidRequest();
        bidRequest.setAmount(BigDecimal.valueOf(110));

        ResponseEntity<BidResponse> bidResponse = restTemplate.postForEntity("/api/v1/auctions/" + auction.getAuctionId() + "/bids", bidRequest, BidResponse.class);
        assertThat(bidResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Remove latency
        latency.remove();

        // Verify bid was recorded
        ResponseEntity<AuctionDetailsDTO> auctionResponse = restTemplate.getForEntity("/api/v1/auctions/" + auction.getAuctionId(), AuctionDetailsDTO.class);
        assertThat(auctionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuctionDetailsDTO updatedAuction = auctionResponse.getBody();
        assertThat(updatedAuction.getCurrentHighestBid()).isEqualTo(BigDecimal.valueOf(110));
    }

    @Test
    void testDatabaseFailureResilience() throws Exception {
        // Create auction before failure
        CreateAuctionRequest createRequest = new CreateAuctionRequest();
        createRequest.setItemId("test-item-6");
        createRequest.setCategoryId("test-category");
        createRequest.setAuctionType(com.auctionflow.core.domain.valueobjects.AuctionType.ENGLISH_OPEN);
        createRequest.setStartTime(Instant.now().plusSeconds(1));
        createRequest.setEndTime(Instant.now().plusSeconds(300));
        createRequest.setReservePrice(BigDecimal.valueOf(100));

        ResponseEntity<AuctionDetailsDTO> createResponse = restTemplate.postForEntity("/api/v1/auctions", createRequest, AuctionDetailsDTO.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuctionDetailsDTO auction = createResponse.getBody();
        assertThat(auction).isNotNull();

        // Place initial bid
        PlaceBidRequest bidRequest = new PlaceBidRequest();
        bidRequest.setAmount(BigDecimal.valueOf(110));

        ResponseEntity<BidResponse> bidResponse = restTemplate.postForEntity("/api/v1/auctions/" + auction.getAuctionId() + "/bids", bidRequest, BidResponse.class);
        assertThat(bidResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Stop Postgres container
        postgres.stop();

        // Wait a bit
        Thread.sleep(2000);

        // Try to place bid during failure - should fail
        PlaceBidRequest bidRequest2 = new PlaceBidRequest();
        bidRequest2.setAmount(BigDecimal.valueOf(120));

        ResponseEntity<BidResponse> bidResponse2 = restTemplate.postForEntity("/api/v1/auctions/" + auction.getAuctionId() + "/bids", bidRequest2, BidResponse.class);
        // Assert it fails
        assertThat(bidResponse2.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR); // or whatever the error is

        // Restart Postgres
        postgres.start();

        // Wait for DB to be ready
        Thread.sleep(5000);

        // Place bid after recovery
        PlaceBidRequest bidRequest3 = new PlaceBidRequest();
        bidRequest3.setAmount(BigDecimal.valueOf(130));

        ResponseEntity<BidResponse> bidResponse3 = restTemplate.postForEntity("/api/v1/auctions/" + auction.getAuctionId() + "/bids", bidRequest3, BidResponse.class);
        assertThat(bidResponse3.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify data consistency - all bids should be there
        // Get bids
        // Assume there's an endpoint to get bids
        // For now, check current highest is 130
        ResponseEntity<AuctionDetailsDTO> auctionResponse = restTemplate.getForEntity("/api/v1/auctions/" + auction.getAuctionId(), AuctionDetailsDTO.class);
        assertThat(auctionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuctionDetailsDTO updatedAuction = auctionResponse.getBody();
        assertThat(updatedAuction.getCurrentHighestBid()).isEqualTo(BigDecimal.valueOf(130));
    }
}