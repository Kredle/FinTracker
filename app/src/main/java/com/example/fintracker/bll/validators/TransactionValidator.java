package com.example.fintracker.bll.validators;

/**
 * Utility class for validating transaction-related data.
 * Provides static methods for transaction amount and title validation.
 * Follows business logic best practices by separating validation concerns.
 */
public class TransactionValidator {

    /**
     * Validates transaction amount.
     * Requirements: must be greater than 0, must be a valid finite double (not NaN or Infinity).
     *
     * @param amount Transaction amount to validate
     * @return true if amount is valid
     * @throws IllegalArgumentException if amount is invalid (negative, zero, NaN, or infinite)
     */
    public static boolean isValidAmount(double amount) {
        if (Double.isNaN(amount)) {
            throw new IllegalArgumentException("Transaction amount cannot be NaN (Not a Number)");
        }
        if (Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Transaction amount cannot be infinite");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than 0");
        }
        return true;
    }

    /**
     * Validates transaction title.
     * Requirements: not null, not empty, no leading/trailing whitespace, max 50 characters.
     *
     * @param title Transaction title to validate
     * @return true if title is valid
     * @throws IllegalArgumentException if title is null or violates constraints
     */
    public static boolean isValidTitle(String title) {
        if (title == null) {
            throw new IllegalArgumentException("Transaction title cannot be null");
        }

        if (!title.equals(title.trim())) {
            throw new IllegalArgumentException("Transaction title cannot have leading or trailing whitespace");
        }

        if (title.isEmpty()) {
            throw new IllegalArgumentException("Transaction title cannot be empty");
        }

        if (title.length() > 50) {
            throw new IllegalArgumentException("Transaction title must not exceed 50 characters");
        }

        return true;
    }

    /**
     * Convenience method to validate all transaction creation fields at once.
     *
     * @param amount Transaction amount to validate
     * @param title Transaction title to validate
     * @return true if all fields are valid
     * @throws IllegalArgumentException if any field is invalid
     */
    public static boolean validateTransaction(double amount, String title) {
        isValidAmount(amount);
        isValidTitle(title);
        return true;
    }
}

