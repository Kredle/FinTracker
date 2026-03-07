package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fintracker.dal.local.entities.LimitEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for Limit entity.
 * Provides database operations for spending limit management including insertion and retrieval.
 * All queries filter out soft-deleted limits (isDeleted = 0), except sync queries which include
 * soft-deleted rows to propagate deletions to the cloud.
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
     * @return LiveData object containing a list of LimitEntity objects for the account
     */
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND isDeleted = 0")
    LiveData<List<LimitEntity>> getLimitsByAccountId(@NonNull String accountId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId AND isDeleted = 0")
    List<LimitEntity> getLimitsByAccountIdSync(@NonNull String accountId);

    /**
     * Retrieves the account-wide limit for a specific account (where tagId is NULL).
     * If multiple non-deleted account-wide limits exist, returns the most recently updated one.
     * This deterministic ordering ensures consistent results.
     *
     * @param accountId The account's unique identifier (UUID)
     * @return LiveData that emits the LimitEntity if an account-wide limit exists, or null otherwise
     */
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId IS NULL AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    LiveData<@Nullable LimitEntity> getAccountWideLimitByAccountId(@NonNull String accountId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId IS NULL AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getAccountWideLimitByAccountIdSync(@NonNull String accountId);

    /**
     * Retrieves a specific tag-specific limit by account and tag.
     * This query only returns limits where tagId is NOT NULL (i.e., tag-specific limits).
     * If multiple non-deleted limits exist for the same (accountId, tagId), returns the most recently updated one.
     * This deterministic ordering ensures consistent results.
     *
     * @param accountId The account's unique identifier (UUID)
     * @param tagId The tag's unique identifier (UUID, must not be null)
     * @return LiveData that emits the LimitEntity if found, or null otherwise
     */
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId = :tagId AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    LiveData<@Nullable LimitEntity> getLimitByAccountAndTag(@NonNull String accountId, @NonNull String tagId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId = :tagId AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getLimitByAccountAndTagSync(@NonNull String accountId, @NonNull String tagId);

    /**
     * Retrieves all unsynced limits (isSynced = false).
     * Used for Firebase synchronization.
     *
     * @return List of unsynced LimitEntity objects
     */
    @Query("SELECT * FROM limits WHERE isSynced = 0")
    List<LimitEntity> getUnsyncedLimits();

    /**
     * Updates an existing limit entity.
     * Used to mark limits as synced after Firebase upload.
     *
     * @param limit The LimitEntity to update
     */
    @Update
    void updateLimit(@NonNull LimitEntity limit);
}
