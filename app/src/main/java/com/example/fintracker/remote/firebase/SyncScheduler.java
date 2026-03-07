package com.example.fintracker.remote.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

/**
 * SyncScheduler manages scheduling of Firebase sync operations via WorkManager.
 * Ensures sync only occurs when network connectivity is available.
 */
public class SyncScheduler {

    private static final String TAG = "SYNC_SCHEDULER";
    private static final String SYNC_WORK_NAME = "firebase_sync_work";

    /**
     * Schedules a Firebase sync operation.
     * The sync will only execute when the device has network connectivity.
     *
     * @param context Application or Activity context
     */
    public static void scheduleSync(@NonNull Context context) {
        Log.d(TAG, "Scheduling Firebase sync with network constraints");

        // Set constraints to require network connectivity
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create a one-time work request for Firebase sync
        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(FirebaseSyncWorker.class)
                .setConstraints(constraints)
                .addTag("firebase_sync")
                .build();

        // Enqueue the work request (replace existing if already queued)
        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        SYNC_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        syncWorkRequest
                );

        Log.d(TAG, "Sync work enqueued successfully");
    }

    /**
     * Cancels any pending sync work.
     *
     * @param context Application or Activity context
     */
    public static void cancelSync(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME);
        Log.d(TAG, "Sync work cancelled");
    }
}

