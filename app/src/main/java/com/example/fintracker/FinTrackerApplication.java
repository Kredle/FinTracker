package com.example.fintracker;

import android.app.Application;

import com.example.fintracker.remote.firebase.SyncScheduler;

/**
 * FinTrackerApplication
 *
 * Custom Application class for the FinTracker app.
 * Initializes Firebase sync scheduling on app startup.
 */
public class FinTrackerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Schedule Firebase sync to run when network is available
        SyncScheduler.scheduleSync(this);
    }
}

