package com.auctionflow.api;

import com.auctionflow.api.entities.User;
import com.auctionflow.api.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/{id}/kyc/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyKyc(@PathVariable Long id) {
        try {
            userService.verifyKyc(id);
            return ResponseEntity.ok("KYC verified");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/kyc/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectKyc(@PathVariable Long id) {
        try {
            userService.rejectKyc(id);
            return ResponseEntity.ok("KYC rejected");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody ChangeRoleRequest request) {
        try {
            User.Role role = User.Role.valueOf(request.getRole().toUpperCase());
            userService.changeRole(id, role);
            return ResponseEntity.ok("Role changed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/2fa/setup")
    public ResponseEntity<?> setup2fa(@PathVariable Long id) {
        // Assume user can setup for themselves, or admin
        try {
            String secret = userService.generateTotpSecret(id);
            Map<String, String> response = new HashMap<>();
            response.put("secret", secret);
            // In practice, generate QR code URL
            response.put("qrCodeUrl", "otpauth://totp/AuctionFlow:" + id + "?secret=" + secret + "&issuer=AuctionFlow");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/2fa/enable")
    public ResponseEntity<?> enable2fa(@PathVariable Long id, @RequestBody TotpRequest request) {
        try {
            boolean valid = userService.enable2fa(id, request.getCode());
            if (valid) {
                return ResponseEntity.ok("2FA enabled");
            } else {
                return ResponseEntity.badRequest().body("Invalid code");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/2fa/disable")
    public ResponseEntity<?> disable2fa(@PathVariable Long id) {
        try {
            userService.disable2fa(id);
            return ResponseEntity.ok("2FA disabled");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    public static class ChangeRoleRequest {
        private String role;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class TotpRequest {
        private String code;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}