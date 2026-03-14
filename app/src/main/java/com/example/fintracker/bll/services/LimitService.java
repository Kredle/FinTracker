package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.validators.LimitValidator;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.LimitEntity;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.repositories.LimitRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LimitService — бизнес-логика для управления лимитами расходов.
 *
 * Поддерживаемые типы лимитов (все опциональные поля = null):
 *
 *   1. Общий лимит на счёт:
 *      setAccountWideLimit(accountId, amount, period, cb)
 *      → userId=null, tagId=null
 *
 *   2. Лимит на участника в совместном счёте:
 *      setUserLimitInAccount(accountId, userId, amount, period, cb)
 *      → userId=X, tagId=null
 *
 *   3. Общий лимит на тег в счёте:
 *      setTagWideLimit(accountId, tagId, amount, period, cb)
 *      → userId=null, tagId=X
 *
 *   4. Лимит на тег для конкретного участника:
 *      setUserTagLimit(accountId, userId, tagId, amount, period, cb)
 *      → userId=X, tagId=X
 *
 * period должен быть одним из: "DAY", "WEEK", "MONTH"
 *
 * Если лимит для данной комбинации уже существует — он обновляется (upsert).
 *
 * ПОЛУЧЕНИЕ:
 *   getAccountWideLimit(accountId)               → LiveData<LimitEntity>
 *   getUserLimitInAccount(accountId, userId)      → LiveData<LimitEntity>
 *   getTagWideLimit(accountId, tagId)             → LiveData<LimitEntity>
 *   getUserTagLimit(accountId, userId, tagId)     → LiveData<LimitEntity>
 *   getAllLimitsForAccount(accountId)             → LiveData<List<LimitEntity>>
 *
 * УДАЛЕНИЕ:
 *   deleteLimit(limitId, cb)
 */
public class LimitService {

