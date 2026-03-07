package com.example.fintracker.dal.repositories;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared executor holders for repository classes.
 */
final class RepositoryExecutors {

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Executor MAIN_THREAD_EXECUTOR = new Handler(Looper.getMainLooper())::post;

    private RepositoryExecutors() {
    }

    static ExecutorService db() {
        return DB_EXECUTOR;
    }

    static Executor mainThread() {
        return MAIN_THREAD_EXECUTOR;
    }
}
