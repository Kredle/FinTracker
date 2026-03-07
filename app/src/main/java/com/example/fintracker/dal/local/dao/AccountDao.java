package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fintracker.dal.local.entities.AccountEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for Account entity.
 * Provides database operations for account management including creation, retrieval, updates, and deletion.
 * Sync queries include soft-deleted accounts to propagate deletions to the cloud.
 */
@Dao
public interface AccountDao {

    /**
     * Inserts a new account into the database.
     * This is used when creating a new account (e.g., "Card", "Cash").
     *
     * @param account The AccountEntity to insert
     */
    @Insert
    void insertAccount(@NonNull AccountEntity account);

    /**
     * Retrieves all accounts owned by a specific user.
     * Used to display a user's accounts in the UI.
     *
     * @param ownerId The owner's user ID (UUID)
     * @return LiveData that emits the list of AccountEntity objects for the user; emits an empty list if none exist
     */
    @Query("SELECT * FROM accounts WHERE ownerId = :ownerId AND isDeleted = 0 ORDER BY name ASC")
    LiveData<List<AccountEntity>> getAccountsByUserId(@NonNull String ownerId);

    @Query("SELECT * FROM accounts WHERE ownerId = :ownerId AND isDeleted = 0 ORDER BY name ASC")
    List<AccountEntity> getAccountsByUserIdSync(@NonNull String ownerId);

    /**
     * Retrieves a specific account by its ID.
     * Useful for loading account details or updating balance.
     *
     * @param accountId The account's unique identifier (UUID)
     * @return LiveData that emits the AccountEntity if found, or null if the account doesn't exist
     */
    @Query("SELECT * FROM accounts WHERE id = :accountId AND isDeleted = 0 LIMIT 1")
    LiveData<@Nullable AccountEntity> getAccountById(@NonNull String accountId);

    @Query("SELECT * FROM accounts WHERE id = :accountId AND isDeleted = 0 LIMIT 1")
    @Nullable
    AccountEntity getAccountByIdSync(@NonNull String accountId);

    /**
     * Retrieves a specific account by name and owner.
     * Useful for checking if an account already exists before insertion (idempotent operations).
     *
     * @param name The account name
     * @param ownerId The owner's user ID
     * @return LiveData that emits the AccountEntity if found, or null if the account doesn't exist
     */
    @Query("SELECT * FROM accounts WHERE name = :name AND ownerId = :ownerId AND isDeleted = 0 LIMIT 1")
    LiveData<@Nullable AccountEntity> getAccountByNameAndOwner(@NonNull String name, @NonNull String ownerId);

    @Query("SELECT * FROM accounts WHERE name = :name AND ownerId = :ownerId AND isDeleted = 0 LIMIT 1")
    @Nullable
    AccountEntity getAccountByNameAndOwnerSync(@NonNull String name, @NonNull String ownerId);

    /**
     * Updates the balance of a specific account.
     * Typically called after a transaction is added to an account.
     * Only updates non-deleted accounts (isDeleted = 0).
     *
     * @param accountId The account's ID
     * @param newBalance The new balance amount
     * @return The number of rows affected (0 if account doesn't exist or is deleted, 1 if updated)
     */
    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :accountId AND isDeleted = 0")
    int updateAccountBalance(@NonNull String accountId, double newBalance);

    /**
     * Updates an entire account entity.
     * Can be used for updating multiple fields at once.
     *
     * @param account The AccountEntity with updated values
     */
    @Update
    void updateAccount(@NonNull AccountEntity account);

    /**
     * Soft-deletes an account by setting isDeleted flag to true.
     * This preserves data for sync operations while hiding the account from views.
     *
     * @param accountId The account's ID to soft-delete
     */
    @Query("UPDATE accounts SET isDeleted = 1 WHERE id = :accountId")
    void deleteAccount(@NonNull String accountId);

    /**
     * Hard-deletes an account from the database.
     * Use with caution - data is permanently removed.
     *
     * @param account The AccountEntity to permanently delete
     */
    @Delete
    void hardDeleteAccount(@NonNull AccountEntity account);

    /**
     * Retrieves all accounts for a user, including deleted ones.
     * Used for UI display that needs to show historical/deleted accounts.
     *
     * @param ownerId The owner's user ID
     * @return LiveData that emits a list of all AccountEntity objects for the user (including soft-deleted)
     */
    @Query("SELECT * FROM accounts WHERE ownerId = :ownerId")
    LiveData<List<AccountEntity>> getAllAccountsByUserIdIncludingDeleted(@NonNull String ownerId);

    /**
     * Retrieves all accounts that need to be synced.
     * Accounts with isSynced = false are candidates for cloud sync.
     *
     * @return List of unsynchronized AccountEntity objects
     */
    @Query("SELECT * FROM accounts WHERE isSynced = 0")
    List<AccountEntity> getUnsyncedAccounts();
}