    private final AppDatabase database;
    private final LimitRepository limitRepository;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public LimitService(@NonNull Application application) {
        this.database          = AppDatabase.getInstance(application);
        this.limitRepository   = new LimitRepository(application);
        this.executorService   = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public LimitService(
            @NonNull AppDatabase database,
            @NonNull LimitRepository limitRepository,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.database           = database;
        this.limitRepository    = limitRepository;
        this.executorService    = executorService;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    // ═════════════════════════════════════════════════════════════
    //  УСТАНОВКА ЛИМИТОВ (upsert — создать или обновить)
    // ═════════════════════════════════════════════════════════════

    /**
     * Тип 1. Общий лимит на весь счёт.
     * Ограничивает суммарные расходы по счёту за указанный период.
     *
     * @param accountId ID счёта
     * @param amount    Максимальная сумма (> 0)
     * @param period    "DAY", "WEEK" или "MONTH"
     * @param callback  Результат операции
     */
    public void setAccountWideLimit(
            @NonNull String accountId,
            double amount,
            @NonNull String period,
            @NonNull LimitCallback callback
    ) {
        requireSession(callback);
        executorService.execute(() -> {
            if (!validate(amount, period, callback)) return;
            if (database.accountDao().getAccountByIdSync(accountId) == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }

            LimitEntity existing = database.limitDao().getAccountWideLimitSync(accountId);
            upsert(existing, accountId, null, null, amount, period, callback);
        });
    }

    /**
     * Тип 2. Лимит на конкретного участника в совместном счёте.
     * Ограничивает расходы одного пользователя по счёту за период.
     * Только для совместных счётов (isShared == true).
     *
     * @param accountId    ID совместного счёта
     * @param targetUserId ID пользователя, на которого ставится лимит
     * @param amount       Максимальная сумма (> 0)
     * @param period       "DAY", "WEEK" или "MONTH"
     * @param callback     Результат операции
     */
    public void setUserLimitInAccount(
            @NonNull String accountId,
            @NonNull String targetUserId,
            double amount,
            @NonNull String period,
            @NonNull LimitCallback callback
    ) {
        String currentUserId = requireSession(callback);
        if (currentUserId == null) return;

        executorService.execute(() -> {
            if (!validate(amount, period, callback)) return;

            var account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            if (!account.isShared) {
                deliverFailure(callback, "Лимит на участника можно задать только для совместного счёта");
                return;
            }
            // Лимит может устанавливать только ADMIN счёта
            var adminMember = database.sharedAccountMemberDao()
                    .getMemberSync(accountId, currentUserId);
            if (adminMember == null || !"ADMIN".equals(adminMember.role)) {
                deliverFailure(callback, "Только администратор может устанавливать лимиты на участников");
                return;
            }
            // Целевой пользователь должен быть участником счёта
            if (database.sharedAccountMemberDao().getMemberSync(accountId, targetUserId) == null) {
                deliverFailure(callback, "Указанный пользователь не является участником счёта");
                return;
            }

            LimitEntity existing = database.limitDao()
                    .getUserLimitInAccountSync(accountId, targetUserId);
            upsert(existing, accountId, targetUserId, null, amount, period, callback);
        });
    }

    /**
     * Тип 3. Общий лимит на тег в счёте.
     * Ограничивает суммарные расходы по данному тегу в рамках счёта за период.
     *
     * @param accountId ID счёта
     * @param tagId     ID тега
     * @param amount    Максимальная сумма (> 0)
     * @param period    "DAY", "WEEK" или "MONTH"
     * @param callback  Результат операции
     */
    public void setTagWideLimit(
            @NonNull String accountId,
            @NonNull String tagId,
            double amount,
            @NonNull String period,
            @NonNull LimitCallback callback
    ) {
        requireSession(callback);
        executorService.execute(() -> {
            if (!validate(amount, period, callback)) return;
            if (database.accountDao().getAccountByIdSync(accountId) == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            if (database.tagDao().getTagByIdSync(tagId) == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            LimitEntity existing = database.limitDao().getTagWideLimitSync(accountId, tagId);
            upsert(existing, accountId, null, tagId, amount, period, callback);
        });
    }

    /**
     * Тип 4. Лимит на тег для конкретного участника в совместном счёте.
     * Ограничивает расходы одного пользователя по данному тегу за период.
     * Только для совместных счётов.
     *
     * @param accountId    ID совместного счёта
     * @param targetUserId ID пользователя
     * @param tagId        ID тега
     * @param amount       Максимальная сумма (> 0)
     * @param period       "DAY", "WEEK" или "MONTH"
     * @param callback     Результат операции
     */
    public void setUserTagLimit(
            @NonNull String accountId,
            @NonNull String targetUserId,
            @NonNull String tagId,
            double amount,
            @NonNull String period,
            @NonNull LimitCallback callback
    ) {
        String currentUserId = requireSession(callback);
        if (currentUserId == null) return;

        executorService.execute(() -> {
            if (!validate(amount, period, callback)) return;

            var account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            if (!account.isShared) {
                deliverFailure(callback, "Лимит на участника по тегу можно задать только для совместного счёта");
                return;
            }
            var adminMember = database.sharedAccountMemberDao()
                    .getMemberSync(accountId, currentUserId);
            if (adminMember == null || !"ADMIN".equals(adminMember.role)) {
                deliverFailure(callback, "Только администратор может устанавливать лимиты на участников");
                return;
            }
            if (database.sharedAccountMemberDao().getMemberSync(accountId, targetUserId) == null) {
                deliverFailure(callback, "Указанный пользователь не является участником счёта");
                return;
            }
            if (database.tagDao().getTagByIdSync(tagId) == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            LimitEntity existing = database.limitDao()
                    .getUserTagLimitSync(accountId, targetUserId, tagId);
            upsert(existing, accountId, targetUserId, tagId, amount, period, callback);
        });
    }

    // ═════════════════════════════════════════════════════════════
    //  ПОЛУЧЕНИЕ ЛИМИТОВ
    // ═════════════════════════════════════════════════════════════

    /**
     * Тип 1. Общий лимит на счёт.
     * LiveData эмитит null, если лимит не задан.
     */
    public LiveData<LimitEntity> getAccountWideLimit(@NonNull String accountId) {
        return database.limitDao().getAccountWideLimit(accountId);
    }

    /**
     * Тип 2. Лимит на конкретного пользователя в совместном счёте.
     * LiveData эмитит null, если лимит не задан.
     *
     * @param accountId ID совместного счёта
     * @param userId    ID пользователя
     */
    public LiveData<LimitEntity> getUserLimitInAccount(
            @NonNull String accountId,
            @NonNull String userId
    ) {
        return database.limitDao().getUserLimitInAccount(accountId, userId);
    }

    /**
     * Тип 3. Общий лимит на тег в счёте.
     * LiveData эмитит null, если лимит не задан.
     */
    public LiveData<LimitEntity> getTagWideLimit(
            @NonNull String accountId,
            @NonNull String tagId
    ) {
        return database.limitDao().getTagWideLimit(accountId, tagId);
    }

    /**
     * Тип 4. Лимит на тег для конкретного пользователя.
     * LiveData эмитит null, если лимит не задан.
     */
    public LiveData<LimitEntity> getUserTagLimit(
            @NonNull String accountId,
            @NonNull String userId,
            @NonNull String tagId
    ) {
        return database.limitDao().getUserTagLimit(accountId, userId, tagId);
    }

    /**
     * Все лимиты счёта (все типы сразу).
     * Удобно для экрана "Настройки лимитов".
     */
    public LiveData<List<LimitEntity>> getAllLimitsForAccount(@NonNull String accountId) {
        return limitRepository.getLimitsByAccountId(accountId);
    }

    // ═════════════════════════════════════════════════════════════
    //  УДАЛЕНИЕ ЛИМИТА
    // ═════════════════════════════════════════════════════════════

    /**
     * Удаляет лимит (soft-delete). Доступно владельцу счёта или ADMIN совместного счёта.
     *
     * @param limitId  ID лимита
     * @param callback Результат операции
     */
    public void deleteLimit(
            @NonNull String limitId,
            @NonNull LimitCallback callback
    ) {
        String currentUserId = requireSession(callback);
        if (currentUserId == null) return;

        executorService.execute(() -> {
            LimitEntity limit = findLimitById(limitId);
            if (limit == null) {
                deliverFailure(callback, "Лимит не найден");
                return;
            }

            // Проверяем права: владелец счёта или ADMIN
            var account = database.accountDao().getAccountByIdSync(limit.accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }

            boolean isOwner = currentUserId.equals(account.ownerId);
            boolean isAdmin = false;
            if (account.isShared) {
                var member = database.sharedAccountMemberDao()
                        .getMemberSync(limit.accountId, currentUserId);
                isAdmin = member != null && "ADMIN".equals(member.role);
            }

            if (!isOwner && !isAdmin) {
                deliverFailure(callback, "Нет прав для удаления этого лимита");
                return;
            }

            limit.isDeleted = true;
            limit.isSynced  = false;
            limit.updatedAt = isoNow();
            database.limitDao().updateLimit(limit);
            deliverSuccess(callback, limit);
        });
    }

    // ═════════════════════════════════════════════════════════════
    //  ПРОВЕРКА ПРЕВЫШЕНИЯ ЛИМИТА
    // ═════════════════════════════════════════════════════════════

    /**
     * Проверяет, не будет ли превышен лимит при добавлении newAmount к текущим тратам.
     * Использует тот же TransactionFilter + getFilteredTransactionsSync для подсчёта трат.
     *
     * Возвращает true, если лимит будет превышен (или уже превышен).
     *
     * @param limit     Лимит для проверки
     * @param newAmount Сумма новой транзакции
     * @return true — лимит будет превышен
     */
    public boolean willExceedLimit(@NonNull LimitEntity limit, double newAmount) {
        double currentSpend = getCurrentSpend(limit);
        return (currentSpend + newAmount) > limit.amountLimit;
    }

    /**
     * Возвращает текущую сумму трат по данному лимиту за его период.
     * Используй для отображения прогресса лимита в UI.
     *
     * @param limit Лимит, для которого считаем траты
     * @return Сумма расходов за текущий период
     */
    public double getCurrentSpend(@NonNull LimitEntity limit) {
        String[] range = periodToDateRange(limit.period);

        TransactionFilter filter = new TransactionFilter.Builder()
                .accountId(limit.accountId)
                .userId(limit.userId)       // null — все участники; не-null — конкретный юзер
                .tagId(limit.tagId)         // null — все теги; не-null — конкретный тег
                .type("EXPENSE")
                .dateFrom(range[0])
                .dateTo(range[1])
                .build();

        List<TransactionEntity> transactions =
                database.transactionDao().getFilteredTransactionsSync(
                        TransactionService.buildQueryStatic(filter));

        double total = 0;
        for (TransactionEntity tx : transactions) {
            total += tx.amount;
        }
        return total;
    }

    // ═════════════════════════════════════════════════════════════
    //  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ═════════════════════════════════════════════════════════════

    /**
     * Upsert: если лимит уже существует — обновляем amount и period,
     * иначе создаём новый.
     */
    private void upsert(
            @Nullable LimitEntity existing,
            @NonNull String accountId,
            @Nullable String userId,
            @Nullable String tagId,
            double amount,
            @NonNull String period,
            @NonNull LimitCallback callback
    ) {
        String now = isoNow();
        if (existing != null) {
            existing.amountLimit = amount;
            existing.period      = period;
            existing.isDeleted   = false;   // на случай, если был мягко удалён
            existing.isSynced    = false;
            existing.updatedAt   = now;
            database.limitDao().updateLimit(existing);
            deliverSuccess(callback, existing);
        } else {
            LimitEntity limit    = new LimitEntity();
            limit.id             = UUID.randomUUID().toString();
            limit.accountId      = accountId;
            limit.userId         = userId;
            limit.tagId          = tagId;
            limit.amountLimit    = amount;
            limit.period         = period;
            limit.isSynced       = false;
            limit.isDeleted      = false;
            limit.updatedAt      = now;
            database.limitDao().insertLimit(limit);
            deliverSuccess(callback, limit);
        }
    }

    @Nullable
    private LimitEntity findLimitById(@NonNull String limitId) {
        return database.limitDao().getLimitByIdSync(limitId);
    }

    /**
     * Возвращает [dateFrom, dateTo] для текущего периода лимита.
     * Строки в формате ISO 8601 ("yyyy-MM-dd'T'HH:mm:ss'Z'").
     */
    private static String[] periodToDateRange(@NonNull String period) {
        java.util.Calendar cal = java.util.Calendar.getInstance();

        // dateTo = конец текущего дня
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        String dateTo = formatDate(cal.getTime());

        // dateFrom = начало периода
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);

        switch (period) {
            case "DAY":
                // dateFrom = начало текущего дня (уже установлено)
                break;
            case "WEEK":
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                break;
            case "MONTH":
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                break;
            default:
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1); // fallback = месяц
        }
        String dateFrom = formatDate(cal.getTime());
        return new String[]{dateFrom, dateTo};
    }

    private static String formatDate(@NonNull Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(date);
    }

    private static String isoNow() {
        return formatDate(new Date());
    }

    private boolean validate(double amount, @NonNull String period,
                             @NonNull LimitCallback callback) {
        try {
            LimitValidator.validateLimit(amount, period);
            return true;
        } catch (IllegalArgumentException e) {
            deliverFailure(callback, e.getMessage());
            return false;
        }
    }

    /** Проверяет сессию. Возвращает userId или null (callback уже вызван). */
    @Nullable
    private String requireSession(@NonNull LimitCallback callback) {
        try {
            return SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return null;
        }
    }

    private void deliverSuccess(@NonNull LimitCallback callback, @NonNull LimitEntity limit) {
        mainThreadExecutor.execute(() -> callback.onResult(LimitResult.success(limit)));
    }

    private void deliverFailure(@NonNull LimitCallback callback, @NonNull String message) {
        mainThreadExecutor.execute(() -> callback.onResult(LimitResult.failure(message)));
    }

    // ═════════════════════════════════════════════════════════════
    //  LimitResult и LimitCallback
    // ═════════════════════════════════════════════════════════════

    public static class LimitResult {

        private final boolean success;
        @Nullable private final LimitEntity limit;
        @Nullable private final String errorMessage;

        private LimitResult(boolean success,
                            @Nullable LimitEntity limit,
                            @Nullable String errorMessage) {
            this.success = success;
            this.limit = limit;
            this.errorMessage = errorMessage;
        }

        public static LimitResult success(@NonNull LimitEntity limit) {
            return new LimitResult(true, limit, null);
        }

        public static LimitResult failure(@NonNull String errorMessage) {
            return new LimitResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }

        @NonNull
        public LimitEntity getLimit() {
            if (limit == null) throw new IllegalStateException("No limit in failed LimitResult");
            return limit;
        }

        @NonNull
        public String getErrorMessage() {
            if (errorMessage == null) throw new IllegalStateException("No error in successful LimitResult");
            return errorMessage;
        }
    }

    public interface LimitCallback {
        void onResult(@NonNull LimitResult result);
    }
}