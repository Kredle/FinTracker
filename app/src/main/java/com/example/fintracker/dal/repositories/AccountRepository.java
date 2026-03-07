package com.example.fintracker.dal.repositories;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.fintracker.dal.local.dao.AccountDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository layer for account-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the AccountDao.
 */
public class AccountRepository {

    private final AccountDao accountDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;

    public AccountRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).accountDao(),
                Executors.newSingleThreadExecutor(),
                command -> new Handler(Looper.getMainLooper()).post(command)
        );
    }

    public AccountRepository(
            @NonNull AccountDao accountDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this.accountDao = accountDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
    }

    public void insertAccount(@NonNull final AccountEntity account) {
        executorService.execute(() -> accountDao.insertAccount(account));
    }

    public void getAccountsByUserId(
            @NonNull final String ownerId,
            @NonNull final DataCallback<List<AccountEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<AccountEntity> result = accountDao.getAccountsByUserId(ownerId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getAccountById(
            @NonNull final String accountId,
            @NonNull final DataCallback<AccountEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final AccountEntity result = accountDao.getAccountById(accountId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getAccountByNameAndOwner(
            @NonNull final String name,
            @NonNull final String ownerId,
            @NonNull final DataCallback<AccountEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final AccountEntity result = accountDao.getAccountByNameAndOwner(name, ownerId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
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

    public void getAllAccountsByUserIdIncludingDeleted(
            @NonNull final String ownerId,
            @NonNull final DataCallback<List<AccountEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<AccountEntity> result = accountDao.getAllAccountsByUserIdIncludingDeleted(ownerId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getUnsyncedAccounts(@NonNull final DataCallback<List<AccountEntity>> callback) {
        executorService.execute(() -> {
            try {
                final List<AccountEntity> result = accountDao.getUnsyncedAccounts();
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
