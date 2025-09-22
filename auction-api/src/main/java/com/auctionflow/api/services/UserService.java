package com.auctionflow.api.services;

import com.auctionflow.api.entities.User;
import com.auctionflow.api.repositories.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);

    public User register(String email, String displayName, String password, User.Role role) {
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

    public void changeRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        userRepository.save(user);
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