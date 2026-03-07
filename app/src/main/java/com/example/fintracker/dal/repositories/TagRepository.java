package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public void getTagsByUserId(
            @NonNull final String ownerId,
            @NonNull final DataCallback<List<TagEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<TagEntity> result = tagDao.getTagsByUserId(ownerId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getDefaultTags(@NonNull final DataCallback<List<TagEntity>> callback) {
        executorService.execute(() -> {
            try {
                final List<TagEntity> result = tagDao.getDefaultTags();
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getTagById(
            @NonNull final String tagId,
            @NonNull final DataCallback<TagEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final TagEntity result = tagDao.getTagById(tagId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getTagByNameAndOwner(
            @NonNull final String name,
            @NonNull final String ownerId,
            @NonNull final DataCallback<TagEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                final TagEntity result = tagDao.getTagByNameAndOwner(name, ownerId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public void getAllAvailableTags(
            @NonNull final String ownerId,
            @NonNull final DataCallback<List<TagEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                final List<TagEntity> result = tagDao.getAllAvailableTags(ownerId);
                callbackExecutor.execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
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
