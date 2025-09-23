package com.auctionflow.api.queryhandlers;

import com.auctionflow.api.dtos.ActiveAuctionsDTO;
import com.auctionflow.api.queries.ListActiveAuctionsQuery;
import com.auctionflow.api.repositories.AuctionReadRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ListActiveAuctionsQueryHandler {

    private final AuctionReadRepository auctionReadRepository;

    public ListActiveAuctionsQueryHandler(AuctionReadRepository auctionReadRepository) {
        this.auctionReadRepository = auctionReadRepository;
    }

    @Cacheable(value = "activeAuctions", key = "#query.category + '_' + #query.sellerId + '_' + #query.query + '_' + #query.page + '_' + #query.size")
    public ActiveAuctionsDTO handle(ListActiveAuctionsQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        Page<ActiveAuctionsDTO.AuctionSummaryDTO> page = auctionReadRepository.findActiveAuctions(
                query.getCategory().orElse(null),
                query.getSellerId().orElse(null),
                query.getQuery().orElse(null),
                pageable
        );

        ActiveAuctionsDTO dto = new ActiveAuctionsDTO();
        dto.setAuctions(page.getContent());
        dto.setPage(page.getNumber());
        dto.setSize(page.getSize());
        dto.setTotalElements(page.getTotalElements());
        dto.setTotalPages(page.getTotalPages());

        return dto;
    }
}