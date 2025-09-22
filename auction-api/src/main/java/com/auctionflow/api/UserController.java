package com.auctionflow.api;

import com.auctionflow.analytics.AuditService;
import com.auctionflow.api.entities.DocumentUpload;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.services.IdentityVerificationService;
import com.auctionflow.api.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private IdentityVerificationService identityVerificationService;

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

    @PostMapping("/{id}/kyc/documents")
    public ResponseEntity<?> uploadKycDocument(@PathVariable Long id,
                                               @RequestParam("documentType") String documentType,
                                               @RequestParam("file") MultipartFile file) {
        try {
            DocumentUpload.DocumentType type = DocumentUpload.DocumentType.valueOf(documentType.toUpperCase());
            String result = userService.uploadKycDocument(id, type, file);
            return ResponseEntity.ok(Map.of("message", "Document uploaded successfully", "documentId", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/kyc/verify-identity")
    public ResponseEntity<?> verifyIdentity(@PathVariable Long id, @RequestBody IdentityVerificationRequest request) {
        try {
            IdentityVerificationService.VerificationResult result = identityVerificationService.verifyIdentity(
                id, request.getFirstName(), request.getLastName(), request.getDateOfBirth(), request.getDocumentNumber());
            userService.updateComplianceCheck(id, result);
            return ResponseEntity.ok(result);
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

    @GetMapping("/{id}/consents")
    public ResponseEntity<?> getConsents(@PathVariable Long id, HttpServletRequest request) {
        try {
            UserService.UserConsents consents = userService.getConsents(id);
            auditService.logDataAccess(id, "VIEW_CONSENTS", "User", id, request.getRemoteAddr(), request.getRequestURI());
            return ResponseEntity.ok(consents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/consents")
    public ResponseEntity<?> updateConsents(@PathVariable Long id, @RequestBody UpdateConsentsRequest request) {
        try {
            userService.updateConsents(id, request.isConsentGiven(), request.isDataProcessingConsent());
            return ResponseEntity.ok("Consents updated");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/data")
    public ResponseEntity<?> deleteUserData(@PathVariable Long id) {
        try {
            // Assume the current user is deleting their own data, or admin
            // For simplicity, use id as deletedBy, but in practice, get from security context
            userService.softDeleteUser(id, id);
            return ResponseEntity.ok("User data deleted");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<?> exportUserData(@PathVariable Long id, HttpServletRequest request) {
        try {
            String data = userService.exportUserData(id);
            auditService.logDataAccess(id, "EXPORT_DATA", "User", id, request.getRemoteAddr(), request.getRequestURI());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .header("Content-Disposition", "attachment; filename=user_" + id + "_data.json")
                    .body(data);
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

    public static class UpdateConsentsRequest {
        private boolean consentGiven;
        private boolean dataProcessingConsent;

        public boolean isConsentGiven() { return consentGiven; }
        public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }

        public boolean isDataProcessingConsent() { return dataProcessingConsent; }
        public void setDataProcessingConsent(boolean dataProcessingConsent) { this.dataProcessingConsent = dataProcessingConsent; }
    }

    public static class IdentityVerificationRequest {
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String documentNumber;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    }
}