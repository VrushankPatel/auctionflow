package com.auctionflow.notifications.service;

import com.auctionflow.notifications.NotificationService;
import com.auctionflow.notifications.entity.NotificationDelivery;
import com.auctionflow.notifications.repository.NotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationRetryService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRetryService.class);

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationService notificationService;

    public NotificationRetryService(NotificationDeliveryRepository deliveryRepository, NotificationService notificationService) {
        this.deliveryRepository = deliveryRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60000) // Retry every minute
    public void retryFailedDeliveries() {
        List<NotificationDelivery> failedDeliveries = deliveryRepository.findByStatus(NotificationDelivery.DeliveryStatus.FAILED);

        for (NotificationDelivery delivery : failedDeliveries) {
            if (delivery.getRetryCount() < 3) { // Max 3 retries
                boolean success = notificationService.retryDelivery(delivery);
                if (success) {
                    delivery.setStatus(NotificationDelivery.DeliveryStatus.SENT);
                    delivery.setSentAt(LocalDateTime.now());
                } else {
                    delivery.setRetryCount(delivery.getRetryCount() + 1);
                    delivery.setLastAttemptAt(LocalDateTime.now());
                }
                deliveryRepository.save(delivery);
            } else {
                logger.warn("Max retries reached for delivery {}", delivery.getId());
            }
        }
    }
}