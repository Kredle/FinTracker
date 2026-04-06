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
 *
 * Поддерживаемые типы лимитов:
 *   1. Общий лимит на счёт          → accountId + userId IS NULL + tagId IS NULL
 *   2. Лимит на человека в счёте    → accountId + userId = X   + tagId IS NULL
 *   3. Общий лимит на тег в счёте   → accountId + userId IS NULL + tagId = X
 *   4. Лимит на тег на человека     → accountId + userId = X   + tagId = X
 */
@Dao
public interface LimitDao {

    @Insert
    void insertLimit(@NonNull LimitEntity limit);

    @Update
    void updateLimit(@NonNull LimitEntity limit);

    // ── 1. Общий лимит на счёт (userId IS NULL, tagId IS NULL) ──

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId IS NULL AND tagId IS NULL AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    LiveData<LimitEntity> getAccountWideLimit(@NonNull String accountId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId IS NULL AND tagId IS NULL AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getAccountWideLimitSync(@NonNull String accountId);

    // ── 2. Лимит на конкретного пользователя в счёте (tagId IS NULL) ──

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId = :userId AND tagId IS NULL AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    LiveData<LimitEntity> getUserLimitInAccount(@NonNull String accountId, @NonNull String userId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId = :userId AND tagId IS NULL AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getUserLimitInAccountSync(@NonNull String accountId, @NonNull String userId);

    // ── 3. Общий лимит на тег в счёте (userId IS NULL) ──

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId IS NULL AND tagId = :tagId AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    LiveData<LimitEntity> getTagWideLimit(@NonNull String accountId, @NonNull String tagId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId IS NULL AND tagId = :tagId AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getTagWideLimitSync(@NonNull String accountId, @NonNull String tagId);

    // ── 4. Лимит на тег для конкретного пользователя ──

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId = :userId AND tagId = :tagId AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    LiveData<LimitEntity> getUserTagLimit(@NonNull String accountId,
                                          @NonNull String userId,
                                          @NonNull String tagId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId " +
            "AND userId = :userId AND tagId = :tagId AND isDeleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getUserTagLimitSync(@NonNull String accountId,
                                    @NonNull String userId,
                                    @NonNull String tagId);

    // ── Все лимиты счёта ──

    @Query("SELECT * FROM limits WHERE accountId = :accountId AND isDeleted = 0")
    LiveData<List<LimitEntity>> getLimitsByAccountId(@NonNull String accountId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId AND isDeleted = 0")
    List<LimitEntity> getLimitsByAccountIdSync(@NonNull String accountId);

    @Query("SELECT * FROM limits WHERE id = :limitId LIMIT 1")
    @Nullable
    LimitEntity getLimitByIdSync(@NonNull String limitId);

    // ── Синхронизация ──

    @Query("SELECT * FROM limits WHERE isSynced = 0")
    List<LimitEntity> getUnsyncedLimits();

    // ── Устаревшие методы (оставлены для обратной совместимости) ──

    /** @deprecated Используй {@link #getAccountWideLimit(String)} */
    @Deprecated
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId IS NULL AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    LiveData<LimitEntity> getAccountWideLimitByAccountId(@NonNull String accountId);

    /** @deprecated Используй {@link #getAccountWideLimitSync(String)} */
    @Deprecated
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId IS NULL AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getAccountWideLimitByAccountIdSync(@NonNull String accountId);

    /** @deprecated Используй {@link #getTagWideLimit(String, String)} */
    @Deprecated
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId = :tagId AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    LiveData<LimitEntity> getLimitByAccountAndTag(@NonNull String accountId, @NonNull String tagId);

    /** @deprecated Используй {@link #getTagWideLimitSync(String, String)} */
    @Deprecated
    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId = :tagId AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    @Nullable
    LimitEntity getLimitByAccountAndTagSync(@NonNull String accountId, @NonNull String tagId);

    @Query("SELECT * FROM limits WHERE accountId IS NULL AND userId = :userId AND tagId IS NOT NULL AND isDeleted = 0")
    List<LimitEntity> getCategoryLimitsByUserIdSync(@NonNull String userId);

    @Query("UPDATE limits SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    void deleteLimit(@NonNull String id, @NonNull String updatedAt);
}