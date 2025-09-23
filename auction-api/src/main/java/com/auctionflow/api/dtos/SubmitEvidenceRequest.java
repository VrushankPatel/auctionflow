package com.auctionflow.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmitEvidenceRequest {

    @NotBlank
    private String evidenceType; // 'TEXT', 'IMAGE', 'DOCUMENT'

    @NotBlank
    private String content;

    public SubmitEvidenceRequest() {}

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
}