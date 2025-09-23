package com.auctionflow.payments;

import com.auctionflow.core.domain.events.AuctionClosedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import io.opentelemetry.extension.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AuctionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionEventListener.class);

    private final InvoiceService invoiceService;

    public AuctionEventListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @KafkaListener(topics = "auction-events", groupId = "payment-service", containerFactory = "kafkaListenerContainerFactory")
    @WithSpan("handle-auction-events-batch")
    public void handleAuctionEventsBatch(List<DomainEvent> events, Acknowledgment acknowledgment) {
        for (DomainEvent event : events) {
            if (event instanceof AuctionClosedEvent) {
                AuctionClosedEvent closedEvent = (AuctionClosedEvent) event;
                logger.info("Received AuctionClosedEvent for auction: {}", closedEvent.getAggregateId());

                // For demo, assume winning bid is 100.00, seller is "seller1", buyer is winner
                // In real, need to query auction details
                String auctionId = ((com.auctionflow.core.domain.valueobjects.AuctionId) closedEvent.getAggregateId()).value().toString();
                String sellerId = "seller1"; // hardcoded
                String buyerId = closedEvent.getWinnerId() != null ? closedEvent.getWinnerId().value().toString() : null;
                BigDecimal winningBid = BigDecimal.valueOf(100.00); // hardcoded

                try {
                    Invoice invoice = invoiceService.generateInvoice(auctionId, sellerId, buyerId, winningBid, "US", "electronics");
                    logger.info("Generated invoice {} for auction {}", invoice.getId(), auctionId);
                } catch (Exception e) {
                    logger.error("Failed to generate invoice for auction {}", auctionId, e);
                }
            }
        }
        acknowledgment.acknowledge(); // Batch commit
    }
}