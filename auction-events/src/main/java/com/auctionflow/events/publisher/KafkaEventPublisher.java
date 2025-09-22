package com.auctionflow.events.publisher;

import com.auctionflow.core.domain.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    public static final String AUCTION_EVENTS_TOPIC = "auction-events";
    public static final String BID_EVENTS_TOPIC = "bid-events";
    public static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";
    public static final String FAILED_EVENTS_TOPIC = "failed-events";

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, DomainEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DomainEvent event) {
        String topic = determineTopic(event);
        String key = event.getEventId().toString();

        kafkaTemplate.send(topic, key, event).addCallback(new ListenableFutureCallback<SendResult<String, DomainEvent>>() {
            @Override
            public void onSuccess(SendResult<String, DomainEvent> result) {
                logger.info("Successfully published event {} to topic {}", event.getEventId(), topic);
            }

            @Override
            public void onFailure(Throwable ex) {
                logger.error("Failed to publish event {} to topic {}", event.getEventId(), topic, ex);
                sendToDLQ(event, ex);
            }
        });
    }

    private String determineTopic(DomainEvent event) {
        if (event instanceof AuctionCreatedEvent || event instanceof AuctionClosedEvent || event instanceof AuctionExtendedEvent) {
            return AUCTION_EVENTS_TOPIC;
        } else if (event instanceof BidPlacedEvent || event instanceof BidRejectedEvent) {
            return BID_EVENTS_TOPIC;
        } else if (event instanceof WinnerDeclaredEvent) {
            return NOTIFICATION_EVENTS_TOPIC;
        } else {
            logger.warn("Unknown event type {}, defaulting to auction-events", event.getClass().getSimpleName());
            return AUCTION_EVENTS_TOPIC;
        }
    }

    private void sendToDLQ(DomainEvent event, Throwable ex) {
        try {
            kafkaTemplate.send(FAILED_EVENTS_TOPIC, event.getEventId().toString(), event);
            logger.info("Sent failed event {} to DLQ", event.getEventId());
        } catch (Exception dlqEx) {
            logger.error("Failed to send event {} to DLQ", event.getEventId(), dlqEx);
        }
    }
}