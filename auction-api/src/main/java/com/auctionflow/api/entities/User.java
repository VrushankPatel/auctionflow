package com.auctionflow.api.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Where(clause = "deleted_at IS NULL")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven = false;

    @Column(name = "consent_date")
    private Instant consent_date;

    @Column(name = "data_processing_consent", nullable = false)
    private boolean dataProcessingConsent = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    public enum Role {
        BUYER, SELLER, ADMIN
    }

    public enum KycStatus {
        PENDING, VERIFIED, REJECTED
    }

    // getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }

    public Instant getConsentDate() { return consent_date; }
    public void setConsentDate(Instant consentDate) { this.consent_date = consentDate; }

    public boolean isDataProcessingConsent() { return dataProcessingConsent; }
    public void setDataProcessingConsent(boolean dataProcessingConsent) { this.dataProcessingConsent = dataProcessingConsent; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Long getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Long deletedBy) { this.deletedBy = deletedBy; }
}