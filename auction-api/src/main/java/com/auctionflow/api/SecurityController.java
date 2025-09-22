package com.auctionflow.api;

import com.auctionflow.api.services.SuspiciousActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/security")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityController {

    @Autowired
    private SuspiciousActivityService suspiciousActivityService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getSecurityStatus() {
        // Placeholder for security metrics
        return ResponseEntity.ok(Map.of("status", "Security monitoring active", "events", "Streaming to Kafka"));
    }
}