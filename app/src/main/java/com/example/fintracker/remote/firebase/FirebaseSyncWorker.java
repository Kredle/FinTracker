package com.example.fintracker.remote.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * FirebaseSyncWorker handles background synchronization of unsynced data to Firestore.
 * Triggered automatically by WorkManager when network connectivity is available.
 * Only runs sync when a user is authenticated to avoid unnecessary retries.
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

        // Check if user is authenticated before attempting sync
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No authenticated user, skipping sync");
            return Result.success(); // Return success to avoid retries when no user is logged in
        }

        try {
            FirebaseSyncManager syncManager = new FirebaseSyncManager(getApplicationContext());
            boolean allSynced = syncManager.syncUnsyncedDataToCloud();
            if (allSynced) {
                Log.d(TAG, "Sync completed successfully");
                return Result.success();
            }

            Log.w(TAG, "Sync completed with failures, requesting retry");
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed with exception", e);
            return Result.retry();
        }
    }
}
