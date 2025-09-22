package com.auctionflow.notifications.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notification_types", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "notification_type")
    private Set<String> notificationTypes; // e.g., "BidAccepted", "AuctionWon", "BidOutbid"

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    // Constructors, getters, setters

    public UserPreferences() {}

    public UserPreferences(String userId) {
        this.userId = userId;
        this.emailEnabled = true;
        this.pushEnabled = true;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Set<String> getNotificationTypes() {
        return notificationTypes;
    }

    public void setNotificationTypes(Set<String> notificationTypes) {
        this.notificationTypes = notificationTypes;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public boolean isWithinQuietHours() {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        } else {
            // Overnight quiet hours, e.g., 22:00 to 08:00
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        }
    }

    public boolean shouldSendNotification(String type) {
        return notificationTypes == null || notificationTypes.isEmpty() || notificationTypes.contains(type);
    }
}