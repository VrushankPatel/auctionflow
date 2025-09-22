package com.auctionflow.api.dtos;

import java.time.Instant;
import java.util.List;

public class DisputeResponse {

    private Long id;
    private String auctionId;
    private String initiatorId;
    private String reason;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant resolvedAt;
    private String resolverId;
    private String resolutionNotes;
    private List<DisputeEvidenceResponse> evidence;

    public DisputeResponse() {}

    // Constructor from entity
    public DisputeResponse(com.auctionflow.api.entities.Dispute dispute, List<DisputeEvidenceResponse> evidence) {
        this.id = dispute.getId();
        this.auctionId = dispute.getAuctionId();
        this.initiatorId = dispute.getInitiatorId();
        this.reason = dispute.getReason();
        this.description = dispute.getDescription();
        this.status = dispute.getStatus();
        this.createdAt = dispute.getCreatedAt();
        this.resolvedAt = dispute.getResolvedAt();
        this.resolverId = dispute.getResolverId();
        this.resolutionNotes = dispute.getResolutionNotes();
        this.evidence = evidence;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolverId() {
        return resolverId;
    }

    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public List<DisputeEvidenceResponse> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<DisputeEvidenceResponse> evidence) {
        this.evidence = evidence;
    }
}