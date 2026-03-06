package com.example.fintracker.bll.validators;

/**
 * Utility class for validating account-related data.
 * Provides static methods for account name and balance validation.
 * Follows business logic best practices by separating validation concerns.
 */
public class AccountValidator {

    /**
     * Validates account name.
     * Requirements: not empty, not null, max 30 characters.
     *
     * @param accountName Account name to validate
     * @return true if account name is valid
     * @throws IllegalArgumentException if account name is null or violates constraints
     */
    public static boolean isValidAccountName(String accountName) {
        if (accountName == null) {
            throw new IllegalArgumentException("Account name cannot be null");
        }

        // Reject names with leading/trailing whitespace
        if (!accountName.equals(accountName.trim())) {
            throw new IllegalArgumentException("Account name cannot have leading or trailing whitespace");
        }

        if (accountName.isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }

        if (accountName.length() > 30) {
            throw new IllegalArgumentException("Account name must not exceed 30 characters");
        }

        return true;
    }

    /**
     * Validates account balance.
     * Requirements: cannot be negative, must be a valid double.
     *
     * @param balance Account balance to validate
     * @return true if balance is valid
     * @throws IllegalArgumentException if balance is negative or invalid
     */
    public static boolean isValidBalance(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Account balance cannot be negative. Provided: " + balance);
        }
        return true;
    }

    /**
     * Convenience method to validate all account creation fields at once.
     *
     * @param accountName Account name to validate
     * @param balance Initial balance to validate
     * @return true if all fields are valid
     * @throws IllegalArgumentException if any field is invalid
     */
    public static boolean validateAccountCreation(String accountName, double balance) {
        isValidAccountName(accountName);
        isValidBalance(balance);
        return true;
    }
}

