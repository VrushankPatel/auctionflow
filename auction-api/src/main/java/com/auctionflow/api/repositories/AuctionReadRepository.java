package com.auctionflow.api.repositories;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.dtos.ActiveAuctionsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AuctionReadRepository {
    Optional<AuctionDetailsDTO> findAuctionDetailsById(String auctionId);
    Page<ActiveAuctionsDTO.AuctionSummaryDTO> findActiveAuctions(String category, String sellerId, String query, Pageable pageable);
}