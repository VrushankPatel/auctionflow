package com.auctionflow.notifications.service;

import com.auctionflow.notifications.entity.UserPreferences;
import com.auctionflow.notifications.repository.UserPreferencesRepository;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    private final FirebaseMessaging firebaseMessaging;
    private final ApnsClient apnsClient;
    private final UserPreferencesRepository preferencesRepository;

    public PushNotificationService(
            @Value("${firebase.config.path}") String firebaseConfigPath,
            @Value("${apns.key-id}") String apnsKeyId,
            @Value("${apns.team-id}") String apnsTeamId,
            @Value("${apns.topic}") String apnsTopic,
            @Value("${apns.key-path}") String apnsKeyPath,
            UserPreferencesRepository preferencesRepository) throws IOException {

        // Initialize Firebase
        this.firebaseMessaging = FirebaseMessaging.getInstance();

        // Initialize APNS
        this.apnsClient = new ApnsClientBuilder()
                .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST) // or PRODUCTION_APNS_HOST
                .setSigningKey(ApnsClientBuilder.loadSigningKey(new File(apnsKeyPath)))
                .setTeamId(apnsTeamId)
                .setKeyId(apnsKeyId)
                .build();

        this.preferencesRepository = preferencesRepository;
    }

    public boolean sendFCMNotification(String token, String title, String body) {
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(token)
                    .build();

            String response = firebaseMessaging.send(message);
            logger.info("FCM notification sent: {}", response);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send FCM notification", e);
            return false;
        }
    }

    public boolean sendAPNSNotification(String deviceToken, String title, String body) {
        try {
            final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
            payloadBuilder.setAlertTitle(title);
            payloadBuilder.setAlertBody(body);

            final String payload = payloadBuilder.build();
            final String token = deviceToken;

            final SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, "com.auctionflow.app", payload);

            apnsClient.sendNotification(pushNotification).get();
            logger.info("APNS notification sent to {}", deviceToken);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to send APNS notification", e);
            return false;
        }
    }

    public boolean sendPushNotification(String userId, String notificationType, String title, String body) {
        // Check user preferences
        Optional<UserPreferences> prefsOpt = preferencesRepository.findByUserId(userId);
        if (prefsOpt.isPresent()) {
            UserPreferences prefs = prefsOpt.get();
            if (!prefs.isPushEnabled()) {
                logger.info("Push notifications disabled for user {}", userId);
                return false;
            }
            if (!prefs.shouldSendNotification(notificationType)) {
                logger.info("Notification type {} disabled for user {}", notificationType, userId);
                return false;
            }
        }

        // Assume FCM token stored in Redis, but since we don't have it here, return false for now
        // In real implementation, you'd retrieve the token and call sendFCMNotification or sendAPNSNotification
        logger.info("Push notification would be sent to user {} for type {}", userId, notificationType);
        return false; // Placeholder
    }
}