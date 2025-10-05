package com.auctionflow.api.services;

import com.auctionflow.api.entities.ComplianceCheck;
import com.auctionflow.api.entities.DocumentUpload;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.repositories.ComplianceCheckRepository;
import com.auctionflow.api.repositories.DocumentUploadRepository;
import com.auctionflow.api.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplianceCheckRepository complianceCheckRepository;

    @Autowired
    private DocumentUploadRepository documentUploadRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${gdpr.retention.softDeleteDays:30}")
    private int softDeleteRetentionDays;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);

    public User createUser(String email, String displayName, String password, String roleStr) {
        if (roleStr == null) roleStr = "BUYER";
        User.Role role = User.Role.valueOf(roleStr.toUpperCase());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setKycStatus(User.KycStatus.PENDING);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    public void verifyKyc(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setKycStatus(User.KycStatus.VERIFIED);
        userRepository.save(user);
    }

    public void rejectKyc(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setKycStatus(User.KycStatus.REJECTED);
        userRepository.save(user);
    }

    public boolean isKycVerified(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getKycStatus() == User.KycStatus.VERIFIED;
    }

    public String uploadKycDocument(Long userId, DocumentUpload.DocumentType documentType, MultipartFile file) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Basic validation - you might want more sophisticated checks
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new RuntimeException("Invalid file type. Only images and PDFs are allowed.");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new RuntimeException("File size exceeds 10MB limit");
        }

        // In a real implementation, you'd save the file to a secure storage (S3, etc.)
        // For now, we'll just store the path as a placeholder
        String filePath = "/uploads/kyc/" + userId + "/" + documentType.name().toLowerCase() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        DocumentUpload document = new DocumentUpload();
        document.setUserId(userId);
        document.setDocumentType(documentType);
        document.setFileName(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setMimeType(contentType);
        document.setFileSize(file.getSize());

        DocumentUpload saved = documentUploadRepository.save(document);

        // Create a compliance check record for document verification
        ComplianceCheck check = new ComplianceCheck();
        check.setUserId(userId);
        check.setCheckType(ComplianceCheck.CheckType.DOCUMENT_VERIFICATION);
        check.setStatus(ComplianceCheck.CheckStatus.PENDING);
        check.setDetails("{\"documentId\": " + saved.getId() + ", \"documentType\": \"" + documentType + "\"}");
        complianceCheckRepository.save(check);

        return saved.getId().toString();
    }

    public void updateComplianceCheck(Long userId, IdentityVerificationService.VerificationResult result) {
        ComplianceCheck check = new ComplianceCheck();
        check.setUserId(userId);
        check.setCheckType(ComplianceCheck.CheckType.IDENTITY_VERIFICATION);
        check.setStatus(result.isVerified() ? ComplianceCheck.CheckStatus.PASSED : ComplianceCheck.CheckStatus.FAILED);
        check.setRiskScore(result.isVerified() ? BigDecimal.valueOf(10.0) : BigDecimal.valueOf(80.0)); // Lower risk if verified

        ObjectMapper mapper = new ObjectMapper();
        try {
            check.setDetails(mapper.writeValueAsString(Map.of(
                "verified", result.isVerified(),
                "confidence", result.getConfidence(),
                "provider", result.getProvider(),
                "checkId", result.getCheckId(),
                "failureReason", result.getFailureReason()
            )));
        } catch (Exception e) {
            check.setDetails("{\"error\": \"Failed to serialize result\"}");
        }

        complianceCheckRepository.save(check);

        // Update user KYC status based on verification
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (result.isVerified()) {
            user.setKycStatus(User.KycStatus.VERIFIED);
        } else {
            user.setKycStatus(User.KycStatus.REJECTED);
        }
        userRepository.save(user);
    }

    public void changeRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public String generateTotpSecret(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        // Generate TOTP secret
        String secret = generateSecret(); // placeholder
        user.setTotpSecret(secret);
        userRepository.save(user);
        return secret;
    }

    public boolean enable2fa(Long userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getTotpSecret() == null) {
            throw new RuntimeException("TOTP not set up");
        }
        boolean valid = verifyTotpCode(user.getTotpSecret(), code);
        if (valid) {
            // Perhaps set a flag, but for now, just return true
        }
        return valid;
    }

    public boolean verifyTotpCode(Long userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getTotpSecret() == null) {
            return false;
        }
        return verifyTotpCode(user.getTotpSecret(), code);
    }

    public void disable2fa(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setTotpSecret(null);
        userRepository.save(user);
    }

    public UserConsents getConsents(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return new UserConsents(user.isConsentGiven(), user.getConsentDate(), user.isDataProcessingConsent());
    }

    public void updateConsents(Long userId, boolean consentGiven, boolean dataProcessingConsent) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setConsentGiven(consentGiven);
        user.setDataProcessingConsent(dataProcessingConsent);
        if (consentGiven || dataProcessingConsent) {
            user.setConsentDate(Instant.now());
        }
        userRepository.save(user);
    }

    public void softDeleteUser(Long userId, Long deletedBy) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setDeletedAt(Instant.now());
        user.setDeletedBy(deletedBy);
        userRepository.save(user);
        // Note: In a real implementation, also soft delete related auctions, bids, etc.
        // For now, just user.
    }

    public String exportUserData(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("userId", user.getId().toString());
        root.put("email", user.getEmail());
        root.put("displayName", user.getDisplayName());
        root.put("role", user.getRole().name());
        root.put("kycStatus", user.getKycStatus().name());
        root.put("createdAt", user.getCreatedAt().toString());
        root.put("consentGiven", user.isConsentGiven());
        root.put("consentDate", user.getConsentDate() != null ? user.getConsentDate().toString() : null);
        root.put("dataProcessingConsent", user.isDataProcessingConsent());
        // In a real implementation, add auctions, bids, payments, etc.
        // For example, query AuctionRepository for auctions by seller, etc.
        // But for now, just user data.
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export data", e);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void cleanupSoftDeletedData() {
        Instant cutoff = Instant.now().minus(softDeleteRetentionDays, ChronoUnit.DAYS);
        userRepository.deleteSoftDeletedUsers(cutoff);
        // Similarly for auctions, bids, etc.
    }

    public static class UserConsents {
        private boolean consentGiven;
        private Instant consentDate;
        private boolean dataProcessingConsent;

        public UserConsents(boolean consentGiven, Instant consentDate, boolean dataProcessingConsent) {
            this.consentGiven = consentGiven;
            this.consentDate = consentDate;
            this.dataProcessingConsent = dataProcessingConsent;
        }

        public boolean isConsentGiven() { return consentGiven; }
        public Instant getConsentDate() { return consentDate; }
        public boolean isDataProcessingConsent() { return dataProcessingConsent; }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }

    private String generateSecret() {
        return secretGenerator.generate();
    }

    private boolean verifyTotpCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}