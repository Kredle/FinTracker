package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.dao.TransactionDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.TransactionEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Repository layer for transaction-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the TransactionDao.
 */
public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;
    private final boolean ownsExecutor;

    public TransactionRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).transactionDao(),
                RepositoryExecutors.db(),
                RepositoryExecutors.mainThread(),
                false
        );
    }

    public TransactionRepository(
            @NonNull TransactionDao transactionDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this(transactionDao, executorService, callbackExecutor, true);
    }

    private TransactionRepository(
            @NonNull TransactionDao transactionDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor,
            boolean ownsExecutor
    ) {
        this.transactionDao = transactionDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
        this.ownsExecutor = ownsExecutor;
    }

    public void insertTransaction(@NonNull final TransactionEntity transaction) {
        insertTransaction(transaction, null);
    }

    public void insertTransaction(
            @NonNull final TransactionEntity transaction,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                transactionDao.insertTransaction(transaction);
                if (callback != null) {
                    callbackExecutor.execute(() -> callback.onSuccess(null));
                }
            } catch (Exception e) {
                if (callback != null) {
                    callbackExecutor.execute(() -> callback.onError(e));
                }
            }
        });
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
        deleteTransaction(transactionId, null);
    }

    public void deleteTransaction(
            @NonNull final String transactionId,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                transactionDao.deleteTransaction(transactionId);
                if (callback != null) {
                    callbackExecutor.execute(() -> callback.onSuccess(null));
                }
            } catch (Exception e) {
                if (callback != null) {
                    callbackExecutor.execute(() -> callback.onError(e));
                }
            }
        });
    }

    public void shutdown() {
        if (ownsExecutor) {
            executorService.shutdown();
        }
    }
}