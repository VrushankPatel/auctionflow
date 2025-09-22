package com.auctionflow.sdk.client;

import com.auctionflow.sdk.model.*;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface AuctionApiClient {

    @RequestLine("POST /api/v1/auth/login")
    @Headers("Content-Type: application/json")
    AuthResponse login(AuthRequest request);

    @RequestLine("POST /api/v1/auth/register")
    @Headers("Content-Type: application/json")
    RegisterResponse register(RegisterRequest request);

    @RequestLine("POST /api/v1/auctions")
    @Headers({"Content-Type: application/json", "Authorization: Bearer {token}"})
    void createAuction(CreateAuctionRequest request, @Param("token") String token);

    @RequestLine("GET /api/v1/auctions")
    @Headers("Authorization: Bearer {token}")
    List<AuctionSummary> listAuctions(@Param("category") String category,
                                      @Param("sellerId") String sellerId,
                                      @Param("page") int page,
                                      @Param("size") int size,
                                      @Param("token") String token);

    @RequestLine("GET /api/v1/auctions/{id}")
    @Headers("Authorization: Bearer {token}")
    AuctionDetails getAuction(@Param("id") String id, @Param("token") String token);

    @RequestLine("POST /api/v1/auctions/{id}/bids")
    @Headers({"Content-Type: application/json", "Authorization: Bearer {token}"})
    void placeBid(@Param("id") String id, PlaceBidRequest request, @Param("token") String token);

    @RequestLine("GET /api/v1/auctions/{id}/bids")
    @Headers("Authorization: Bearer {token}")
    BidHistory getBidHistory(@Param("id") String id,
                             @Param("page") int page,
                             @Param("size") int size,
                             @Param("token") String token);

    // Add more methods as needed
}