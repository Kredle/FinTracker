package com.example.fintracker.bll.validators;

import android.util.Patterns;

/**
 * Utility class for validating user registration and login data.
 * Provides static methods for email, username, and password validation.
 * Follows business logic best practices by separating validation concerns.
 */
public class UserValidator {

    /**
     * Validates email format using Android's built-in email pattern.
     *
     * @param email Email address to validate
     * @return true if email is valid
     * @throws IllegalArgumentException if email is null, empty, or invalid format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Електронна пошта не може бути пустою");
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            throw new IllegalArgumentException("Невірний формат електронної пошти. Очікуваний формат: користувач@приклад.com");
        }
        return true;
    }

    /**
     * Validates username format.
     * Requirements: not empty, not null, min 3 characters, max 25 characters (measured on trimmed value).
     * Leading/trailing whitespace is not allowed.
     *
     * @param username Username to validate
     * @return true if username is valid
     * @throws IllegalArgumentException if username is null or violates constraints
     */
    public static boolean isValidUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Ім'я користувача не може бути пустим");
        }

        // Reject usernames with leading/trailing whitespace
        if (!username.equals(username.trim())) {
            throw new IllegalArgumentException("Ім'я користувача не може мати пробіли на початку або наприкінці");
        }

        if (username.isEmpty()) {
            throw new IllegalArgumentException("Ім'я користувача не може бути пустим");
        }

        // Check length constraints on the (already-trimmed) username
        if (username.length() < 3) {
            throw new IllegalArgumentException("Ім'я користувача має бути принаймні 3 символи");
        }
        if (username.length() > 25) {
            throw new IllegalArgumentException("Ім'я користувача не повинно перевищувати 25 символів");
        }
        return true;
    }

    /**
     * Validates password format.
     * Requirements: not empty, min 6 characters.
     *
     * @param password Password to validate
     * @return true if password is valid, false otherwise
     * @throws IllegalArgumentException if password is null or violates constraints
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Пароль не може бути пустим");
        }
        if (password.isEmpty()) {
            throw new IllegalArgumentException("Пароль не може бути пустим");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Пароль має бути принаймні 6 символів");
        }
        return true;
    }

    /**
     * Convenience method to validate all registration fields at once.
     *
     * @param email Email to validate
     * @param username Username to validate
     * @param password Password to validate
     * @return true if all fields are valid
     * @throws IllegalArgumentException if any field is invalid
     */
    public static boolean validateRegistration(String email, String username, String password) {
        isValidEmail(email);
        isValidUsername(username);
        isValidPassword(password);
        return true;
    }
}

