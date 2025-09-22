package com.auctionflow.sdk.model;

public class RegisterRequest {
    private String email;
    private String displayName;
    private String password;
    private String role;

    public RegisterRequest() {}

    public RegisterRequest(String email, String displayName, String password, String role) {
        this.email = email;
        this.displayName = displayName;
        this.password = password;
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}