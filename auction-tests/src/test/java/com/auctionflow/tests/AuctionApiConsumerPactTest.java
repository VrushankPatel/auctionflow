package com.auctionflow.tests;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.PactDslJsonBody.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "auction-api", port = "8080")
public class AuctionApiConsumerPactTest {

    @Pact(provider = "auction-api", consumer = "auction-analytics")
    public RequestResponsePact getAuctionDetails(PactDslWithProvider builder) {
        return builder
            .given("an auction exists")
            .uponReceiving("a request to get auction details")
            .path("/auctions/123")
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(object()
                .stringType("id", "123")
                .stringType("itemId", "item-123")
                .stringType("status", "ACTIVE")
                .object("currentHighestBid")
                    .numberType("amount", 150.0)
                    .stringType("bidderId", "bidder-456")
                .closeObject()
                .stringType("endTime", "2023-10-01T12:00:00Z")
            )
            .toPact();
    }

    @Pact(provider = "auction-api", consumer = "auction-analytics")
    public RequestResponsePact placeBid(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token");

        return builder
            .given("an active auction exists")
            .uponReceiving("a request to place a bid")
            .path("/auctions/123/bids")
            .method("POST")
            .headers(headers)
            .body(object()
                .numberType("amount", 200.0)
                .stringType("idempotencyKey", "key-123")
            )
            .willRespondWith()
            .status(200)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getAuctionDetails")
    void testGetAuctionDetails(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        var response = restTemplate.getForEntity(mockServer.getUrl() + "/auctions/123", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Additional assertions can be made on the response body
    }

    @Test
    @PactTestFor(pactMethod = "placeBid")
    void testPlaceBid(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> bidRequest = Map.of("amount", 200.0, "idempotencyKey", "key-123");
        var response = restTemplate.postForEntity(mockServer.getUrl() + "/auctions/123/bids", bidRequest, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}