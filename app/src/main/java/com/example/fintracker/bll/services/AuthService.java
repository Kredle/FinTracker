package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.session.SessionStorage;
import com.example.fintracker.bll.utils.PasswordHasher;
import com.example.fintracker.bll.validators.UserValidator;
import com.example.fintracker.dal.local.entities.UserEntity;
import com.example.fintracker.dal.repositories.DataCallback;
import com.example.fintracker.dal.repositories.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthService {

    private final UserRepository userRepository;
    private final SessionStorage sessionStorage;  // ← новое
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public AuthService(@NonNull Application application) {
        this.userRepository = new UserRepository(application);
        this.sessionStorage = new SessionStorage(application);  // ← новое
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public AuthService(
            @NonNull UserRepository userRepository,
            @NonNull SessionStorage sessionStorage,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.userRepository = userRepository;
        this.sessionStorage = sessionStorage;
        this.executorService = executorService;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    // ─────────────────────────────────────────────────────────────
    //  РЕГИСТРАЦИЯ
    // ─────────────────────────────────────────────────────────────

    public void register(
            @NonNull String email,
            @NonNull String username,
            @NonNull String password,
            @NonNull AuthResult.AuthCallback callback
    ) {
        executorService.execute(() -> {
            try {
                UserValidator.validateRegistration(email, username, password);
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            userRepository.checkIfUserExists(email, username, new DataCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean exists) {
                    if (Boolean.TRUE.equals(exists)) {
                        deliverFailure(callback, "Пользователь с таким email или именем уже существует");
                        return;
                    }
                    createAndSaveUser(email, username, password, callback);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка проверки пользователя: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ВХОД
    // ─────────────────────────────────────────────────────────────

    public void login(
            @NonNull String login,
            @NonNull String password,
            @NonNull AuthResult.AuthCallback callback
    ) {
        executorService.execute(() -> {
            if (login.trim().isEmpty()) {
                deliverFailure(callback, "Email или имя пользователя не может быть пустым");
                return;
            }
            try {
                UserValidator.isValidPassword(password);
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            String hashedPassword = PasswordHasher.hash(password);

            userRepository.getUserByEmailOrName(login.trim(), hashedPassword,
                    new DataCallback<UserEntity>() {
                        @Override
                        public void onSuccess(@Nullable UserEntity user) {
                            if (user == null) {
                                deliverFailure(callback, "Неверный логин или пароль");
                                return;
                            }
                            // Сохраняем в память и на диск
                            SessionManager.getInstance().login(user);
                            sessionStorage.saveUserId(user.id);  // ← новое
                            deliverSuccess(callback, user);
                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {
                            deliverFailure(callback, "Ошибка входа: " + throwable.getMessage());
                        }
                    });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ВЫХОД
    // ─────────────────────────────────────────────────────────────

    public void logout() {
        SessionManager.getInstance().logout();
        sessionStorage.clear();  // ← новое: очищаем SharedPreferences
    }

    // ─────────────────────────────────────────────────────────────
    //  ПРИВАТНЫЕ МЕТОДЫ
    // ─────────────────────────────────────────────────────────────

    private void createAndSaveUser(
            String email,
            String username,
            String password,
            AuthResult.AuthCallback callback
    ) {
        String hashedPassword = PasswordHasher.hash(password);
        String now = isoNow();

        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = email.trim();
        user.name = username.trim();
        user.password = hashedPassword;
        user.hourlyRate = 0.0;
        user.isBankSyncEnabled = false;
        user.isSynced = false;
        user.isDeleted = false;
        user.updatedAt = now;

        userRepository.insertUser(user, new DataCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void data) {
                // Сохраняем в память и на диск
                SessionManager.getInstance().login(user);
                sessionStorage.saveUserId(user.id);  // ← новое
                deliverSuccess(callback, user);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                deliverFailure(callback, "Ошибка сохранения пользователя: " + throwable.getMessage());
            }
        });
    }

    private void deliverSuccess(AuthResult.AuthCallback callback, UserEntity user) {
        mainThreadExecutor.execute(() -> callback.onResult(AuthResult.success(user)));
    }

    private void deliverFailure(AuthResult.AuthCallback callback, String message) {
        mainThreadExecutor.execute(() -> callback.onResult(AuthResult.failure(message)));
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }
}