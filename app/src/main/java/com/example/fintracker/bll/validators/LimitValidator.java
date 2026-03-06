package com.example.fintracker.bll.validators;

public class LimitValidator {

    public static boolean isValidAmountLimit(double amountLimit) {
        if (amountLimit <= 0) {
            throw new IllegalArgumentException("Limit amount must be greater than 0");
        }
        return true;
    }

    public static boolean isValidPeriod(String period) {
        if (period == null) {
            throw new IllegalArgumentException("Limit period cannot be null");
        }

        String normalizedPeriod = period.trim().toUpperCase();
        if (!("DAY".equals(normalizedPeriod) || "WEEK".equals(normalizedPeriod) || "MONTH".equals(normalizedPeriod))) {
            throw new IllegalArgumentException("Limit period must be exactly DAY, WEEK, or MONTH");
        }

        return true;
    }

    public static boolean validateLimit(double amountLimit, String period) {
        isValidAmountLimit(amountLimit);
        isValidPeriod(period);
        return true;
    }
}

