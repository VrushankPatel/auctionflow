package com.auctionflow.payments;

import com.auctionflow.core.domain.events.AuctionClosedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuctionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionEventListener.class);

    private final InvoiceService invoiceService;

    public AuctionEventListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @KafkaListener(topics = "auction-events", groupId = "payment-service")
    @WithSpan("handle-auction-closed-event")
    public void handleAuctionEvent(DomainEvent event) {
        if (event instanceof AuctionClosedEvent) {
            AuctionClosedEvent closedEvent = (AuctionClosedEvent) event;
            logger.info("Received AuctionClosedEvent for auction: {}", closedEvent.getAggregateId());

            // For demo, assume winning bid is 100.00, seller is "seller1", buyer is winner
            // In real, need to query auction details
            String auctionId = closedEvent.getAggregateId().getValue();
            String sellerId = "seller1"; // hardcoded
            String buyerId = closedEvent.getWinnerId() != null ? closedEvent.getWinnerId().getValue() : null;
            BigDecimal winningBid = BigDecimal.valueOf(100.00); // hardcoded

            try {
                Invoice invoice = invoiceService.generateInvoice(auctionId, sellerId, buyerId, winningBid);
                logger.info("Generated invoice {} for auction {}", invoice.getId(), auctionId);
            } catch (Exception e) {
                logger.error("Failed to generate invoice for auction {}", auctionId, e);
            }
        }
    }
}