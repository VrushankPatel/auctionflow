package com.auctionflow.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class PushNotificationRequest {
    @NotBlank
    private String deviceToken;
    private String deviceId;
    private String platform; // ios, android

    public PushNotificationRequest() {}

    public PushNotificationRequest(String deviceToken, String deviceId, String platform) {
        this.deviceToken = deviceToken;
        this.deviceId = deviceId;
        this.platform = platform;
    }

    // Getters and setters
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}