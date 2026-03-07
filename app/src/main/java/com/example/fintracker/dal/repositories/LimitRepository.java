package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.dal.local.dao.LimitDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.LimitEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Repository layer for limit-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the LimitDao.
 */
public class LimitRepository {

    private final LimitDao limitDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;
    private final boolean ownsExecutor;

    public LimitRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).limitDao(),
                RepositoryExecutors.db(),
                RepositoryExecutors.mainThread(),
                false
        );
    }

    public LimitRepository(
            @NonNull LimitDao limitDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this(limitDao, executorService, callbackExecutor, true);
    }

    private LimitRepository(
            @NonNull LimitDao limitDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor,
            boolean ownsExecutor
    ) {
        this.limitDao = limitDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
        this.ownsExecutor = ownsExecutor;
    }

    public void insertLimit(@NonNull final LimitEntity limit) {
        insertLimit(limit, null);
    }

    public void insertLimit(
            @NonNull final LimitEntity limit,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                limitDao.insertLimit(limit);
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

    public LiveData<List<LimitEntity>> getLimitsByAccountId(@NonNull String accountId) {
        return limitDao.getLimitsByAccountId(accountId);
    }

    public LiveData<LimitEntity> getAccountWideLimitByAccountId(@NonNull String accountId) {
        return limitDao.getAccountWideLimitByAccountId(accountId);
    }

    public LiveData<LimitEntity> getLimitByAccountAndTag(@NonNull String accountId, @NonNull String tagId) {
        return limitDao.getLimitByAccountAndTag(accountId, tagId);
    }

    public void shutdown() {
        if (ownsExecutor) {
            executorService.shutdown();
        }
    }
}
