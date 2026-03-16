package com.example.fintracker.bll.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SessionStorage — сохраняет userId в SharedPreferences.
 * Благодаря этому приложение "помнит" залогиненного юзера после перезапуска.
 */
public class SessionStorage {

    private static final String PREFS_NAME = "fintracker_session";
    private static final String KEY_USER_ID = "userId";

    private final SharedPreferences prefs;

    public SessionStorage(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Сохраняет ID залогиненного пользователя. */
    public void saveUserId(@NonNull String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    /** Возвращает сохранённый userId или null если никто не логинился. */
    @Nullable
    public String getSavedUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /** Очищает сохранённую сессию (при логауте). */
    public void clear() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }
}