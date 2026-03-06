package com.example.fintracker.bll.validators;

/**
 * Utility class for validating tag-related data.
 * Provides static methods for tag name validation.
 * Follows business logic best practices by separating validation concerns.
 */
public class TagValidator {

    /**
     * Validates tag name.
     * Requirements: not empty, not null, max 20 characters.
     *
     * @param tagName Tag name to validate
     * @return true if tag name is valid
     * @throws IllegalArgumentException if tag name is null or violates constraints
     */
    public static boolean isValidTagName(String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException("Tag name cannot be null");
        }

        // Reject names with leading/trailing whitespace
        if (!tagName.equals(tagName.trim())) {
            throw new IllegalArgumentException("Tag name cannot have leading or trailing whitespace");
        }

        if (tagName.isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }

        if (tagName.length() > 20) {
            throw new IllegalArgumentException("Tag name must not exceed 20 characters");
        }

        return true;
    }

    /**
     * Convenience method to validate tag creation.
     * Currently only validates tag name, but provides extension point for future validations.
     *
     * @param tagName Tag name to validate
     * @return true if all fields are valid
     * @throws IllegalArgumentException if any field is invalid
     */
    public static boolean validateTagCreation(String tagName) {
        isValidTagName(tagName);
        return true;
    }
}

