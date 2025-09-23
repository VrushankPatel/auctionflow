package com.auctionflow.api.queryhandlers;

import com.auctionflow.api.dtos.OfferResponse;
import com.auctionflow.api.entities.Offer;
import com.auctionflow.api.queries.GetOffersQuery;
import com.auctionflow.api.repositories.OfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetOffersQueryHandler {

    @Autowired
    private OfferRepository offerRepository;

    public List<OfferResponse> handle(GetOffersQuery query) {
        List<Offer> offers = offerRepository.findByAuctionId(query.getAuctionId());
        return offers.stream()
                .map(offer -> new OfferResponse(
                        offer.getId().toString(),
                        offer.getAuctionId(),
                        offer.getBuyerId(),
                        offer.getSellerId(),
                        offer.getAmount(),
                        offer.getStatus(),
                        offer.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}