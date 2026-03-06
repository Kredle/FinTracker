package com.example.fintracker.bll.validators;

public class TransactionValidator {

    public static boolean isValidAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than 0");
        }
        return true;
    }

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

    public static boolean validateTransaction(double amount, String title) {
        isValidAmount(amount);
        isValidTitle(title);
        return true;
    }
}

