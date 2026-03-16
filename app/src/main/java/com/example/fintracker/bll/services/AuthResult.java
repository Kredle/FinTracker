package com.example.fintracker.bll.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.dal.local.entities.UserEntity;

/**
 * Результат операций аутентификации (регистрация / вход).
 *
 * Использование во ViewModel:
 *
 *   authService.register(email, username, password, new AuthCallback() {
 *       public void onResult(AuthResult result) {
 *           if (result.isSuccess()) {
 *               UserEntity user = result.getUser(); // гарантированно не null
 *               // перейти на главный экран
 *           } else {
 *               showError(result.getErrorMessage());
 *           }
 *       }
 *   });
 */
public class AuthResult {

    private final boolean success;
    @Nullable private final UserEntity user;
    @Nullable private final String errorMessage;

    private AuthResult(boolean success, @Nullable UserEntity user, @Nullable String errorMessage) {
        this.success = success;
        this.user = user;
        this.errorMessage = errorMessage;
    }

    /** Создаёт успешный результат с залогиненным пользователем. */
    public static AuthResult success(@NonNull UserEntity user) {
        return new AuthResult(true, user, null);
    }

    /** Создаёт результат с ошибкой. */
    public static AuthResult failure(@NonNull String errorMessage) {
        return new AuthResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * Возвращает пользователя при успехе.
     * @throws IllegalStateException если результат не успешный
     */
    @NonNull
    public UserEntity getUser() {
        if (user == null) throw new IllegalStateException("No user in failed AuthResult");
        return user;
    }

    /**
     * Возвращает сообщение об ошибке при неудаче.
     * @throws IllegalStateException если результат успешный
     */
    @NonNull
    public String getErrorMessage() {
        if (errorMessage == null) throw new IllegalStateException("No error in successful AuthResult");
        return errorMessage;
    }

    /** Callback интерфейс для получения результата аутентификации. */
    public interface AuthCallback {
        void onResult(@NonNull AuthResult result);
    }
}