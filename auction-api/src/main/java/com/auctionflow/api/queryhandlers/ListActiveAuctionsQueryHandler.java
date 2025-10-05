package com.auctionflow.api.queryhandlers;

import com.auctionflow.api.dtos.ActiveAuctionsDTO;
import com.auctionflow.api.dtos.AuctionSummaryDTO;
import com.auctionflow.api.queries.ListActiveAuctionsQuery;
import com.auctionflow.api.repositories.AuctionReadRepository;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ListActiveAuctionsQueryHandler {

    private final AuctionReadRepository auctionReadRepository;

    public ListActiveAuctionsQueryHandler(AuctionReadRepository auctionReadRepository) {
        this.auctionReadRepository = auctionReadRepository;
    }

    public ActiveAuctionsDTO handle(ListActiveAuctionsQuery query) {
        List<Object[]> results = auctionReadRepository.findActiveAuctionsNative(
                query.getCategory().orElse(null),
                query.getSellerId().orElse(null),
                query.getQuery().orElse(null)
        );

        List<AuctionSummaryDTO> list = results.stream()
                .map(row -> {
                    AuctionSummaryDTO dto = new AuctionSummaryDTO();
                    dto.setId((String) row[0]);
                    dto.setItemId((String) row[1]);
                    dto.setSellerId(row[2] != null ? ((Number) row[2]).longValue() : null);
                    dto.setTitle((String) row[3]);
                    dto.setDescription((String) row[4]);
                    dto.setCategory((String) row[5]);
                    dto.setCondition((String) row[6]);
                    
                    // Parse images JSON array
                    String imagesJson = (String) row[7];
                    if (imagesJson != null && !imagesJson.isEmpty()) {
                        try {
                            // Simple JSON array parsing for PostgreSQL array format
                            imagesJson = imagesJson.replace("{", "[").replace("}", "]");
                            dto.setImages(java.util.Arrays.asList(
                                imagesJson.substring(1, imagesJson.length() - 1).split(",")
                            ));
                        } catch (Exception e) {
                            dto.setImages(java.util.Collections.emptyList());
                        }
                    } else {
                        dto.setImages(java.util.Collections.emptyList());
                    }
                    
                    dto.setAuctionType((String) row[8]);
                    dto.setStartingPrice((java.math.BigDecimal) row[9]);
                    dto.setReservePrice((java.math.BigDecimal) row[10]);
                    dto.setBuyNowPrice((java.math.BigDecimal) row[11]);
                    dto.setCurrentHighestBid((java.math.BigDecimal) row[12]);
                    dto.setBidCount(row[13] != null ? ((Number) row[13]).intValue() : 0);
                    dto.setHiddenReserve((Boolean) row[14]);
                    dto.setStartTime((java.time.Instant) row[15]);
                    dto.setEndTime((java.time.Instant) row[16]);
                    dto.setStatus((String) row[17]);
                    
                    return dto;
                })
                .collect(Collectors.toList());

        ActiveAuctionsDTO dto = new ActiveAuctionsDTO();
        dto.setAuctions(list);
        dto.setPage(0);
        dto.setSize(list.size());
        dto.setTotalElements(list.size());
        dto.setTotalPages(1);

        return dto;
    }
}