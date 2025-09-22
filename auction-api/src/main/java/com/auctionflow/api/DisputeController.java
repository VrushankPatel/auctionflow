package com.auctionflow.api;

import com.auctionflow.api.dtos.*;
import com.auctionflow.api.entities.Dispute;
import com.auctionflow.api.entities.DisputeEvidence;
import com.auctionflow.api.services.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/disputes")
@Tag(name = "Disputes", description = "Dispute management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @PostMapping
    @Operation(summary = "Create a new dispute")
    public ResponseEntity<DisputeResponse> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        String userId = getCurrentUserId();
        Dispute dispute = disputeService.createDispute(request.getAuctionId(), userId, request.getReason(), request.getDescription());
        DisputeResponse response = new DisputeResponse(dispute, List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dispute details")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable Long id) {
        Optional<Dispute> disputeOpt = disputeService.getDisputeById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Dispute dispute = disputeOpt.get();
        List<DisputeEvidenceResponse> evidence = disputeService.getEvidenceForDispute(id).stream()
                .map(DisputeEvidenceResponse::new)
                .collect(Collectors.toList());
        DisputeResponse response = new DisputeResponse(dispute, evidence);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List user's disputes")
    public ResponseEntity<List<DisputeResponse>> getUserDisputes() {
        String userId = getCurrentUserId();
        List<Dispute> disputes = disputeService.getDisputesByInitiator(userId);
        List<DisputeResponse> responses = disputes.stream()
                .map(d -> {
                    List<DisputeEvidenceResponse> evidence = disputeService.getEvidenceForDispute(d.getId()).stream()
                            .map(DisputeEvidenceResponse::new)
                            .collect(Collectors.toList());
                    return new DisputeResponse(d, evidence);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/evidence")
    @Operation(summary = "Submit evidence for a dispute")
    public ResponseEntity<DisputeEvidenceResponse> submitEvidence(@PathVariable Long id, @Valid @RequestBody SubmitEvidenceRequest request) {
        String userId = getCurrentUserId();
        DisputeEvidence evidence = disputeService.submitEvidence(id, userId, request.getEvidenceType(), request.getContent());
        DisputeEvidenceResponse response = new DisputeEvidenceResponse(evidence);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Admin endpoints
    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve a dispute (admin only)")
    public ResponseEntity<DisputeResponse> resolveDispute(@PathVariable Long id, @RequestBody ResolveDisputeRequest request) {
        // Assume admin check here
        // For simplicity, assume capture if resolution notes contain "capture" or something
        boolean capture = request.getResolutionNotes().toLowerCase().contains("capture");
        Dispute dispute = disputeService.resolveDispute(id, getCurrentUserId(), request.getResolutionNotes(), capture);
        DisputeResponse response = new DisputeResponse(dispute, List.of());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a dispute (admin only)")
    public ResponseEntity<DisputeResponse> closeDispute(@PathVariable Long id) {
        // Assume admin check here
        Dispute dispute = disputeService.closeDispute(id);
        DisputeResponse response = new DisputeResponse(dispute, List.of());
        return ResponseEntity.ok(response);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}