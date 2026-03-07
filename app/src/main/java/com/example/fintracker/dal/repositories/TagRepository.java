package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.dal.local.dao.TagDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.TagEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Repository layer for tag-related local database operations.
 * Acts as a mediator between ViewModels/use-cases and the TagDao.
 */
public class TagRepository {

    private final TagDao tagDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;
    private final boolean ownsExecutor;

    public TagRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).tagDao(),
                RepositoryExecutors.db(),
                RepositoryExecutors.mainThread(),
                false
        );
    }

    public TagRepository(
            @NonNull TagDao tagDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this(tagDao, executorService, callbackExecutor, true);
    }

    private TagRepository(
            @NonNull TagDao tagDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor,
            boolean ownsExecutor
    ) {
        this.tagDao = tagDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
        this.ownsExecutor = ownsExecutor;
    }

    public void insertTag(@NonNull final TagEntity tag) {
        insertTag(tag, null);
    }

    public void insertTag(
            @NonNull final TagEntity tag,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                tagDao.insertTag(tag);
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

    public LiveData<List<TagEntity>> getTagsByUserId(@NonNull String ownerId) {
        return tagDao.getTagsByUserId(ownerId);
    }

    public LiveData<List<TagEntity>> getDefaultTags() {
        return tagDao.getDefaultTags();
    }

    public LiveData<TagEntity> getTagById(@NonNull String tagId) {
        return tagDao.getTagById(tagId);
    }

    public LiveData<TagEntity> getTagByNameAndOwner(@NonNull String name, @NonNull String ownerId) {
        return tagDao.getTagByNameAndOwner(name, ownerId);
    }

    public LiveData<List<TagEntity>> getAllAvailableTags(@NonNull String ownerId) {
        return tagDao.getAllAvailableTags(ownerId);
    }

    public void updateTag(@NonNull final TagEntity tag) {
        updateTag(tag, null);
    }

    public void updateTag(
            @NonNull final TagEntity tag,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                tagDao.updateTag(tag);
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

    public void deleteTag(@NonNull final String tagId) {
        deleteTag(tagId, null);
    }

    public void deleteTag(
            @NonNull final String tagId,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                tagDao.deleteTag(tagId);
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
