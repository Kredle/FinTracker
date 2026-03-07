package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.dal.local.dao.AccountDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Repository layer for account-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the AccountDao.
 */
public class AccountRepository {

    private final AccountDao accountDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;
    private final boolean ownsExecutor;

    public AccountRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).accountDao(),
                RepositoryExecutors.db(),
                RepositoryExecutors.mainThread(),
                false
        );
    }

    public AccountRepository(
            @NonNull AccountDao accountDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this(accountDao, executorService, callbackExecutor, true);
    }

    private AccountRepository(
            @NonNull AccountDao accountDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor,
            boolean ownsExecutor
    ) {
        this.accountDao = accountDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
        this.ownsExecutor = ownsExecutor;
    }

    public void insertAccount(@NonNull final AccountEntity account) {
        insertAccount(account, null);
    }

    public void insertAccount(
            @NonNull final AccountEntity account,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                accountDao.insertAccount(account);
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

    public LiveData<List<AccountEntity>> getAccountsByUserId(@NonNull String ownerId) {
        return accountDao.getAccountsByUserId(ownerId);
    }

    public LiveData<AccountEntity> getAccountById(@NonNull String accountId) {
        return accountDao.getAccountById(accountId);
    }

    public LiveData<AccountEntity> getAccountByNameAndOwner(@NonNull String name, @NonNull String ownerId) {
        return accountDao.getAccountByNameAndOwner(name, ownerId);
    }

    public void updateAccountBalance(
            @NonNull final String accountId,
            final double newBalance,
            @NonNull final DataCallback<Integer> callback
    ) {
        executorService.execute(() -> {
            try {
                final int rowsAffected = accountDao.updateAccountBalance(accountId, newBalance);
                callbackExecutor.execute(() -> callback.onSuccess(rowsAffected));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void updateAccount(@NonNull final AccountEntity account) {
        executorService.execute(() -> accountDao.updateAccount(account));
    }

    public void deleteAccount(@NonNull final String accountId) {
        executorService.execute(() -> accountDao.deleteAccount(accountId));
    }

    public void hardDeleteAccount(@NonNull final AccountEntity account) {
        executorService.execute(() -> accountDao.hardDeleteAccount(account));
    }

    public LiveData<List<AccountEntity>> getAllAccountsByUserIdIncludingDeleted(@NonNull String ownerId) {
        return accountDao.getAllAccountsByUserIdIncludingDeleted(ownerId);
    }

    public void shutdown() {
        if (ownsExecutor) {
            executorService.shutdown();
        }
    }
}
