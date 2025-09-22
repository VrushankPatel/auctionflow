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
    public static final String SECURITY_EVENTS_TOPIC = "security-events";
    public static final String FAILED_EVENTS_TOPIC = "failed-events";

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final KafkaTemplate<String, SecurityEvent> securityKafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, DomainEvent> kafkaTemplate, KafkaTemplate<String, SecurityEvent> securityKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.securityKafkaTemplate = securityKafkaTemplate;
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

    public void publishSecurityEvent(SecurityEvent event) {
        String topic = SECURITY_EVENTS_TOPIC;
        String key = event.getEventId().toString();

        securityKafkaTemplate.send(topic, key, event).addCallback(new ListenableFutureCallback<SendResult<String, SecurityEvent>>() {
            @Override
            public void onSuccess(SendResult<String, SecurityEvent> result) {
                logger.info("Successfully published security event {} to topic {}", event.getEventId(), topic);
            }

            @Override
            public void onFailure(Throwable ex) {
                logger.error("Failed to publish security event {} to topic {}", event.getEventId(), topic, ex);
                sendSecurityToDLQ(event, ex);
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
        } else if (event instanceof DisputeCreatedEvent || event instanceof DisputeResolvedEvent) {
            return AUCTION_EVENTS_TOPIC; // Or create a new topic
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

    private void sendSecurityToDLQ(SecurityEvent event, Throwable ex) {
        try {
            securityKafkaTemplate.send(FAILED_EVENTS_TOPIC, event.getEventId().toString(), event);
            logger.info("Sent failed security event {} to DLQ", event.getEventId());
        } catch (Exception dlqEx) {
            logger.error("Failed to send security event {} to DLQ", event.getEventId(), dlqEx);
        }
    }

    /**
     * Sends a tombstone message (null value) to a compacted topic for cleanup.
     * @param topic the topic to send to
     * @param key the key to tombstone
     */
    public void sendTombstone(String topic, String key) {
        kafkaTemplate.send(topic, key, null).addCallback(new ListenableFutureCallback<SendResult<String, DomainEvent>>() {
            @Override
            public void onSuccess(SendResult<String, DomainEvent> result) {
                logger.info("Successfully sent tombstone for key {} to topic {}", key, topic);
            }

            @Override
            public void onFailure(Throwable ex) {
                logger.error("Failed to send tombstone for key {} to topic {}", key, topic, ex);
            }
        });
    }
}