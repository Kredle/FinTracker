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

    @RawQuery(observedEntities = TransactionEntity.class)
    LiveData<List<TransactionEntity>> getFilteredTransactions(SupportSQLiteQuery query);

    @RawQuery
    List<TransactionEntity> getFilteredTransactionsSync(SupportSQLiteQuery query);

    // ── Удаление ─────────────────────────────────────────────────

    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0, updatedAt = datetime('now') WHERE id = :transactionId")
    void deleteTransaction(@NonNull String transactionId);

    // ── Дедупликация банковских транзакций ────────────────────────

    /**
     * Проверяет, существует ли транзакция с данным bankMessageHash.
     * Используется в BankNotificationService для защиты от дублирования
     * одного уведомления MonoBank.
     *
     * @param hash SHA-256 хэш текста уведомления
     * @return true если транзакция с таким хэшем уже есть в базе
     */
    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE bankMessageHash = :hash AND isDeleted = 0 LIMIT 1)")
    boolean existsByBankMessageHash(@NonNull String hash);

    // ── Синхронизация ─────────────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    List<TransactionEntity> getUnsyncedTransactions();
}