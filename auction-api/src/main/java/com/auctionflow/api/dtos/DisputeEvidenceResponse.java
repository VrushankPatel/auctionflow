package com.auctionflow.api.dtos;

import java.time.Instant;

public class DisputeEvidenceResponse {

    private Long id;
    private Long disputeId;
    private String submittedBy;
    private String evidenceType;
    private String content;
    private Instant submittedAt;

    public DisputeEvidenceResponse() {}

    // Constructor from entity
    public DisputeEvidenceResponse(com.auctionflow.api.entities.DisputeEvidence evidence) {
        this.id = evidence.getId();
        this.disputeId = evidence.getDisputeId();
        this.submittedBy = evidence.getSubmittedBy();
        this.evidenceType = evidence.getEvidenceType();
        this.content = evidence.getContent();
        this.submittedAt = evidence.getSubmittedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDisputeId() {
        return disputeId;
    }

    public void setDisputeId(Long disputeId) {
        this.disputeId = disputeId;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}