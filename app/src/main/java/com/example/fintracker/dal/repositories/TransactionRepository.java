package com.example.fintracker.dal.repositories;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.fintracker.dal.local.dao.TransactionDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.TransactionEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository layer for transaction-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the TransactionDao.
 */
public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;

    public TransactionRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).transactionDao(),
                Executors.newSingleThreadExecutor(),
                command -> new Handler(Looper.getMainLooper()).post(command)
        );
    }

    public TransactionRepository(
            @NonNull TransactionDao transactionDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this.transactionDao = transactionDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
    }

    public void insertTransaction(@NonNull final TransactionEntity transaction) {
        executorService.execute(() -> transactionDao.insertTransaction(transaction));
    }

    public void getTransactionsByAccountId(
            @NonNull final String accountId,
            @NonNull final DataCallback<List<TransactionEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<TransactionEntity> result = transactionDao.getTransactionsByAccountId(accountId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void searchTransactions(
            @NonNull final String accountId,
            @NonNull final String searchQuery,
            @NonNull final DataCallback<List<TransactionEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<TransactionEntity> result = transactionDao.searchTransactions(accountId, searchQuery);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void deleteTransaction(@NonNull final String transactionId) {
        executorService.execute(() -> transactionDao.deleteTransaction(transactionId));
    }

    public void shutdown() {
        executorService.shutdown();
    }
}