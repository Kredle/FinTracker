package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.fintracker.dal.local.entities.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insertTransaction(@NonNull TransactionEntity transaction);

    @Update
    void updateTransaction(@NonNull TransactionEntity transaction);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getTransactionsByAccountId(@NonNull String accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY timestamp DESC")
    List<TransactionEntity> getTransactionsByAccountIdSync(@NonNull String accountId);

    @Query("SELECT * FROM transactions WHERE id = :transactionId AND isDeleted = 0 LIMIT 1")
    TransactionEntity getTransactionByIdSync(@NonNull String transactionId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 " +
            "AND (title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%') " +
            "ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> searchTransactions(@NonNull String accountId, @NonNull String q);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 " +
            "AND (title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%') " +
            "ORDER BY timestamp DESC")
    List<TransactionEntity> searchTransactionsSync(@NonNull String accountId, @NonNull String q);

    @RawQuery(observedEntities = TransactionEntity.class)
    LiveData<List<TransactionEntity>> getFilteredTransactions(SupportSQLiteQuery query);

    @RawQuery
    List<TransactionEntity> getFilteredTransactionsSync(SupportSQLiteQuery query);

    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0, updatedAt = datetime('now') WHERE id = :transactionId")
    void deleteTransaction(@NonNull String transactionId);

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE bankMessageHash = :hash AND isDeleted = 0 LIMIT 1)")
    boolean existsByBankMessageHash(@NonNull String hash);

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    List<TransactionEntity> getUnsyncedTransactions();

    @Query("SELECT * FROM transactions WHERE accountId IN (:accountIds) AND isDeleted = 0 ORDER BY timestamp DESC")
    List<TransactionEntity> getTransactionsByAccountIdsSync(@NonNull List<String> accountIds);
}

