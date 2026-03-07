package com.example.fintracker.dal.repositories;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.dao.LimitDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.LimitEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository layer for limit-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the LimitDao.
 */
public class LimitRepository {

    private final LimitDao limitDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;

    public LimitRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).limitDao(),
                Executors.newSingleThreadExecutor(),
                createMainThreadExecutor()
        );
    }

    private static Executor createMainThreadExecutor() {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        return mainHandler::post;
    }

    public LimitRepository(
            @NonNull LimitDao limitDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this.limitDao = limitDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
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

    public void getLimitsByAccountId(
            @NonNull final String accountId,
            @NonNull final DataCallback<List<LimitEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<LimitEntity> result = limitDao.getLimitsByAccountId(accountId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getAccountWideLimitByAccountId(
            @NonNull final String accountId,
            @NonNull final DataCallback<LimitEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final LimitEntity result = limitDao.getAccountWideLimitByAccountId(accountId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getLimitByAccountAndTag(
            @NonNull final String accountId,
            @NonNull final String tagId,
            @NonNull final DataCallback<LimitEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final LimitEntity result = limitDao.getLimitByAccountAndTag(accountId, tagId);
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

