package com.auctionflow.core.domain.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {

    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a random salt for hashing.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Computes the hash of the bid amount with salt.
     */
    public static String hashBid(String bidAmount, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            String input = bidAmount + salt;
            byte[] hashBytes = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    /**
     * Verifies if the provided bid amount matches the hash with the given salt.
     */
    public static boolean verifyBid(String bidAmount, String salt, String expectedHash) {
        String computedHash = hashBid(bidAmount, salt);
        return computedHash.equals(expectedHash);
    }
}