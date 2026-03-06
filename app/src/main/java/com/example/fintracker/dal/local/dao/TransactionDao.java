package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fintracker.dal.local.entities.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insertTransaction(@NonNull TransactionEntity transaction);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY timestamp DESC")
    List<TransactionEntity> getTransactionsByAccountId(@NonNull String accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 AND (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') ORDER BY timestamp DESC")
    List<TransactionEntity> searchTransactions(@NonNull String accountId, @NonNull String searchQuery);

    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :transactionId")
    void deleteTransaction(@NonNull String transactionId);
}