package com.example.fintracker.dal.repositories;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.dao.SharedAccountMemberDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.SharedAccountMemberEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository layer for shared account member-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the SharedAccountMemberDao.
 */
public class SharedAccountMemberRepository {

    private final SharedAccountMemberDao sharedAccountMemberDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;

    public SharedAccountMemberRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).sharedAccountMemberDao(),
                Executors.newSingleThreadExecutor(),
                createMainThreadExecutor()
        );
    }

    private static Executor createMainThreadExecutor() {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        return mainHandler::post;
    }

    public SharedAccountMemberRepository(
            @NonNull SharedAccountMemberDao sharedAccountMemberDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this.sharedAccountMemberDao = sharedAccountMemberDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
    }

    public void addMember(@NonNull final SharedAccountMemberEntity member) {
        addMember(member, null);
    }

    public void addMember(
            @NonNull final SharedAccountMemberEntity member,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                sharedAccountMemberDao.addMember(member);
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

    public void getMembersForAccount(
            @NonNull final String accountId,
            @NonNull final DataCallback<List<SharedAccountMemberEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<SharedAccountMemberEntity> result = sharedAccountMemberDao.getMembersForAccount(accountId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getMember(
            @NonNull final String accountId,
            @NonNull final String userId,
            @NonNull final DataCallback<SharedAccountMemberEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final SharedAccountMemberEntity result = sharedAccountMemberDao.getMember(accountId, userId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void updateMemberRole(
            @NonNull final String accountId,
            @NonNull final String userId,
            @NonNull final String newRole,
            @NonNull final DataCallback<Integer> callback
    ) {
        executorService.execute(() -> {
            try {
                final int rowsAffected = sharedAccountMemberDao.updateMemberRole(accountId, userId, newRole);
                callbackExecutor.execute(() -> callback.onSuccess(rowsAffected));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void removeMember(
            @NonNull final String accountId,
            @NonNull final String userId,
            @NonNull final DataCallback<Integer> callback
    ) {
        executorService.execute(() -> {
            try {
                final int rowsAffected = sharedAccountMemberDao.removeMember(accountId, userId);
                callbackExecutor.execute(() -> callback.onSuccess(rowsAffected));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

