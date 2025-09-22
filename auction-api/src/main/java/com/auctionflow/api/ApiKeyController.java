package com.auctionflow.api;

import com.auctionflow.api.services.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateKey(@RequestBody GenerateKeyRequest request) {
        try {
            String rawKey = apiKeyService.generateKey(request.getServiceName());
            Map<String, String> response = new HashMap<>();
            response.put("serviceName", request.getServiceName());
            response.put("apiKey", rawKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeKey(@RequestBody RevokeKeyRequest request) {
        try {
            apiKeyService.revokeKey(request.getServiceName());
            return ResponseEntity.ok("API key revoked for service: " + request.getServiceName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    public static class GenerateKeyRequest {
        private String serviceName;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    }

    public static class RevokeKeyRequest {
        private String serviceName;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    }
}