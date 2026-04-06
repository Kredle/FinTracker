package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.dal.local.dao.TransactionDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.TransactionEntity;

import java.util.ArrayList;
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

    public LiveData<List<TransactionEntity>> getTransactionsByAccountId(@NonNull String accountId) {
        return transactionDao.getTransactionsByAccountId(accountId);
    }

    public LiveData<List<TransactionEntity>> searchTransactions(@NonNull String accountId, @NonNull String searchQuery) {
        return transactionDao.searchTransactions(accountId, searchQuery);
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

    public void getTransactionsByUserId(@NonNull Application application, @NonNull String userId, @NonNull DataCallback<List<TransactionEntity>> callback) {
        executorService.execute(() -> {
            try {
                // First get all account IDs for the user
                List<String> accountIds = AppDatabase.getInstance(application).accountDao().getAccountIdsByUserIdSync(userId);
                if (accountIds.isEmpty()) {
                    callbackExecutor.execute(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }

                // Then get transactions for these accounts
                List<TransactionEntity> transactions = transactionDao.getTransactionsByAccountIdsSync(accountIds);
                callbackExecutor.execute(() -> callback.onSuccess(transactions));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getAccountBalance(@NonNull String accountId, @NonNull DataCallback<Double> callback) {
        executorService.execute(() -> {
            try {
                List<TransactionEntity> transactions = transactionDao.getTransactionsByAccountIdSync(accountId);
                final double[] balance = {0.0};
                for (TransactionEntity transaction : transactions) {
                    if ("INCOME".equals(transaction.type)) {
                        balance[0] += transaction.amount;
                    } else if ("EXPENSE".equals(transaction.type)) {
                        balance[0] -= transaction.amount;
                    }
                }
                callbackExecutor.execute(() -> callback.onSuccess(balance[0]));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void shutdown() {
        if (ownsExecutor) {
            executorService.shutdown();
        }
    }
}