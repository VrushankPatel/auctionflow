package com.auctionflow.api.dtos;

import javax.validation.constraints.NotBlank;

public class DeviceTrackingRequest {
    @NotBlank
    private String deviceId;
    private String deviceModel;
    private String osVersion;
    private String appVersion;
    private String location; // optional

    public DeviceTrackingRequest() {}

    public DeviceTrackingRequest(String deviceId, String deviceModel, String osVersion, String appVersion, String location) {
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.location = location;
    }

    // Getters and setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}