package com.example.fintracker.dal.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic callback interface for asynchronous repository data operations.
 *
 * @param <T> The result type
 */
public interface DataCallback<T> {

    /**
     * Called when data operation completes successfully.
     * Some DAO-backed lookups may return null when no row is found.
     *
     * @param data The operation result, nullable for "not found" lookups
     */
    void onSuccess(@Nullable T data);

    /**
     * Called when data operation fails.
     *
     * @param throwable The error that occurred
     */
    void onError(@NonNull Throwable throwable);
}
