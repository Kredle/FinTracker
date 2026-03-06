package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fintracker.dal.local.entities.LimitEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for Limit entity.
 * Provides database operations for spending limit management including insertion and retrieval.
 * All queries filter out soft-deleted limits (isDeleted = 0).
 *
 * Note: The current implementation allows multiple active limits per (accountId, tagId) pair.
 * If business rules require uniqueness, consider adding a UNIQUE constraint at the schema level.
 */
@Dao
public interface LimitDao {

    /**
     * Inserts a new spending limit into the database.
     * This is used when creating account-level or tag-specific spending limits.
     *
     * @param limit The LimitEntity to insert
     */
    @Insert
    void insertLimit(@NonNull LimitEntity limit);

    /**
     * Retrieves all non-deleted limits for a specific account.
     * This includes both account-wide limits and tag-specific limits.
     *
     * @param accountId The account's unique identifier (UUID)
     * @return List of LimitEntity objects for the account, empty list if none exist
     */
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND isDeleted = 0")
    List<LimitEntity> getLimitsByAccountId(@NonNull String accountId);

    /**
     * Retrieves a specific limit by account and tag.
     * If multiple non-deleted limits exist for the same (accountId, tagId), returns the most recently updated one.
     * This deterministic ordering ensures consistent results.
     *
     * @param accountId The account's unique identifier (UUID)
     * @param tagId The tag's unique identifier (UUID)
     * @return LimitEntity if found, null otherwise
     */
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId = :tagId AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getLimitByAccountAndTag(@NonNull String accountId, @NonNull String tagId);
}

