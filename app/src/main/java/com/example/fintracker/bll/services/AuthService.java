package com.example.fintracker.bll.services;

import android.app.Application;
import android.util.Log;

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
    //  РЕЄСТРАЦІЯ
    // ─────────────────────────────────────────────────────────────

    public void register(
            @NonNull String email,
            @NonNull String username,
            @NonNull String password,
            @NonNull AuthResult.AuthCallback callback
    ) {
        Log.d("AuthService", "🔵 register() called for: " + email);
        executorService.execute(() -> {
            try {
                Log.d("AuthService", "🟡 Validating registration data");
                UserValidator.validateRegistration(email, username, password);
                Log.d("AuthService", "🟢 Validation passed");
            } catch (IllegalArgumentException e) {
                Log.e("AuthService", "❌ Validation failed: " + e.getMessage());
                deliverFailure(callback, e.getMessage());
                return;
            }

            try {
                Log.d("AuthService", "🟡 Checking if user exists...");
                Log.d("AuthService", "📧 Email: " + email + ", Username: " + username);
                userRepository.checkIfUserExists(email, username, new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(@Nullable Boolean exists) {
                        Log.d("AuthService", "🟢 checkIfUserExists callback RECEIVED - exists: " + exists);
                        if (Boolean.TRUE.equals(exists)) {
                            Log.e("AuthService", "❌ User already exists");
                            deliverFailure(callback, "Користувач з такою електронною поштою або ім'ям уже існує");
                            return;
                        }
                        Log.d("AuthService", "✅ User does not exist, proceeding to create...");
                        Log.d("AuthService", "🟡 Creating and saving user...");
                        createAndSaveUser(email, username, password, callback);
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        Log.e("AuthService", "❌ checkIfUserExists ERROR: " + throwable.getMessage(), throwable);
                        deliverFailure(callback, "Помилка перевірки користувача: " + throwable.getMessage());
                    }
                });
                Log.d("AuthService", "⏳ checkIfUserExists called, waiting for callback...");
            } catch (Exception e) {
                Log.e("AuthService", "❌ Exception in register: " + e.getMessage(), e);
                deliverFailure(callback, "Помилка бази даних: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ВХІД
    // ─────────────────────────────────────────────────────────────

    public void login(
            @NonNull String login,
            @NonNull String password,
            @NonNull AuthResult.AuthCallback callback
    ) {
        executorService.execute(() -> {
            if (login.trim().isEmpty()) {
                deliverFailure(callback, "Електронна пошта або ім'я користувача не можуть бути пустими");
                return;
            }
            try {
                UserValidator.isValidPassword(password);
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            String hashedPassword = PasswordHasher.hash(password);
            Log.d("AuthService", "Login attempt: login='" + login.trim() + "', hashedPassword='" + hashedPassword + "'");

            try {
                userRepository.getUserByEmailOrName(login.trim(), hashedPassword,
                        new DataCallback<UserEntity>() {
                            @Override
                            public void onSuccess(@Nullable UserEntity user) {
                                if (user == null) {
                                    Log.w("AuthService", "Login failed: user not found for login='" + login.trim() + "'");
                                    deliverFailure(callback, "Невірний логін або пароль");
                                    return;
                                }
                                Log.d("AuthService", "Login successful for user: " + user.name + " (" + user.email + ")");
                                // Сохраняем в память и на диск
                                SessionManager.getInstance().login(user);
                                sessionStorage.saveUserId(user.id);  // ← новое
                                deliverSuccess(callback, user);
                            }

                            @Override
                            public void onError(@NonNull Throwable throwable) {
                                Log.e("AuthService", "Login database error", throwable);
                                deliverFailure(callback, "Помилка входу: " + throwable.getMessage());
                            }
                        });
            } catch (Exception e) {
                Log.e("AuthService", "Login exception", e);
                deliverFailure(callback, "Помилка бази даних: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ВИХІД
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
        Log.d("AuthService", "🟡 createAndSaveUser() started");
        String hashedPassword = PasswordHasher.hash(password);
        String now = isoNow();

        Log.d("AuthService", "Creating user: email='" + email.trim() + "', username='" + username.trim() + "'");

        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = email.trim();
        user.name = username.trim();
        user.password = hashedPassword;
        user.hourlyRate = 0.0;
        user.isBankSyncEnabled = false;
        user.generalLimit = 0.0;
        user.isSynced = false;
        user.isDeleted = false;
        user.updatedAt = now;

        Log.d("AuthService", "🟡 Calling userRepository.insertUser()");
        userRepository.insertUser(user, new DataCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void data) {
                Log.d("AuthService", "🟢 User created successfully in database: " + user.name);
                SessionManager.getInstance().login(user);
                sessionStorage.saveUserId(user.id);
                Log.d("AuthService", "🟢 SessionManager updated, calling deliverSuccess");
                deliverSuccess(callback, user);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                Log.e("AuthService", "❌ Failed to create user: " + throwable.getMessage(), throwable);
                deliverFailure(callback, "Ошибка сохранения пользователя: " + throwable.getMessage());
            }
        });
        Log.d("AuthService", "🟡 insertUser() callback registered");
    }

    private void deliverSuccess(AuthResult.AuthCallback callback, UserEntity user) {
        Log.d("AuthService", "🟢 deliverSuccess() - executing callback on main thread");
        mainThreadExecutor.execute(() -> {
            Log.d("AuthService", "✅ SUCCESS CALLBACK DELIVERED to RegisterActivity");
            callback.onResult(AuthResult.success(user));
        });
    }

    private void deliverFailure(AuthResult.AuthCallback callback, String message) {
        Log.d("AuthService", "❌ deliverFailure() - executing callback: " + message);
        mainThreadExecutor.execute(() -> {
            Log.d("AuthService", "❌ FAILURE CALLBACK DELIVERED to RegisterActivity: " + message);
            callback.onResult(AuthResult.failure(message));
        });
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }
}