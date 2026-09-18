package com.bank.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class providing cryptographic hash functions for PIN verification.
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // Prevent instantiation
    }

    /**
     * Hashes a 4-digit PIN using SHA-256 with a standard salt.
     */
    public static String hashPin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 numeric digits.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = "BANK_SALT_V1_" + pin;
            byte[] encodedHash = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validates if a raw PIN matches the stored hash.
     */
    public static boolean verifyPin(String rawPin, String storedHash) {
        if (rawPin == null || storedHash == null) {
            return false;
        }
        try {
            return hashPin(rawPin).equalsIgnoreCase(storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
