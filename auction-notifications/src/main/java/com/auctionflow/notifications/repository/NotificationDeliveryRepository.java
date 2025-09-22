package com.auctionflow.notifications.repository;

import com.auctionflow.notifications.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByStatus(NotificationDelivery.DeliveryStatus status);

    List<NotificationDelivery> findByUserIdAndStatus(String userId, NotificationDelivery.DeliveryStatus status);
}