package com.auctionflow.notifications;

import com.auctionflow.notifications.entity.UnsubscribeToken;
import com.auctionflow.notifications.entity.UserPreferences;
import com.auctionflow.notifications.repository.UnsubscribeTokenRepository;
import com.auctionflow.notifications.repository.UserPreferencesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final UserPreferencesRepository preferencesRepository;
    private final UnsubscribeTokenRepository tokenRepository;

    public NotificationController(UserPreferencesRepository preferencesRepository,
                                  UnsubscribeTokenRepository tokenRepository) {
        this.preferencesRepository = preferencesRepository;
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/preferences/{userId}")
    public ResponseEntity<UserPreferences> getPreferences(@PathVariable String userId) {
        Optional<UserPreferences> prefs = preferencesRepository.findByUserId(userId);
        if (prefs.isPresent()) {
            return ResponseEntity.ok(prefs.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/preferences/{userId}")
    public ResponseEntity<UserPreferences> updatePreferences(@PathVariable String userId,
                                                             @RequestParam(required = false) Boolean emailEnabled,
                                                             @RequestParam(required = false) Boolean pushEnabled,
                                                             @RequestParam(required = false) Set<String> notificationTypes,
                                                             @RequestParam(required = false) String quietHoursStart,
                                                             @RequestParam(required = false) String quietHoursEnd) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(new UserPreferences(userId));

        if (emailEnabled != null) prefs.setEmailEnabled(emailEnabled);
        if (pushEnabled != null) prefs.setPushEnabled(pushEnabled);
        if (notificationTypes != null) prefs.setNotificationTypes(notificationTypes);
        if (quietHoursStart != null) prefs.setQuietHoursStart(LocalTime.parse(quietHoursStart));
        if (quietHoursEnd != null) prefs.setQuietHoursEnd(LocalTime.parse(quietHoursEnd));

        UserPreferences saved = preferencesRepository.save(prefs);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam String token) {
        Optional<UnsubscribeToken> optToken = tokenRepository.findByToken(token);
        if (optToken.isPresent()) {
            UnsubscribeToken unsubscribeToken = optToken.get();
            if (!unsubscribeToken.isUsed()) {
                // Disable email for the user
                Optional<UserPreferences> prefsOpt = preferencesRepository.findByUserId(unsubscribeToken.getUserId());
                if (prefsOpt.isPresent()) {
                    UserPreferences prefs = prefsOpt.get();
                    prefs.setEmailEnabled(false);
                    preferencesRepository.save(prefs);
                } else {
                    UserPreferences prefs = new UserPreferences(unsubscribeToken.getUserId());
                    prefs.setEmailEnabled(false);
                    preferencesRepository.save(prefs);
                }
                unsubscribeToken.setUsedAt(java.time.LocalDateTime.now());
                tokenRepository.save(unsubscribeToken);
                return ResponseEntity.ok("Successfully unsubscribed from email notifications.");
            } else {
                return ResponseEntity.badRequest().body("Token already used.");
            }
        } else {
            return ResponseEntity.badRequest().body("Invalid token.");
        }
    }
}