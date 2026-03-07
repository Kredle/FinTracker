package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fintracker.dal.local.entities.TagEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for Tag entity.
 * Provides database operations for tag management including creation, retrieval, updates, and deletion.
 * Tags can be user-created or system default tags (where ownerId is null).
 * Sync queries include soft-deleted tags to propagate deletions to the cloud.
 */
@Dao
public interface TagDao {

    /**
     * Inserts a new tag into the database.
     * This is used when creating a custom expense tag (e.g., "Groceries", "Entertainment").
     *
     * @param tag The TagEntity to insert
     */
    @Insert
    void insertTag(@NonNull TagEntity tag);

    /**
     * Retrieves all custom tags owned by a specific user.
     * Used to display a user's custom expense tags in the UI.
     *
     * @param ownerId The owner's user ID (UUID)
     * @return List of TagEntity objects owned by the user, empty list if none exist
     */
    @Query("SELECT * FROM tags WHERE ownerId = :ownerId AND isDeleted = 0 ORDER BY name ASC")
    LiveData<List<TagEntity>> getTagsByUserId(@NonNull String ownerId);

    @Query("SELECT * FROM tags WHERE ownerId = :ownerId AND isDeleted = 0 ORDER BY name ASC")
    List<TagEntity> getTagsByUserIdSync(@NonNull String ownerId);

    /**
     * Retrieves system default tags (tags not owned by any user).
     * Default tags have ownerId = null or ownerId = '' (empty string) and are available to all users.
     * Typically includes predefined categories like "Food", "Transport", "Entertainment", etc.
     *
     * @return List of default TagEntity objects
     */
    @Query("SELECT * FROM tags WHERE (ownerId IS NULL OR ownerId = '') AND isDeleted = 0 ORDER BY name ASC")
    LiveData<List<TagEntity>> getDefaultTags();

    @Query("SELECT * FROM tags WHERE (ownerId IS NULL OR ownerId = '') AND isDeleted = 0 ORDER BY name ASC")
    List<TagEntity> getDefaultTagsSync();

    /**
     * Retrieves a specific tag by its ID.
     * Useful for loading tag details or validation.
     *
     * @param tagId The tag's unique identifier (UUID)
     * @return TagEntity if found, null otherwise
     */
    @Query("SELECT * FROM tags WHERE id = :tagId AND isDeleted = 0 LIMIT 1")
    LiveData<TagEntity> getTagById(@NonNull String tagId);

    @Query("SELECT * FROM tags WHERE id = :tagId AND isDeleted = 0 LIMIT 1")
    @Nullable
    TagEntity getTagByIdSync(@NonNull String tagId);

    /**
     * Retrieves a specific tag by name and owner.
     * Useful for checking if a tag already exists before insertion (idempotent operations).
     *
     * @param name The tag name
     * @param ownerId The owner's user ID
     * @return TagEntity if found, null otherwise
     */
    @Query("SELECT * FROM tags WHERE name = :name AND ownerId = :ownerId AND isDeleted = 0 LIMIT 1")
    LiveData<TagEntity> getTagByNameAndOwner(@NonNull String name, @NonNull String ownerId);

    @Query("SELECT * FROM tags WHERE name = :name AND ownerId = :ownerId AND isDeleted = 0 LIMIT 1")
    @Nullable
    TagEntity getTagByNameAndOwnerSync(@NonNull String name, @NonNull String ownerId);

    /**
     * Retrieves both user-created and default tags for display in the UI.
     * Combines tags owned by the user with system default tags.
     *
     * @param ownerId The user's ID
     * @return List of TagEntity objects (user tags + default tags)
     */
    @Query("SELECT * FROM tags WHERE (ownerId = :ownerId OR ownerId IS NULL OR ownerId = '') AND isDeleted = 0 ORDER BY name ASC")
    LiveData<List<TagEntity>> getAllAvailableTags(@NonNull String ownerId);

    @Query("SELECT * FROM tags WHERE (ownerId = :ownerId OR ownerId IS NULL OR ownerId = '') AND isDeleted = 0 ORDER BY name ASC")
    List<TagEntity> getAllAvailableTagsSync(@NonNull String ownerId);

    /**
     * Updates an entire tag entity.
     * Can be used for updating tag properties (name, icon, etc.).
     *
     * @param tag The TagEntity with updated values
     */
    @Update
    void updateTag(@NonNull TagEntity tag);

    /**
     * Soft-deletes a tag by setting isDeleted flag to true.
     * This preserves data for sync operations while hiding the tag from views.
     *
     * @param tagId The tag's ID to soft-delete
     */
    @Query("UPDATE tags SET isDeleted = 1 WHERE id = :tagId")
    void deleteTag(@NonNull String tagId);

    /**
     * Hard-deletes a tag from the database.
     * Use with caution - data is permanently removed.
     *
     * @param tag The TagEntity to permanently delete
     */
    @Delete
    void hardDeleteTag(@NonNull TagEntity tag);

    /**
     * Retrieves all tags that need to be synced.
     * Tags with isSynced = false are candidates for cloud sync.
     *
     * @return List of unsynchronized TagEntity objects
     */
    @Query("SELECT * FROM tags WHERE isSynced = 0")
    List<TagEntity> getUnsyncedTags();
}
