package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.dal.local.dao.SharedAccountMemberDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.SharedAccountMemberEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Repository layer for shared account member-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the SharedAccountMemberDao.
 */
public class SharedAccountMemberRepository {

    private final SharedAccountMemberDao sharedAccountMemberDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;
    private final boolean ownsExecutor;

    public SharedAccountMemberRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).sharedAccountMemberDao(),
                RepositoryExecutors.db(),
                RepositoryExecutors.mainThread(),
                false
        );
    }

    public SharedAccountMemberRepository(
            @NonNull SharedAccountMemberDao sharedAccountMemberDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this(sharedAccountMemberDao, executorService, callbackExecutor, true);
    }

    private SharedAccountMemberRepository(
            @NonNull SharedAccountMemberDao sharedAccountMemberDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor,
            boolean ownsExecutor
    ) {
        this.sharedAccountMemberDao = sharedAccountMemberDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
        this.ownsExecutor = ownsExecutor;
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

    public LiveData<List<SharedAccountMemberEntity>> getMembersForAccount(@NonNull String accountId) {
        return sharedAccountMemberDao.getMembersForAccount(accountId);
    }

    public LiveData<@Nullable SharedAccountMemberEntity> getMember(@NonNull String accountId, @NonNull String userId) {
        return sharedAccountMemberDao.getMember(accountId, userId);
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
        if (ownsExecutor) {
            executorService.shutdown();
        }
    }
}
