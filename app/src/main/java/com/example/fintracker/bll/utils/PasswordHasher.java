package com.example.fintracker.bll.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing passwords using SHA-256.
 *
 * NOTE: For production apps, prefer BCrypt (via a library like jBCrypt).
 * SHA-256 is used here to avoid adding external dependencies, but it is
 * not salted. If you add the jBCrypt dependency, replace this with BCrypt.
 *
 * To add BCrypt:
 *   implementation("de.svenkubiak:jBCrypt:0.4.3") in build.gradle.kts
 *   Then: BCrypt.hashpw(password, BCrypt.gensalt())
 *         BCrypt.checkpw(password, storedHash)
 */
public final class PasswordHasher {

    private PasswordHasher() {}

    /**
     * Hashes a plain-text password using SHA-256.
     *
     * @param plainPassword The raw password from user input
     * @return Hex-encoded SHA-256 hash string
     * @throws RuntimeException if SHA-256 algorithm is not available (never happens on Android)
     */
    public static String hash(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on Android
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored hash.
     *
     * @param plainPassword The raw password to verify
     * @param storedHash    The hash stored in the database
     * @return true if the password matches the hash
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        return hash(plainPassword).equals(storedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}