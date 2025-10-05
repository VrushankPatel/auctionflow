package com.auctionflow.api;

import com.auctionflow.api.config.JwtTokenProvider;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.services.UserService;
import com.auctionflow.events.publisher.KafkaEventPublisher;
import com.auctionflow.core.domain.events.FailedLoginEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Profile("!ui-only")
public class AuthController {

    @Autowired
    private Optional<AuthenticationManager> authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private KafkaEventPublisher eventPublisher;

    // @Autowired
    // private SuspiciousActivityService suspiciousActivityService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User.Role role = registerRequest.getRole() != null ? User.Role.valueOf(registerRequest.getRole().toUpperCase()) : User.Role.BUYER;
            User user = userService.createUser(registerRequest.getEmail(), registerRequest.getDisplayName(), registerRequest.getPassword(), role.name());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        if (!authenticationManager.isPresent()) {
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", "Authentication service not available"));
        }
        try {
            // Support both email and username fields
            String username = loginRequest.getEmail() != null ? loginRequest.getEmail() : loginRequest.getUsername();
            
            Authentication authentication = authenticationManager.get().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            // Get user details
            User user = userService.getUserByEmail(username);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "USER_NOT_FOUND", "message", "User not found"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt); // Frontend expects "token" not "accessToken"
            response.put("accessToken", jwt); // Keep for backward compatibility
            response.put("refreshToken", refreshToken);
            
            // Add user object
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId().toString());
            userMap.put("email", user.getEmail());
            userMap.put("displayName", user.getDisplayName());
            userMap.put("role", user.getRole().name());
            response.put("user", userMap);

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            // Publish failed login event
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String username = loginRequest.getEmail() != null ? loginRequest.getEmail() : loginRequest.getUsername();
            FailedLoginEvent failedEvent = new FailedLoginEvent(
                java.util.UUID.randomUUID(),
                java.time.Instant.now(),
                ipAddress,
                userAgent,
                username,
                "Invalid credentials",
                java.util.Map.of("attemptCount", 1) // Could track in Redis
            );
            eventPublisher.publishSecurityEvent(failedEvent);
            // suspiciousActivityService.recordFailedLogin(ipAddress);

            return ResponseEntity.status(401).body(Map.of("error", "INVALID_CREDENTIALS", "message", "Invalid email or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (tokenProvider.validateToken(refreshToken)) {
            String username = tokenProvider.getUsernameFromJwtToken(refreshToken);
            // In practice, validate refresh token against stored tokens in DB
            // If valid, generate new access and refresh tokens, store new refresh token, invalidate old one

            // For now, simplified: generate new tokens
            // Assuming we can create authentication from username
            // This is a placeholder; proper implementation needs UserDetailsService and token storage

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", "newAccessToken"); // Generate properly with authentication
            response.put("refreshToken", "newRefreshToken"); // Rotate: generate new and store

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<?> oauth2Success(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        // Handle OAuth2 user, create or update user in DB, generate JWT

        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", jwt);
        response.put("refreshToken", refreshToken);
        response.put("user", oauth2User.getAttributes().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/failure")
    public ResponseEntity<?> oauth2Failure() {
        return ResponseEntity.badRequest().body("OAuth2 login failed");
    }

    public static class LoginRequest {
        private String username;
        private String email; // Support email field for frontend compatibility
        private String password;

        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class RegisterRequest {
        private String email;
        private String displayName;
        private String password;
        private String role;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}