package com.example.fintracker.bll.validators;

/**
 * Utility class for validating limit-related data.
 * Provides static methods for spending limit amount and period validation.
 * Follows business logic best practices by separating validation concerns.
 */
public class LimitValidator {

    /**
     * Validates limit amount.
     * Requirements: must be greater than 0, must be a valid finite double (not NaN or Infinity).
     *
     * @param amountLimit Limit amount to validate
     * @return true if amount is valid
     * @throws IllegalArgumentException if amount is invalid (negative, zero, NaN, or infinite)
     */
    public static boolean isValidAmountLimit(double amountLimit) {
        if (Double.isNaN(amountLimit)) {
            throw new IllegalArgumentException("Limit amount cannot be NaN (Not a Number)");
        }
        if (Double.isInfinite(amountLimit)) {
            throw new IllegalArgumentException("Limit amount cannot be infinite");
        }
        if (amountLimit <= 0) {
            throw new IllegalArgumentException("Limit amount must be greater than 0");
        }
        return true;
    }

    /**
     * Validates limit period.
     * Requirements: must be exactly "DAY", "WEEK", or "MONTH" (case-sensitive, no leading/trailing whitespace).
     * This strict validation ensures consistent stored values and prevents normalization issues.
     *
     * @param period Limit period to validate (must be exactly "DAY", "WEEK", or "MONTH")
     * @return true if period is valid
     * @throws IllegalArgumentException if period is null, has whitespace, or is not an exact match
     */
    public static boolean isValidPeriod(String period) {
        if (period == null) {
            throw new IllegalArgumentException("Limit period cannot be null");
        }

        // Enforce strict validation: reject leading/trailing whitespace
        if (!period.equals(period.trim())) {
            throw new IllegalArgumentException("Limit period cannot have leading or trailing whitespace. Must be exactly DAY, WEEK, or MONTH");
        }

        // Require exact case match to ensure consistent storage
        if (!("DAY".equals(period) || "WEEK".equals(period) || "MONTH".equals(period))) {
            throw new IllegalArgumentException("Limit period must be exactly DAY, WEEK, or MONTH (case-sensitive)");
        }

        return true;
    }

    /**
     * Convenience method to validate all limit creation fields at once.
     *
     * @param amountLimit Limit amount to validate
     * @param period Limit period to validate
     * @return true if all fields are valid
     * @throws IllegalArgumentException if any field is invalid
     */
    public static boolean validateLimit(double amountLimit, String period) {
        isValidAmountLimit(amountLimit);
        isValidPeriod(period);
        return true;
    }
}

