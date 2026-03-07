package com.example.fintracker.remote.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * FirebaseSyncWorker handles background synchronization of unsynced data to Firestore.
 * Triggered automatically by WorkManager when network connectivity is available.
 */
public class FirebaseSyncWorker extends Worker {

    private static final String TAG = "FIREBASE_SYNC_WORKER";

    public FirebaseSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "FirebaseSyncWorker started");

        try {
            FirebaseSyncManager syncManager = new FirebaseSyncManager(getApplicationContext());
            syncManager.syncUnsyncedDataToCloud();

            Log.d(TAG, "Sync initiated successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed with exception", e);
            return Result.retry();
        }
    }
}

