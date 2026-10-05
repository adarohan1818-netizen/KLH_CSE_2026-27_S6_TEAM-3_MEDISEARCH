package models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a user account for system access.
 *
 * User credentials are stored in data/users/users.txt using standard Java file handling.
 * Passwords are hashed using SHA-256.
 */
public class User {

    private final String username;
    private final String passwordHash;
    private final String role;

    public User(String username, String passwordHash, String role) {
        this.username = safe(username);
        this.passwordHash = safe(passwordHash);
        this.role = safe(role).isEmpty() ? "doctor" : safe(role);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    /**
     * Verifies whether the provided plain-text password matches the stored SHA-256 hash.
     */
    public boolean verifyPassword(String plainPassword) {
        if (plainPassword == null) {
            return false;
        }
        String inputHash = hashPassword(plainPassword);
        return inputHash.equalsIgnoreCase(this.passwordHash);
    }

    /**
     * Computes the SHA-256 hash of a string using java.security.MessageDigest.
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing algorithm not available", e);
        }
    }

    /**
     * Formats user record for saving into data/users/users.txt file.
     */
    public String toFileText() {
        return "username=" + username + "\n"
             + "passwordHash=" + passwordHash + "\n"
             + "role=" + role + "\n";
    }

    /**
     * Hand-rolled JSON representation for client response (omits password hash).
     */
    public String toJson() {
        return "{"
             + "\"username\": \"" + Patient.escapeJson(username) + "\", "
             + "\"role\": \"" + Patient.escapeJson(role) + "\""
             + "}";
    }
}
