package com.auctionflow.notifications.repository;

import com.auctionflow.notifications.entity.BatchedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchedNotificationRepository extends JpaRepository<BatchedNotification, Long> {

    List<BatchedNotification> findByUserIdAndSent(String userId, boolean sent);
}