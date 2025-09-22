package com.auctionflow.api.repositories;

import com.auctionflow.api.dtos.BidHistoryDTO;
import com.auctionflow.api.dtos.UserBidsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BidReadRepository {
    Page<BidHistoryDTO.BidDTO> findBidHistoryByAuctionId(String auctionId, Pageable pageable);
    Page<UserBidsDTO.UserBidDTO> findBidsByUserId(String userId, Pageable pageable);
}