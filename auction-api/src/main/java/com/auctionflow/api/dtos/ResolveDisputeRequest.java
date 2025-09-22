package com.auctionflow.api.dtos;

import javax.validation.constraints.NotBlank;

public class ResolveDisputeRequest {

    @NotBlank
    private String resolutionNotes;

    public ResolveDisputeRequest() {}

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}