package com.example.fintracker;

import android.app.Application;
import android.util.Log;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.session.SessionStorage;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;
import com.example.fintracker.remote.firebase.SyncScheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class FinTrackerApplication extends Application {

    private static final String TAG = "APP_SESSION";

    @Override
    public void onCreate() {
        super.onCreate();

        // Восстанавливаем сессию из SharedPreferences
        restoreSession();

        // Планируем Firebase sync
        SyncScheduler.scheduleSync(this);
    }

    /**
     * Проверяет SharedPreferences: если userId сохранён — загружает юзера из
     * Room и устанавливает в SessionManager. Всё выполняется в фоновом потоке,
     * т.к. Room нельзя использовать на главном потоке.
     */
    private void restoreSession() {
        SessionStorage sessionStorage = new SessionStorage(this);
        String savedUserId = sessionStorage.getSavedUserId();

        if (savedUserId == null) {
            Log.d(TAG, "Нет сохранённой сессии — пользователь не залогинен");
            return;
        }

        Log.d(TAG, "Найден сохранённый userId: " + savedUserId + " — восстанавливаем сессию...");

        // Загружаем юзера из базы в фоне
        public class FinTrackerApplication extends Application {

        Log.d(TAG, "Найден сохранённый userId: " + savedUserId + " — восстанавливаем сессию...");

        // Загружаем юзера из базы в фоне
        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity user = AppDatabase.getInstance(this)
                    .userDao()
                    .getUserByIdSync(savedUserId);

            if (user != null && !user.isDeleted) {
                SessionManager.getInstance().login(user);
                Log.d(TAG, "✅ Сессия восстановлена: " + user.name + " (" + user.email + ")");
            } else {
                // Юзер удалён или не найден — очищаем устаревшие данные
                sessionStorage.clear();
                Log.w(TAG, "⚠ Сохранённый userId не найден в базе — сессия сброшена");
            }
        });
    }
}
