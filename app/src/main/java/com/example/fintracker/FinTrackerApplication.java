package com.example.fintracker;

import android.app.Application;
import android.util.Log;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.session.SessionStorage;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;
import com.example.fintracker.remote.firebase.SyncScheduler;

import java.util.concurrent.Executors;

public class FinTrackerApplication extends Application {

    private static final String TAG = "APP_SESSION";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FinTrackerApplication onCreate started");

        try {
            boolean dbInitialized = false;
            Exception lastError = null;
            
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    Log.d(TAG, "Database init attempt " + attempt + "/5");
                    AppDatabase.getInstance(this);
                    Log.d(TAG, "Database initialized successfully on attempt " + attempt);
                    dbInitialized = true;
                    break;
                } catch (Exception dbError) {
                    lastError = dbError;
                    Log.e(TAG, "Database init attempt " + attempt + " failed: " + dbError.getClass().getSimpleName());
                    
                    if (dbError.getMessage() != null &&
                        dbError.getMessage().contains("cannot verify data integrity")) {
                        
                        Log.w(TAG, "Integrity error detected! Performing emergency reset... (attempt " + attempt + ")");
                        try {
                            AppDatabase.resetDatabase(this);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ignored) {}
                            Log.d(TAG, "Emergency reset completed, retrying...");
                        } catch (Exception resetError) {
                            Log.e(TAG, "Reset failed: " + resetError.getMessage());
                            if (attempt < 5) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException ignored) {}
                            }
                        }
                    } else {
                        Log.e(TAG, "Non-integrity error: " + dbError.getClass().getSimpleName());
                        if (attempt < 5) {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ignored) {}
                        }
                    }
                }
            }
            
            if (!dbInitialized) {
                Log.e(TAG, "CRITICAL: Database failed to initialize after 5 attempts");
                Log.e(TAG, "Last error: " + (lastError != null ? lastError.getMessage() : "unknown"));
            }
            
            restoreSession();

            try {
                SyncScheduler.scheduleSync(this);
                Log.d(TAG, "Firebase sync scheduled successfully");
            } catch (Exception e) {
                Log.e(TAG, "Warning during Firebase sync scheduling: " + e.getMessage());
            }
            Log.d(TAG, "FinTrackerApplication onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Critical error in Application.onCreate(): " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("resource")
    private void restoreSession() {
        try {
            SessionStorage sessionStorage = new SessionStorage(this);
            String savedUserId = sessionStorage.getSavedUserId();

            if (savedUserId == null) {
                Log.d(TAG, "No saved session - user not logged in");
                return;
            }

            Log.d(TAG, "Found saved userId: " + savedUserId + " - restoring session...");

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Log.d(TAG, "Loading database for session restore...");
                    AppDatabase db = AppDatabase.getInstance(FinTrackerApplication.this);
                    Log.d(TAG, "Database initialized successfully");
                    
                    UserEntity user = db.userDao().getUserByIdSync(savedUserId);

                    if (user != null && !user.isDeleted) {
                        SessionManager.getInstance().login(user);
                        Log.d(TAG, "Session restored: " + user.name + " (" + user.email + ")");
                    } else {
                        sessionStorage.clear();
                        Log.w(TAG, "Saved userId not found in database - session cleared");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ERROR during session restore: " + e.getClass().getName() + " - " + e.getMessage(), e);
                    for (StackTraceElement ste : e.getStackTrace()) {
                        Log.e(TAG, "  at " + ste.toString());
                    }
                    sessionStorage.clear();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in restoreSession: " + e.getMessage(), e);
        }
    }
}
