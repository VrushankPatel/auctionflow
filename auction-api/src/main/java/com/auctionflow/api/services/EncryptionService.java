package com.auctionflow.api.services;

import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {
    private final TextEncryptor textEncryptor;

    public EncryptionService(TextEncryptor textEncryptor) {
        this.textEncryptor = textEncryptor;
    }

    public String encrypt(String text) {
        return textEncryptor.encrypt(text);
    }

    public String decrypt(String encryptedText) {
        return textEncryptor.decrypt(encryptedText);
    }
}