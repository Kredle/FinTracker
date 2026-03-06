package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fintracker.dal.local.entities.TransactionEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for Transaction entity.
 * Provides database operations for transaction management including insertion, retrieval, search, and soft-deletion.
 * All queries filter out soft-deleted transactions (isDeleted = 0).
 */
@Dao
public interface TransactionDao {

    /**
     * Inserts a new transaction into the database.
     * This is used when recording income or expense transactions.
     *
     * @param transaction The TransactionEntity to insert
     */
    @Insert
    void insertTransaction(@NonNull TransactionEntity transaction);

    /**
     * Retrieves all non-deleted transactions for a specific account.
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param accountId The account's unique identifier (UUID)
     * @return List of TransactionEntity objects for the account, empty list if none exist
     */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY timestamp DESC")
    List<TransactionEntity> getTransactionsByAccountId(@NonNull String accountId);

    /**
     * Searches for transactions by keyword in title or description.
     * Uses SQL LIKE with wildcards to match partial strings in either field.
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param accountId The account's unique identifier (UUID)
     * @param searchQuery The search keyword (will be matched against title and description using LIKE %query%)
     * @return List of matching TransactionEntity objects, empty list if none match
     */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 AND (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') ORDER BY timestamp DESC")
    List<TransactionEntity> searchTransactions(@NonNull String accountId, @NonNull String searchQuery);

    /**
     * Soft-deletes a transaction by setting isDeleted flag to true.
     * This preserves data for sync operations while hiding the transaction from views.
     *
     * @param transactionId The transaction's ID to soft-delete
     */
    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :transactionId")
    void deleteTransaction(@NonNull String transactionId);
}