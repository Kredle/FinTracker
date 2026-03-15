package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.utils.PasswordHasher;
import com.example.fintracker.bll.validators.UserValidator;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UserService — управление профилем текущего залогиненного пользователя.
 *
 * ИСПОЛЬЗОВАНИЕ:
 *
 *   UserService userService = new UserService(application);
 *
 *   // Сменить пароль:
 *   userService.changePassword("старый", "новый", result -> {
 *       if (result.isSuccess()) { ... }
 *       else { showError(result.getErrorMessage()); }
 *   });
 *
 *   // Обновить почасовую ставку:
 *   userService.setHourlyRate(250.0, result -> { ... });
 *
 *   // Получить почасовую ставку:
 *   userService.getHourlyRate(result -> {
 *       if (result.isSuccess()) {
 *           double rate = result.getValue();
 *       }
 *   });
 */
public class UserService {

    private final AppDatabase database;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public UserService(@NonNull Application application) {
        this.database           = AppDatabase.getInstance(application);
        this.executorService    = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public UserService(
            @NonNull AppDatabase database,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.database           = database;
        this.executorService    = executorService;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    // ─────────────────────────────────────────────────────────────
    //  СМЕНА ПАРОЛЯ
    // ─────────────────────────────────────────────────────────────

    /**
     * Меняет пароль текущего пользователя.
     * Требует ввода правильного старого пароля.
     *
     * Правила нового пароля — те же, что при регистрации (минимум 6 символов).
     * Новый пароль не должен совпадать со старым.
     *
     * @param oldPassword Текущий пароль (в открытом виде)
     * @param newPassword Новый пароль (в открытом виде)
     * @param callback    Результат операции
     */
    public void changePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword,
            @NonNull UserCallback callback
    ) {
        String userId = requireSession(callback);
        if (userId == null) return;

        executorService.execute(() -> {
            // Валидация нового пароля
            try {
                UserValidator.isValidPassword(newPassword);
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            // Загружаем актуальные данные из базы (не из кэша сессии)
            UserEntity user = database.userDao().getUserByIdSync(userId);
            if (user == null) {
                deliverFailure(callback, "Пользователь не найден");
                return;
            }

            // Проверяем старый пароль
            if (!PasswordHasher.verify(oldPassword, user.password)) {
                deliverFailure(callback, "Неверный текущий пароль");
                return;
            }

            // Новый пароль не должен совпадать со старым
            if (PasswordHasher.verify(newPassword, user.password)) {
                deliverFailure(callback, "Новый пароль должен отличаться от текущего");
                return;
            }

            // Сохраняем хэш нового пароля
            user.password  = PasswordHasher.hash(newPassword);
            user.isSynced  = false;
            user.updatedAt = isoNow();
            database.userDao().updateUser(user);

            // Обновляем сессию — данные изменились
            SessionManager.getInstance().login(user);

            deliverSuccess(callback, user);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ПОЧАСОВАЯ СТАВКА
    // ─────────────────────────────────────────────────────────────

    /**
     * Устанавливает почасовую ставку дохода текущего пользователя.
     * Используется для расчёта "стоимость в часах" при просмотре трат.
     *
     * @param hourlyRate Ставка (≥ 0)
     * @param callback   Результат операции
     */
    public void setHourlyRate(
            double hourlyRate,
            @NonNull UserCallback callback
    ) {
        String userId = requireSession(callback);
        if (userId == null) return;

        if (Double.isNaN(hourlyRate) || Double.isInfinite(hourlyRate)) {
            deliverFailure(callback, "Некорректное значение ставки");
            return;
        }
        if (hourlyRate < 0) {
            deliverFailure(callback, "Ставка не может быть отрицательной");
            return;
        }

        executorService.execute(() -> {
            UserEntity user = database.userDao().getUserByIdSync(userId);
            if (user == null) {
                deliverFailure(callback, "Пользователь не найден");
                return;
            }

            user.hourlyRate = hourlyRate;
            user.isSynced   = false;
            user.updatedAt  = isoNow();
            database.userDao().updateUser(user);

            // Обновляем сессию
            SessionManager.getInstance().login(user);

            deliverSuccess(callback, user);
        });
    }

    /**
     * Возвращает текущую почасовую ставку залогиненного пользователя.
     * Читается напрямую из сессии — нет обращения к БД.
     *
     * @param callback Результат: result.getValue() — значение ставки
     * @throws IllegalStateException если пользователь не залогинен
     */
    public void getHourlyRate(@NonNull DoubleCallback callback) {
        String userId = requireSessionDouble(callback);
        if (userId == null) return;

        // Берём из сессии — актуально, т.к. мы обновляем сессию после setHourlyRate
        UserEntity currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            mainThreadExecutor.execute(() -> callback.onResult(DoubleResult.failure("Пользователь не найден")));
            return;
        }
        mainThreadExecutor.execute(() -> callback.onResult(DoubleResult.success(currentUser.hourlyRate)));
    }

    // ─────────────────────────────────────────────────────────────
    //  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ─────────────────────────────────────────────────────────────

    @Nullable
    private String requireSession(@NonNull UserCallback callback) {
        try {
            return SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return null;
        }
    }

    @Nullable
    private String requireSessionDouble(@NonNull DoubleCallback callback) {
        try {
            return SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            mainThreadExecutor.execute(() ->
                    callback.onResult(DoubleResult.failure("Необходимо войти в аккаунт")));
            return null;
        }
    }

    private void deliverSuccess(@NonNull UserCallback callback, @NonNull UserEntity user) {
        mainThreadExecutor.execute(() -> callback.onResult(UserResult.success(user)));
    }

    private void deliverFailure(@NonNull UserCallback callback, @NonNull String message) {
        mainThreadExecutor.execute(() -> callback.onResult(UserResult.failure(message)));
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    // ─────────────────────────────────────────────────────────────
    //  UserResult / UserCallback
    // ─────────────────────────────────────────────────────────────

    /**
     * Результат операции над профилем пользователя.
     * При успехе содержит обновлённый UserEntity.
     */
    public static class UserResult {

        private final boolean success;
        @Nullable private final UserEntity user;
        @Nullable private final String errorMessage;

        private UserResult(boolean success,
                           @Nullable UserEntity user,
                           @Nullable String errorMessage) {
            this.success      = success;
            this.user         = user;
            this.errorMessage = errorMessage;
        }

        public static UserResult success(@NonNull UserEntity user) {
            return new UserResult(true, user, null);
        }

        public static UserResult failure(@NonNull String errorMessage) {
            return new UserResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }

        @NonNull
        public UserEntity getUser() {
            if (user == null) throw new IllegalStateException("No user in failed UserResult");
            return user;
        }

        @NonNull
        public String getErrorMessage() {
            if (errorMessage == null) throw new IllegalStateException("No error in successful UserResult");
            return errorMessage;
        }
    }

    public interface UserCallback {
        void onResult(@NonNull UserResult result);
    }

    // ─────────────────────────────────────────────────────────────
    //  DoubleResult / DoubleCallback  (для getHourlyRate)
    // ─────────────────────────────────────────────────────────────

    public static class DoubleResult {

        private final boolean success;
        private final double value;
        @Nullable private final String errorMessage;

        private DoubleResult(boolean success, double value, @Nullable String errorMessage) {
            this.success      = success;
            this.value        = value;
            this.errorMessage = errorMessage;
        }

        public static DoubleResult success(double value) {
            return new DoubleResult(true, value, null);
        }

        public static DoubleResult failure(@NonNull String errorMessage) {
            return new DoubleResult(false, 0.0, errorMessage);
        }

        public boolean isSuccess() { return success; }

        /** Значение ставки. Имеет смысл только при isSuccess() == true. */
        public double getValue() { return value; }

        @NonNull
        public String getErrorMessage() {
            if (errorMessage == null) throw new IllegalStateException("No error in successful DoubleResult");
            return errorMessage;
        }
    }

    public interface DoubleCallback {
        void onResult(@NonNull DoubleResult result);
    }
}