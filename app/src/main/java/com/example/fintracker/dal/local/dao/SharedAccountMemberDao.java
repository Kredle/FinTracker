package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fintracker.dal.local.entities.SharedAccountMemberEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for SharedAccountMember entity.
 * Provides database operations for shared account membership management including adding members,
 * retrieving members, updating roles, and removing members (soft-delete).
 * All queries filter out soft-deleted members (isDeleted = 0), except sync queries which include
 * soft-deleted rows to propagate deletions to the cloud.
 */
@Dao
public interface SharedAccountMemberDao {

    /**
     * Inserts a new member into a shared account.
     * This is used when adding a user to a shared account with a specific role (ADMIN or USER).
     *
     * @param member The SharedAccountMemberEntity to insert
     */
    @Insert
    void addMember(@NonNull SharedAccountMemberEntity member);

    /**
     * Retrieves all non-deleted members for a specific shared account.
     * Used to display the list of users who have access to the account.
     *
     * @param accountId The account's unique identifier (UUID)
     * @return LiveData list of SharedAccountMemberEntity objects for the account
     */
    @Query("SELECT * FROM shared_account_members WHERE accountId = :accountId AND isDeleted = 0")
    LiveData<List<SharedAccountMemberEntity>> getMembersForAccount(@NonNull String accountId);

    @Query("SELECT * FROM shared_account_members WHERE accountId = :accountId AND isDeleted = 0")
    List<SharedAccountMemberEntity> getMembersForAccountSync(@NonNull String accountId);

    /**
     * Retrieves a specific active membership by account and user.
     * Used to check whether a user currently has access and what role they hold.
     *
     * @param accountId The account's unique identifier (UUID)
     * @param userId The user's unique identifier (UUID)
     * @return LiveData that emits the SharedAccountMemberEntity if found, or null if the user is not an active member
     */
    @Query("SELECT * FROM shared_account_members WHERE accountId = :accountId AND userId = :userId AND isDeleted = 0 LIMIT 1")
    LiveData<SharedAccountMemberEntity> getMember(@NonNull String accountId, @NonNull String userId);

    @Query("SELECT * FROM shared_account_members WHERE accountId = :accountId AND userId = :userId AND isDeleted = 0 LIMIT 1")
    @Nullable
    SharedAccountMemberEntity getMemberSync(@NonNull String accountId, @NonNull String userId);

    /**
     * Updates the role of a specific member in a shared account.
     * Only updates non-deleted members (isDeleted = 0).
     * Typically used when promoting a USER to ADMIN or demoting an ADMIN to USER.
     *
     * @param accountId The account's unique identifier (UUID)
     * @param userId The user's unique identifier (UUID)
     * @param newRole The new role (must be "ADMIN" or "USER")
     * @return The number of rows affected (0 if member doesn't exist or is deleted, 1 if updated)
     */
    @Query("UPDATE shared_account_members SET role = :newRole WHERE accountId = :accountId AND userId = :userId AND isDeleted = 0")
    int updateMemberRole(@NonNull String accountId, @NonNull String userId, @NonNull String newRole);

    /**
     * Soft-deletes an active member from a shared account by setting isDeleted flag to true.
     * This preserves data for sync operations while removing the user's access.
     * Used when an ADMIN removes a user from the shared account.
     *
     * @param accountId The account's unique identifier (UUID)
     * @param userId The user's unique identifier (UUID)
     * @return The number of rows affected (0 if member doesn't exist or is already deleted, 1 if soft-deleted)
     */
    @Query("UPDATE shared_account_members SET isDeleted = 1 WHERE accountId = :accountId AND userId = :userId AND isDeleted = 0")
    int removeMember(@NonNull String accountId, @NonNull String userId);

    /**
     * Retrieves all unsynced shared account members (isSynced = false).
     * Used for Firebase synchronization.
     *
     * @return List of unsynced SharedAccountMemberEntity objects
     */
    @Query("SELECT * FROM shared_account_members WHERE isSynced = 0")
    List<SharedAccountMemberEntity> getUnsyncedSharedMembers();

    /**
     * Updates an existing shared account member entity.
     * Used to mark members as synced after Firebase upload.
     *
     * @param member The SharedAccountMemberEntity to update
     */
    @Update
    void updateSharedAccountMember(@NonNull SharedAccountMemberEntity member);
}
