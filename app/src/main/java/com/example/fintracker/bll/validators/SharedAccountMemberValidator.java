package com.example.fintracker.bll.validators;

/**
 * Utility class for validating shared account member-related data.
 * Provides static methods for role validation.
 * Follows business logic best practices by separating validation concerns.
 */
public class SharedAccountMemberValidator {

    /**
     * Validates shared account member role.
     * Requirements: must be exactly "ADMIN" or "USER" (case-sensitive, no leading/trailing whitespace).
     * This strict validation ensures consistent stored values.
     *
     * @param role Member role to validate (must be exactly "ADMIN" or "USER")
     * @return true if role is valid
     * @throws IllegalArgumentException if role is null, has whitespace, or is not an exact match
     */
    public static boolean isValidRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Member role cannot be null");
        }

        // Enforce strict validation: reject leading/trailing whitespace
        if (!role.equals(role.trim())) {
            throw new IllegalArgumentException("Member role cannot have leading or trailing whitespace. Must be exactly ADMIN or USER");
        }

        // Require exact case match to ensure consistent storage
        if (!("ADMIN".equals(role) || "USER".equals(role))) {
            throw new IllegalArgumentException("Member role must be exactly ADMIN or USER (case-sensitive)");
        }

        return true;
    }

    /**
     * Convenience method to validate shared account member creation.
     *
     * @param role Member role to validate
     * @return true if role is valid
     * @throws IllegalArgumentException if role is invalid
     */
    public static boolean validateMemberCreation(String role) {
        isValidRole(role);
        return true;
    }
}

