package com.example.fintracker.dal.repositories;

import androidx.annotation.NonNull;

/**
 * Generic callback interface for asynchronous repository data operations.
 *
 * @param <T> The result type
 */
public interface DataCallback<T> {

    /**
     * Called when data operation completes successfully.
     *
     * @param data The operation result
     */
    void onSuccess(T data);

    /**
     * Called when data operation fails.
     *
     * @param throwable The error that occurred
     */
    void onError(@NonNull Throwable throwable);
}

