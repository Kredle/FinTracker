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

/**
 * Data Access Object (DAO) for Transaction entity.
 */
@Dao
public interface TransactionDao {

    @Insert
    void insertTransaction(@NonNull TransactionEntity transaction);

    @Update
    void updateTransaction(@NonNull TransactionEntity transaction);

    // ── Базовые запросы ──────────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getTransactionsByAccountId(@NonNull String accountId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY timestamp DESC")
    List<TransactionEntity> getTransactionsByAccountIdSync(@NonNull String accountId);

    @Query("SELECT * FROM transactions WHERE id = :transactionId AND isDeleted = 0 LIMIT 1")
    TransactionEntity getTransactionByIdSync(@NonNull String transactionId);

    // ── Поиск по тексту (оставлен для обратной совместимости) ────

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 " +
            "AND (title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%') " +
            "ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> searchTransactions(@NonNull String accountId, @NonNull String q);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 " +
            "AND (title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%') " +
            "ORDER BY timestamp DESC")
    List<TransactionEntity> searchTransactionsSync(@NonNull String accountId, @NonNull String q);

    // ── Динамический запрос с любой комбинацией фильтров ─────────

    /**
     * Выполняет произвольный SQL-запрос, сформированный в TransactionService.
     * Используется для комбинации фильтров (accountId, tagId, type, dateFrom, dateTo, searchQuery).
     */
    @RawQuery(observedEntities = TransactionEntity.class)
    LiveData<List<TransactionEntity>> getFilteredTransactions(SupportSQLiteQuery query);

    /**
     * Синхронная версия динамического запроса — для фоновых операций и тестов.
     */
    @RawQuery
    List<TransactionEntity> getFilteredTransactionsSync(SupportSQLiteQuery query);

    // ── Удаление ─────────────────────────────────────────────────

    /**
     * Soft-delete транзакции: isDeleted=1, isSynced=0.
     * updatedAt проставляется через datetime('now') прямо в SQL —
     * сигнатура остаётся однопараметровой для совместимости с TransactionRepository.
     */
    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0, updatedAt = datetime('now') WHERE id = :transactionId")
    void deleteTransaction(@NonNull String transactionId);

    // ── Синхронизация ─────────────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    List<TransactionEntity> getUnsyncedTransactions();
}