package com.auctionflow.api.queryhandlers;

import com.auctionflow.api.dtos.UserBidsDTO;
import com.auctionflow.api.queries.GetUserBidsQuery;
import com.auctionflow.api.repositories.BidReadRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class GetUserBidsQueryHandler {

    private final BidReadRepository bidReadRepository;

    public GetUserBidsQueryHandler(BidReadRepository bidReadRepository) {
        this.bidReadRepository = bidReadRepository;
    }

    public UserBidsDTO handle(GetUserBidsQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        Page<UserBidsDTO.UserBidDTO> page = bidReadRepository.findBidsByUserId(query.getUserId(), pageable);

        UserBidsDTO dto = new UserBidsDTO();
        dto.setBids(page.getContent());
        dto.setPage(page.getNumber());
        dto.setSize(page.getSize());
        dto.setTotalElements(page.getTotalElements());
        dto.setTotalPages(page.getTotalPages());

        return dto;
    }
}