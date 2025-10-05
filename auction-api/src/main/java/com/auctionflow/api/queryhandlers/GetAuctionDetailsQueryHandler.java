package com.auctionflow.api.queryhandlers;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.queries.GetAuctionDetailsQuery;
import com.auctionflow.api.repositories.AuctionReadRepository;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GetAuctionDetailsQueryHandler {

    private final AuctionReadRepository auctionReadRepository;

    public GetAuctionDetailsQueryHandler(AuctionReadRepository auctionReadRepository) {
        this.auctionReadRepository = auctionReadRepository;
    }

    public Optional<AuctionDetailsDTO> handle(GetAuctionDetailsQuery query) {
        var results = auctionReadRepository.findAuctionDetailsByIdNative(query.getAuctionId());
        if (results.isEmpty()) {
            return Optional.empty();
        }
        var row = results.get(0);
        AuctionDetailsDTO dto = new AuctionDetailsDTO(
            (String) row[0], // auctionId
            (String) row[1], // itemId
            ((Number) row[2]).longValue(), // sellerId
            (String) row[3], // title
            (String) row[4], // description
            (String) row[5], // status
            (java.time.Instant) row[6], // startTs
            (java.time.Instant) row[7], // endTs
            null, // reservePrice
            (java.math.BigDecimal) row[9], // buyNowPrice
            (Boolean) row[10], // hiddenReserve
            (java.math.BigDecimal) row[11], // currentHighestBid
            row[12] != null ? row[12].toString() : null, // highestBidderId
            (java.time.Instant) row[13] // lastBidTs
        );
        if (dto.isHiddenReserve()) {
            dto.setReservePrice(null);
        }
        return Optional.of(dto);
    }
}