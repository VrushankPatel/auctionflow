package com.auctionflow.notifications.service;

import com.auctionflow.notifications.entity.UnsubscribeToken;
import com.auctionflow.notifications.entity.UserPreferences;
import com.auctionflow.notifications.repository.UnsubscribeTokenRepository;
import com.auctionflow.notifications.repository.UserPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final UserPreferencesRepository preferencesRepository;
    private final UnsubscribeTokenRepository tokenRepository;

    public EmailService(JavaMailSender mailSender, UserPreferencesRepository preferencesRepository,
                        UnsubscribeTokenRepository tokenRepository) {
        this.mailSender = mailSender;
        this.preferencesRepository = preferencesRepository;
        this.tokenRepository = tokenRepository;
    }

    public boolean sendEmail(String userId, String email, String subject, String text, String notificationType) {
        // Check user preferences
        Optional<UserPreferences> prefsOpt = preferencesRepository.findByUserId(userId);
        if (prefsOpt.isPresent()) {
            UserPreferences prefs = prefsOpt.get();
            if (!prefs.isEmailEnabled()) {
                logger.info("Email disabled for user {}", userId);
                return false;
            }
            if (!prefs.shouldSendNotification(notificationType)) {
                logger.info("Notification type {} disabled for user {}", notificationType, userId);
                return false;
            }
            if (prefs.isWithinQuietHours()) {
                logger.info("Within quiet hours for user {}, skipping email", userId);
                return false;
            }
        }

        // Generate or get unsubscribe token
        String unsubscribeToken = getOrCreateUnsubscribeToken(userId, email);
        String unsubscribeLink = "https://yourapp.com/unsubscribe?token=" + unsubscribeToken;
        String fullText = text + "\n\nTo unsubscribe, click here: " + unsubscribeLink;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(fullText);
            mailSender.send(message);
            logger.info("Email sent to {}", email);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send email to {}", email, e);
            return false;
        }
    }

    private String getOrCreateUnsubscribeToken(String userId, String email) {
        Optional<UnsubscribeToken> existing = tokenRepository.findByUserIdAndEmail(userId, email);
        if (existing.isPresent()) {
            return existing.get().getToken();
        } else {
            String token = UUID.randomUUID().toString();
            UnsubscribeToken newToken = new UnsubscribeToken(token, userId, email);
            tokenRepository.save(newToken);
            return token;
        }
    }
}