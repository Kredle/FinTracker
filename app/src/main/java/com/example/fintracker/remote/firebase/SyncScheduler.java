package com.example.fintracker.remote.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * SyncScheduler manages scheduling of Firebase sync operations via WorkManager.
 * Ensures sync only occurs when network connectivity is available (WiFi or cellular).
 */
public class SyncScheduler {

    private static final String TAG = "SYNC_SCHEDULER";
    private static final String SYNC_WORK_NAME = "firebase_sync_periodic_work";

    /**
     * Schedules periodic Firebase sync.
     * WorkManager enforces a minimum 15-minute interval for periodic work.
     * Sync will run on any available network connection (WiFi or cellular).
     * To restrict to WiFi-only, change NetworkType.CONNECTED to NetworkType.UNMETERED.
     *
     * @param context Application or Activity context
     */
    public static void scheduleSync(@NonNull Context context) {
        Log.d(TAG, "Scheduling periodic Firebase sync (WiFi or cellular)");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Allows WiFi or cellular
                .build();

        PeriodicWorkRequest syncWorkRequest =
                new PeriodicWorkRequest.Builder(FirebaseSyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .addTag("firebase_sync")
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        SYNC_WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncWorkRequest
                );

        Log.d(TAG, "Periodic sync work enqueued successfully");
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
