package com.auctionflow.notifications;

import com.auctionflow.core.domain.events.*;
import com.auctionflow.core.domain.events.ProxyBidOutbidEvent;
import com.auctionflow.core.domain.events.ReserveMetEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.notifications.entity.BatchedNotification;
import com.auctionflow.notifications.entity.NotificationDelivery;
import com.auctionflow.notifications.repository.BatchedNotificationRepository;
import com.auctionflow.notifications.repository.NotificationDeliveryRepository;
import com.auctionflow.notifications.service.EmailService;
import com.auctionflow.notifications.service.PushNotificationService;
import io.opentelemetry.extension.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final PushNotificationService pushNotificationService;
    private final EmailService emailService;
    private final NotificationDeliveryRepository deliveryRepository;
    private final BatchedNotificationRepository batchedRepository;
    private final RestTemplate restTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate, StringRedisTemplate redisTemplate,
                               PushNotificationService pushNotificationService, EmailService emailService,
                               NotificationDeliveryRepository deliveryRepository,
                               BatchedNotificationRepository batchedRepository, RestTemplate restTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        this.pushNotificationService = pushNotificationService;
        this.emailService = emailService;
        this.deliveryRepository = deliveryRepository;
        this.batchedRepository = batchedRepository;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "auction-events", groupId = "notification-service", containerFactory = "kafkaListenerContainerFactory")
    @WithSpan("handle-auction-events-batch")
    public void handleAuctionEventsBatch(List<DomainEvent> events, Acknowledgment acknowledgment) {
        for (DomainEvent event : events) {
            if (event instanceof AuctionExtendedEvent) {
                handleAuctionExtended((AuctionExtendedEvent) event);
            } else if (event instanceof AuctionClosedEvent) {
                handleAuctionClosed((AuctionClosedEvent) event);
            }
        }
        acknowledgment.acknowledge(); // Batch commit
    }

    @KafkaListener(topics = "bid-events", groupId = "notification-service", containerFactory = "kafkaListenerContainerFactory")
    @WithSpan("handle-bid-events-batch")
    public void handleBidEventsBatch(List<DomainEvent> events, Acknowledgment acknowledgment) {
        for (DomainEvent event : events) {
            if (event instanceof BidPlacedEvent) {
                handleBidPlaced((BidPlacedEvent) event);
            } else if (event instanceof BidRejectedEvent) {
                handleBidRejected((BidRejectedEvent) event);
            } else if (event instanceof ProxyBidOutbidEvent) {
                handleProxyBidOutbid((ProxyBidOutbidEvent) event);
            } else if (event instanceof ReserveMetEvent) {
                handleReserveMet((ReserveMetEvent) event);
            }
        }
        acknowledgment.acknowledge(); // Batch commit
    }

    @KafkaListener(topics = "notification-events", groupId = "notification-service", containerFactory = "kafkaListenerContainerFactory")
    @WithSpan("handle-notification-events-batch")
    public void handleNotificationEventsBatch(List<DomainEvent> events, Acknowledgment acknowledgment) {
        for (DomainEvent event : events) {
            if (event instanceof WinnerDeclaredEvent) {
                handleWinnerDeclared((WinnerDeclaredEvent) event);
            }
        }
        acknowledgment.acknowledge(); // Batch commit
    }

    private void handleBidPlaced(BidPlacedEvent event) {
        String auctionId = ((AuctionId) event.getAggregateId()).value().toString();
        String bidderId = event.getBidderId().toString();

        // Notify bidder that bid was accepted
        sendNotification(bidderId, "BidAccepted", "Your bid of " + event.getAmount() + " has been accepted for auction " + auctionId);

        // Notify previous bidder that they were outbid
        String previousBidderKey = "currentBidder:auction:" + auctionId;
        String previousBidder = redisTemplate.opsForValue().get(previousBidderKey);
        if (previousBidder != null && !previousBidder.equals(bidderId)) {
            sendNotification(previousBidder, "BidOutbid", "You have been outbid on auction " + auctionId);
        }

        // Update current bidder
        redisTemplate.opsForValue().set(previousBidderKey, bidderId);

        // Notify watchers
        notifyWatchers(auctionId, "BidPlaced", "A new bid of " + event.getAmount() + " has been placed on auction " + auctionId);
    }

    private void handleBidRejected(BidRejectedEvent event) {
        String bidderId = event.getBidderId().toString();
        String auctionId = ((AuctionId) event.getAggregateId()).value().toString();
        sendNotification(bidderId, "BidRejected", "Your bid has been rejected for auction " + auctionId);
    }

    private void handleProxyBidOutbid(ProxyBidOutbidEvent event) {
        String userId = event.getUserId().toString();
        String auctionId = ((AuctionId) event.getAggregateId()).value().toString();
        sendNotification(userId, "ProxyBidOutbid", "Your proxy bid has been outbid on auction " + auctionId + ". " + event.getReason());
    }

    private void handleReserveMet(ReserveMetEvent event) {
        String auctionId = ((AuctionId) event.getAggregateId()).value().toString();
        String bidderId = event.getBidderId().toString();
        // Notify seller that reserve has been met
        // Assuming sellerId is stored in Redis or fetched
        String sellerId = redisTemplate.opsForValue().get("seller:auction:" + auctionId);
        if (sellerId != null) {
            sendNotification(sellerId, "ReserveMet", "Reserve price has been met on auction " + auctionId + " by bidder " + bidderId);
        }
        // Optionally notify watchers
        notifyWatchers(auctionId, "ReserveMet", "Reserve price has been met on auction " + auctionId);
    }

    private void handleAuctionExtended(AuctionExtendedEvent event) {
        String auctionId = ((AuctionId) event.getAggregateId()).value().toString();
        notifyWatchers(auctionId, "AuctionExtended", "Auction " + auctionId + " has been extended");
    }

    private void handleAuctionClosed(AuctionClosedEvent event) {
        String auctionId = ((AuctionId) event.getAggregateId()).value().toString();
        notifyWatchers(auctionId, "AuctionClosed", "Auction " + auctionId + " has closed");
    }

    private void handleWinnerDeclared(WinnerDeclaredEvent event) {
        String auctionId = ((AuctionId) event.getAggregateId()).value().toString();
        if (event.getWinnerId() != null) {
            String winnerId = event.getWinnerId().value().toString();
            sendNotification(winnerId, "AuctionWon", "Congratulations! You won auction " + auctionId);
        }
        notifyWatchers(auctionId, "AuctionWon", "Auction " + auctionId + " has a winner");
    }

    private boolean sendPushNotification(String userId, String notificationType, String title, String message) {
        return pushNotificationService.sendPushNotification(userId, notificationType, title, message);
    }

    private boolean sendEmailNotification(String userId, String notificationType, String message) {
        String email = redisTemplate.opsForValue().get("email:" + userId);
        if (email != null) {
            return emailService.sendEmail(userId, email, "Auction Notification: " + notificationType, message, notificationType);
        }
        return false;
    }

    private void sendNotification(String userId, String type, String message) {
        // Send via WebSocket (no preference check for WebSocket, as it's real-time)
        boolean websocketSent = sendWebSocketNotification(userId, type, message);

        // For non-urgent notifications, batch them
        if ("BidOutbid".equals(type)) {
            batchNotification(userId, type, message);
            return;
        }

        // Try push notification (preferences checked inside)
        boolean pushSent = pushNotificationService.sendPushNotification(userId, type, type, message);

        // For critical events, send email (preferences checked inside)
        boolean emailSent = false;
        if ("AuctionWon".equals(type) || "BidAccepted".equals(type)) {
            String email = redisTemplate.opsForValue().get("email:" + userId);
            if (email != null) {
                emailSent = emailService.sendEmail(userId, email, "Auction Notification: " + type, message, type);
            }
        }

        // Track delivery
        trackDelivery(userId, type, "websocket", websocketSent, message);
        if (pushSent) trackDelivery(userId, type, "push", true, message);
        if (emailSent) trackDelivery(userId, type, "email", true, message);
    }

    private boolean sendWebSocketNotification(String userId, String type, String message) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", new Notification(type, message));
            return true;
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification to {}", userId, e);
            return false;
        }
    }

    private boolean sendWebSocketNotificationToQueue(String userId, String queue, String type, String message) {
        try {
            messagingTemplate.convertAndSendToUser(userId, queue, new Notification(type, message));
            return true;
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification to {} on queue {}", userId, queue, e);
            return false;
        }
    }





    private void trackDelivery(String userId, String type, String channel, boolean success, String message) {
        NotificationDelivery delivery = new NotificationDelivery(userId, type, channel, message);
        if (success) {
            delivery.setStatus(NotificationDelivery.DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());
        } else {
            delivery.setStatus(NotificationDelivery.DeliveryStatus.FAILED);
            delivery.setLastAttemptAt(LocalDateTime.now());
        }
        deliveryRepository.save(delivery);
    }

    public boolean retryDelivery(NotificationDelivery delivery) {
        boolean success = false;
        switch (delivery.getChannel()) {
            case "websocket":
                success = sendWebSocketNotification(delivery.getUserId(), delivery.getNotificationType(), delivery.getMessage());
                break;
            case "push":
                success = sendPushNotification(delivery.getUserId(), delivery.getNotificationType(), delivery.getNotificationType(), delivery.getMessage());
                break;
            case "email":
                success = sendEmailNotification(delivery.getUserId(), delivery.getNotificationType(), delivery.getMessage());
                break;
        }
        return success;
    }

    private void batchNotification(String userId, String type, String message) {
        BatchedNotification batched = new BatchedNotification(userId, type, message);
        batchedRepository.save(batched);
        logger.info("Batched notification for user {} type {}", userId, type);
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void sendBatchedNotifications() {
        // Group by user and send summary emails
        // For simplicity, send each batched notification as email
        List<BatchedNotification> unsent = batchedRepository.findBySent(false);
        for (BatchedNotification bn : unsent) {
            String email = redisTemplate.opsForValue().get("email:" + bn.getUserId());
            if (email != null) {
                boolean sent = emailService.sendEmail(bn.getUserId(), email, "Auction Notification: " + bn.getNotificationType(), bn.getMessage(), bn.getNotificationType());
                if (sent) {
                    bn.setSent(true);
                    batchedRepository.save(bn);
                }
            }
        }
    }

    @Scheduled(fixedRate = 86400000) // Daily
    public void sendPersonalizedRecommendations() {
        // Assume users who have bid recently
        // For simplicity, get all users from Redis or DB, but since no user repo here, assume from Redis
        // This is placeholder; in real, query users who have preferences for recommendations
        // For now, skip implementation as it requires more setup
        logger.info("Sending personalized recommendations - placeholder");
    }

    private void notifyWatchers(String auctionId, String type, String message) {
        String key = "watchers:auction:" + auctionId;
        Set<String> watchers = redisTemplate.opsForSet().members(key);
        if (watchers != null) {
            for (String watcher : watchers) {
                // Send to user's auction queue
                sendWebSocketNotificationToQueue(watcher, "/queue/auctions/" + auctionId, type, message);
                // Also send push/email if needed, but for auction updates, perhaps only WebSocket
            }
        }
    }

    public static class Notification {
        private String type;
        private String message;

        public Notification(String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}