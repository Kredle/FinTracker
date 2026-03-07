package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fintracker.dal.local.entities.UserEntity;

/**
 * Data Access Object (DAO) for User entity.
 * Provides database operations for user registration, login, and existence checks.
 */
@Dao
public interface UserDao {

    /**
     * Inserts a new user into the database.
     * This is used during user registration.
     *
     * @param user The UserEntity to insert
     */
    @Insert
    void insertUser(@NonNull UserEntity user);

    /**
     * Retrieves a user by email or username and password.
     * Used for login functionality - user can login with either email or username.
     *
     * @param login Email or username (username is stored in 'name' field)
     * @param password The user's password hashed
     * @return UserEntity if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE (email = :login OR name = :login) AND password = :password LIMIT 1")
    @Nullable
    UserEntity getUserByEmailOrName(@NonNull String login, @NonNull String password);

    /**
     * Checks if a user with the given email or username already exists.
     * Used during registration to prevent duplicate accounts.
     *
     * @param email Email address to check
     * @param username Username to check
     * @return true if user exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email OR name = :username LIMIT 1)")
    boolean checkIfUserExists(@NonNull String email, @NonNull String username);

    /**
     * Retrieves a user by their ID (UUID).
     * Useful for profile lookups and session management.
     *
     * @param userId The user's unique identifier
     * @return UserEntity if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    @Nullable
    UserEntity getUserById(@NonNull String userId);

    /**
     * Retrieves a user by email address.
     * Useful for email-based lookups.
     *
     * @param email The email address to search for
     * @return UserEntity if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    @Nullable
    UserEntity getUserByEmail(@NonNull String email);
}

