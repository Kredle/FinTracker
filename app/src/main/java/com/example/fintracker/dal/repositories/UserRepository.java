package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Repository layer for user-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the UserDao.
 */
public class UserRepository {

    private final UserDao userDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;
    private final boolean ownsExecutor;

    public UserRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).userDao(),
                RepositoryExecutors.db(),
                RepositoryExecutors.mainThread(),
                false
        );
    }

    public UserRepository(
            @NonNull UserDao userDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this(userDao, executorService, callbackExecutor, true);
    }

    private UserRepository(
            @NonNull UserDao userDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor,
            boolean ownsExecutor
    ) {
        this.userDao = userDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
        this.ownsExecutor = ownsExecutor;
    }

    public void insertUser(@NonNull final UserEntity user) {
        insertUser(user, null);
    }

    public void insertUser(
            @NonNull final UserEntity user,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                userDao.insertUser(user);
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

    public void getUserByEmailOrName(
            @NonNull final String login,
            @NonNull final String password,
            @NonNull final DataCallback<UserEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final UserEntity result = userDao.getUserByEmailOrName(login, password);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void checkIfUserExists(
            @NonNull final String email,
            @NonNull final String username,
            @NonNull final DataCallback<Boolean> callback
    ) {
        executorService.execute(() -> {
            try {
                final boolean result = userDao.checkIfUserExists(email, username);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getUserById(
            @NonNull final String userId,
            @NonNull final DataCallback<UserEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final UserEntity result = userDao.getUserById(userId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getUserByEmail(
            @NonNull final String email,
            @NonNull final DataCallback<UserEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final UserEntity result = userDao.getUserByEmail(email);
                callbackExecutor.execute(() -> callback.onSuccess(result));
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
