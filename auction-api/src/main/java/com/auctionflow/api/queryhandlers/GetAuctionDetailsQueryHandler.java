package com.auctionflow.api.queryhandlers;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.queries.GetAuctionDetailsQuery;
import com.auctionflow.api.repositories.AuctionReadRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GetAuctionDetailsQueryHandler {

    private final AuctionReadRepository auctionReadRepository;

    public GetAuctionDetailsQueryHandler(AuctionReadRepository auctionReadRepository) {
        this.auctionReadRepository = auctionReadRepository;
    }

    @Cacheable(value = "auctionDetails", key = "#query.auctionId")
    public Optional<AuctionDetailsDTO> handle(GetAuctionDetailsQuery query) {
        Optional<AuctionDetailsDTO> dtoOpt = auctionReadRepository.findAuctionDetailsById(query.getAuctionId());
        dtoOpt.ifPresent(dto -> {
            if (dto.isHiddenReserve()) {
                dto.setReservePrice(null);
            }
        });
        return dtoOpt;
    }
}