package com.example.fintracker.bll.session;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.entities.UserEntity;

public class SessionManager {

    private static volatile SessionManager instance;
    @Nullable
    private UserEntity currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public synchronized void login(@NonNull UserEntity user) {
        this.currentUser = user;

    }

    public synchronized void logout() {
        this.currentUser = null;
    }

    @Nullable
    public synchronized UserEntity getCurrentUser() {
        return currentUser;
    }

    @Nullable
    public synchronized String getCurrentUserId() {
        return currentUser != null ? currentUser.id : null;
    }

    @NonNull
    public synchronized String requireUserId() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in. Call login() first.");
        }
        return currentUser.id;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    @Nullable
    public synchronized String getCurrentUserEmail() {
        return currentUser != null ? currentUser.email : null;
    }
}

