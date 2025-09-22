package com.auctionflow.api.services;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.repositories.AuctionReadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuctionSecurityService {

    @Autowired
    private AuctionReadRepository auctionReadRepository;

    public boolean canBid(String auctionId, UUID userId) {
        Optional<AuctionDetailsDTO> auctionOpt = auctionReadRepository.findAuctionDetailsById(auctionId);
        if (auctionOpt.isEmpty()) {
            return false;
        }
        AuctionDetailsDTO auction = auctionOpt.get();
        if (!"OPEN".equals(auction.getStatus())) {
            return false;
        }
        Instant now = Instant.now();
        if (now.isBefore(auction.getStartTs()) || now.isAfter(auction.getEndTs())) {
            return false;
        }
        // Check if user is not the seller
        return !userId.toString().equals(auction.getSellerId());
    }

    public boolean canEdit(String auctionId, UUID userId) {
        Optional<AuctionDetailsDTO> auctionOpt = auctionReadRepository.findAuctionDetailsById(auctionId);
        if (auctionOpt.isEmpty()) {
            return false;
        }
        AuctionDetailsDTO auction = auctionOpt.get();
        // Can edit if seller
        return userId.toString().equals(auction.getSellerId());
    }

    public boolean isSeller(String auctionId, UUID userId) {
        Optional<AuctionDetailsDTO> auctionOpt = auctionReadRepository.findAuctionDetailsById(auctionId);
        return auctionOpt.map(auction -> userId.toString().equals(auction.getSellerId())).orElse(false);
    }
}