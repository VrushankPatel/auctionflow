package com.auctionflow.api.queryhandlers;

import com.auctionflow.api.dtos.BidHistoryDTO;
import com.auctionflow.api.queries.GetBidHistoryQuery;
import com.auctionflow.api.repositories.BidReadRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class GetBidHistoryQueryHandler {

    private final BidReadRepository bidReadRepository;

    public GetBidHistoryQueryHandler(BidReadRepository bidReadRepository) {
        this.bidReadRepository = bidReadRepository;
    }

    @Cacheable(value = "bidHistory", key = "#query.auctionId + '_' + #query.page + '_' + #query.size")
    public BidHistoryDTO handle(GetBidHistoryQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        Page<BidHistoryDTO.BidDTO> page = bidReadRepository.findBidHistoryByAuctionId(query.getAuctionId(), pageable);

        BidHistoryDTO dto = new BidHistoryDTO();
        dto.setBids(page.getContent());
        dto.setPage(page.getNumber());
        dto.setSize(page.getSize());
        dto.setTotalElements(page.getTotalElements());
        dto.setTotalPages(page.getTotalPages());

        return dto;
    }
}