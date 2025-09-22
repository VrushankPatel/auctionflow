package com.auctionflow.sdk;

import com.auctionflow.sdk.client.AuctionApiClient;
import com.auctionflow.sdk.model.*;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AuctionFlowClient {
    private final AuctionApiClient apiClient;
    private String accessToken;
    private String refreshToken;

    public AuctionFlowClient(String baseUrl) {
        this.apiClient = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(AuctionApiClient.class))
                .logLevel(Logger.Level.FULL)
                .retryer(new feign.Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 5)) // retry logic
                .target(AuctionApiClient.class, baseUrl);
    }

    public AuthResponse login(String username, String password) {
        AuthRequest request = new AuthRequest(username, password);
        AuthResponse response = apiClient.login(request);
        this.accessToken = response.getAccessToken();
        this.refreshToken = response.getRefreshToken();
        return response;
    }

    public RegisterResponse register(String email, String displayName, String password, String role) {
        RegisterRequest request = new RegisterRequest(email, displayName, password, role);
        return apiClient.register(request);
    }

    public void createAuction(CreateAuctionRequest request) {
        ensureAuthenticated();
        apiClient.createAuction(request, accessToken);
    }

    public List<AuctionSummary> listAuctions(String category, String sellerId, int page, int size) {
        ensureAuthenticated();
        return apiClient.listAuctions(category, sellerId, page, size, accessToken);
    }

    public AuctionDetails getAuction(String id) {
        ensureAuthenticated();
        return apiClient.getAuction(id, accessToken);
    }

    public void placeBid(String auctionId, PlaceBidRequest request) {
        ensureAuthenticated();
        apiClient.placeBid(auctionId, request, accessToken);
    }

    public BidHistory getBidHistory(String auctionId, int page, int size) {
        ensureAuthenticated();
        return apiClient.getBidHistory(auctionId, page, size, accessToken);
    }

    private void ensureAuthenticated() {
        if (accessToken == null) {
            throw new IllegalStateException("Not authenticated. Please login first.");
        }
        // TODO: check if token is expired and refresh if needed
    }

    // TODO: implement refresh token logic
}