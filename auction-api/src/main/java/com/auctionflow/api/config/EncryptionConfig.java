package com.auctionflow.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfig {
    @Bean
    public TextEncryptor textEncryptor() {
        // Use a strong password and salt in production
        String password = "mySecretKey";
        String salt = "5c0744940b5c369b";
        return Encryptors.text(password, salt);
    }
}