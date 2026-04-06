package com.example.fintracker.bll.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.entities.UserEntity;

public class AuthResult {

    private final boolean success;
    @Nullable private final UserEntity user;
    @Nullable private final String errorMessage;

    private AuthResult(boolean success, @Nullable UserEntity user, @Nullable String errorMessage) {
        this.success = success;
        this.user = user;
        this.errorMessage = errorMessage;
    }

    public static AuthResult success(@NonNull UserEntity user) {
        return new AuthResult(true, user, null);
    }

    public static AuthResult failure(@NonNull String errorMessage) {
        return new AuthResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    @NonNull
    public UserEntity getUser() {
        if (user == null) throw new IllegalStateException("No user in failed AuthResult");
        return user;
    }

    @NonNull
    public String getErrorMessage() {
        if (errorMessage == null) throw new IllegalStateException("No error in successful AuthResult");
        return errorMessage;
    }

    public interface AuthCallback {
        void onResult(@NonNull AuthResult result);
    }
}