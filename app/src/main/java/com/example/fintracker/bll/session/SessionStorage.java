package com.example.fintracker.bll.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SessionStorage {

    private static final String PREFS_NAME = "fintracker_session";
    private static final String KEY_USER_ID = "userId";

    private final SharedPreferences prefs;

    public SessionStorage(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(@NonNull String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    @Nullable
    public String getSavedUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void clear() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }
}

