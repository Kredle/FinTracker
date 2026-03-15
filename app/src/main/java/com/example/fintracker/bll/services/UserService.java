package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;

import com.example.fintracker.bll.services.bank.BankNotificationService;
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
    private final Context context;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public UserService(@NonNull Application application) {
        this.database           = AppDatabase.getInstance(application);
        this.context            = application.getApplicationContext();
        this.executorService    = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public UserService(
            @NonNull AppDatabase database,
            @NonNull Context context,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.database           = database;
        this.context            = context;
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
    //  БАНКОВСКАЯ СИНХРОНИЗАЦИЯ
    // ─────────────────────────────────────────────────────────────

    /**
     * Включает или отключает автоматическую запись транзакций из уведомлений MonoBank.
     *
     * При включении (enabled = true):
     *   1. Проверяет, выдано ли системное разрешение на чтение уведомлений.
     *      Если нет — возвращает ошибку с предложением открыть настройки.
     *   2. Включает компонент BankNotificationService через PackageManager —
     *      система начнёт доставлять уведомления.
     *   3. Сохраняет флаг isBankSyncEnabled = true в базе и сессии.
     *
     * При отключении (enabled = false):
     *   1. Отключает компонент BankNotificationService — система перестаёт
     *      запускать сервис. Уведомления больше не обрабатываются.
     *   2. Сохраняет флаг isBankSyncEnabled = false в базе и сессии.
     *
     * @param enabled  true — включить, false — выключить
     * @param callback Результат операции
     */
    /**
     * Включает банковскую синхронизацию и привязывает её к указанному счёту.
     * Используй этот метод когда пользователь выбирает счёт и включает синхронизацию.
     *
     * Порядок действий:
     *   1. Проверяет системное разрешение на чтение уведомлений.
     *   2. Проверяет, что счёт существует и принадлежит текущему пользователю
     *      (либо пользователь является участником совместного счёта).
     *   3. Сохраняет accountId как целевой для банк-синка.
     *   4. Включает компонент сервиса через PackageManager.
     *   5. Обновляет isBankSyncEnabled = true в базе и сессии.
     *
     * @param accountId ID счёта, на который будут записываться транзакции
     * @param callback  Результат операции
     */
    public void enableBankSync(
            @NonNull String accountId,
            @NonNull UserCallback callback
    ) {
        String userId = requireSession(callback);
        if (userId == null) return;

        // Проверяем разрешение до фонового потока — быстрый сброс
        if (!BankNotificationService.isNotificationAccessGranted(context)) {
            deliverFailure(callback,
                    "Нет разрешения на чтение уведомлений. " +
                            "Выдай его в: Настройки → Приложения → Специальный доступ → " +
                            "Доступ к уведомлениям");
            return;
        }

        final String userIdFinal = userId;

        executorService.execute(() -> {
            // Проверяем, что счёт существует
            var account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }

            // Проверяем доступ: владелец счёта или участник совместного счёта
            boolean isOwner = userIdFinal.equals(account.ownerId);
            boolean isMember = account.isShared &&
                    database.sharedAccountMemberDao()
                            .getMemberSync(accountId, userIdFinal) != null;

            if (!isOwner && !isMember) {
                deliverFailure(callback, "Нет доступа к этому счёту");
                return;
            }

            UserEntity user = database.userDao().getUserByIdSync(userIdFinal);
            if (user == null) {
                deliverFailure(callback, "Пользователь не найден");
                return;
            }

            // Сохраняем целевой счёт
            BankNotificationService.setTargetAccount(context, accountId);

            // Включаем компонент сервиса
            BankNotificationService.enable(context);

            // Обновляем флаг в базе
            user.isBankSyncEnabled = true;
            user.isSynced          = false;
            user.updatedAt         = isoNow();
            database.userDao().updateUser(user);

            // Обновляем сессию
            SessionManager.getInstance().login(user);

            deliverSuccess(callback, user);
        });
    }

    /**
     * Отключает банковскую синхронизацию.
     * Компонент сервиса отключается на уровне системы — уведомления
     * MonoBank перестают обрабатываться до следующего вызова enableBankSync().
     * Привязка к счёту сохраняется, чтобы при повторном включении не нужно
     * было выбирать счёт заново.
     *
     * @param callback Результат операции
     */
    public void disableBankSync(@NonNull UserCallback callback) {
        String userId = requireSession(callback);
        if (userId == null) return;

        executorService.execute(() -> {
            UserEntity user = database.userDao().getUserByIdSync(userId);
            if (user == null) {
                deliverFailure(callback, "Пользователь не найден");
                return;
            }

            // Отключаем компонент сервиса
            BankNotificationService.disable(context);

            // Обновляем флаг в базе
            user.isBankSyncEnabled = false;
            user.isSynced          = false;
            user.updatedAt         = isoNow();
            database.userDao().updateUser(user);

            // Обновляем сессию
            SessionManager.getInstance().login(user);

            deliverSuccess(callback, user);
        });
    }

    /**
     * Меняет счёт для банковской синхронизации не отключая её.
     * Если синхронизация выключена — только сохраняет новый счёт,
     * не включая сервис.
     *
     * @param newAccountId ID нового счёта
     * @param callback     Результат операции
     */
    public void changeBankSyncAccount(
            @NonNull String newAccountId,
            @NonNull UserCallback callback
    ) {
        String userId = requireSession(callback);
        if (userId == null) return;

        final String userIdFinal = userId;

        executorService.execute(() -> {
            var account = database.accountDao().getAccountByIdSync(newAccountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }

            boolean isOwner = userIdFinal.equals(account.ownerId);
            boolean isMember = account.isShared &&
                    database.sharedAccountMemberDao()
                            .getMemberSync(newAccountId, userIdFinal) != null;

            if (!isOwner && !isMember) {
                deliverFailure(callback, "Нет доступа к этому счёту");
                return;
            }

            UserEntity user = database.userDao().getUserByIdSync(userIdFinal);
            if (user == null) {
                deliverFailure(callback, "Пользователь не найден");
                return;
            }

            // Сохраняем новый целевой счёт
            BankNotificationService.setTargetAccount(context, newAccountId);

            deliverSuccess(callback, user);
        });
    }

    /**
     * Возвращает ID счёта, привязанного к банковской синхронизации, или null.
     */
    @Nullable
    public String getBankSyncAccountId() {
        return BankNotificationService.getTargetAccountId(context);
    }

    /**
     * @deprecated Используй {@link #enableBankSync(String, UserCallback)}
     *             или {@link #disableBankSync(UserCallback)}
     */
    @Deprecated
    public void setBankSyncEnabled(boolean enabled, @NonNull UserCallback callback) {
        if (enabled) {
            String savedAccountId = BankNotificationService.getTargetAccountId(context);
            if (savedAccountId == null) {
                deliverFailure(callback,
                        "Выбери счёт для синхронизации: вызови enableBankSync(accountId, callback)");
                return;
            }
            enableBankSync(savedAccountId, callback);
        } else {
            disableBankSync(callback);
        }
    }

    /**
     * Возвращает текущее состояние банковской синхронизации для залогиненного пользователя.
     * Читается из сессии — без обращения к БД.
     *
     * @return true если isBankSyncEnabled == true у текущего пользователя
     * @throws IllegalStateException если пользователь не залогинен
     */
    public boolean isBankSyncEnabled() {
        UserEntity user = SessionManager.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("No user is logged in");
        return user.isBankSyncEnabled;
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