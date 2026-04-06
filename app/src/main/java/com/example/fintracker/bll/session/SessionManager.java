package com.example.fintracker.bll.session;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.entities.UserEntity;

/**
 * SessionManager — хранит данные залогиненного пользователя в памяти на время сессии.
 *
 * ИСПОЛЬЗОВАНИЕ:
 *   После успешного логина/регистрации:
 *       SessionManager.getInstance().login(userEntity);
 *
 *   В любом месте приложения:
 *       String userId = SessionManager.getInstance().requireUserId();
 *       boolean isLoggedIn = SessionManager.getInstance().isLoggedIn();
 *
 *   При выходе:
 *       SessionManager.getInstance().logout();
 *
 * ВАЖНО: Данные хранятся только в памяти (теряются при перезапуске приложения).
 * Для персистентной сессии — сохраняйте userId в SharedPreferences и
 * восстанавливайте пользователя при старте приложения (см. пример в конце файла).
 */
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

    /** Сохраняет пользователя как текущего (вызывается после логина/регистрации). */
    public synchronized void login(@NonNull UserEntity user) {
        this.currentUser = user;

    }

    /** Очищает сессию (вызывается при логауте). */
    public synchronized void logout() {
        this.currentUser = null;
    }

    /** Возвращает текущего пользователя или null если не залогинен. */
    @Nullable
    public synchronized UserEntity getCurrentUser() {
        return currentUser;
    }

    /** Возвращает ID текущего пользователя или null если не залогинен. */
    @Nullable
    public synchronized String getCurrentUserId() {
        return currentUser != null ? currentUser.id : null;
    }

    /**
     * Возвращает ID текущего пользователя.
     * @throws IllegalStateException если пользователь не залогинен
     */
    @NonNull
    public synchronized String requireUserId() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in. Call login() first.");
        }
        return currentUser.id;
    }

    /** Проверяет, залогинен ли пользователь. */
    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Возвращает email текущего пользователя или null если не залогинен. */
    @Nullable
    public synchronized String getCurrentUserEmail() {
        return currentUser != null ? currentUser.email : null;
    }
}

/*
 * ── ПРИМЕР: Персистентная сессия через SharedPreferences ──────────────────────
 *
 * В FinTrackerApplication.onCreate():
 *
 *   SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
 *   String savedUserId = prefs.getString("userId", null);
 *
 *   if (savedUserId != null) {
 *       // Восстанавливаем юзера из базы (async)
 *       AppDatabase.getInstance(this)
 *           .userDao()
 *           .getUserByIdSync(savedUserId);   // в background thread!
 *       // → SessionManager.getInstance().login(user);
 *   }
 *
 * При логине сохраняем:
 *   prefs.edit().putString("userId", user.id).apply();
 *
 * При логауте очищаем:
 *   prefs.edit().remove("userId").apply();
 * ────────────────────────────────────────────────────────────────────────────────
 */