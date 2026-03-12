package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.validators.TransactionValidator;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.repositories.DataCallback;
import com.example.fintracker.dal.repositories.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TransactionService — бизнес-логика для работы с транзакциями.
 *
 * ИСПОЛЬЗОВАНИЕ:
 *
 *   TransactionService service = new TransactionService(application);
 *
 *   // Добавить транзакцию:
 *   service.addTransaction(accountId, "EXPENSE", "Кофе", 150.0, tagId, "Описание", result -> { ... });
 *
 *   // Удалить транзакцию:
 *   service.deleteTransaction(transactionId, result -> { ... });
 *
 *   // Назначить тег транзакции:
 *   service.assignTag(transactionId, tagId, result -> { ... });
 *
 *   // Убрать тег с транзакции:
 *   service.removeTag(transactionId, result -> { ... });
 *
 *   // Фильтрация (все параметры опциональны, комбинируются):
 *   TransactionFilter filter = new TransactionFilter.Builder()
 *       .accountId("uuid")
 *       .type("EXPENSE")
 *       .tagId("uuid-tag")
 *       .dateFrom("2025-01-01T00:00:00Z")
 *       .dateTo("2025-12-31T23:59:59Z")
 *       .searchQuery("кофе продукты")
 *       .build();
 *   LiveData<List<TransactionEntity>> liveData = service.getFilteredTransactions(filter);
 */
public class TransactionService {

    private final AppDatabase database;
    private final TransactionRepository transactionRepository;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public TransactionService(@NonNull Application application) {
        this.database               = AppDatabase.getInstance(application);
        this.transactionRepository  = new TransactionRepository(application);
        this.executorService        = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor     = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public TransactionService(
            @NonNull AppDatabase database,
            @NonNull TransactionRepository transactionRepository,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.database              = database;
        this.transactionRepository = transactionRepository;
        this.executorService       = executorService;
        this.mainThreadExecutor    = mainThreadExecutor;
    }

    // ─────────────────────────────────────────────────────────────
    //  ДОБАВЛЕНИЕ ТРАНЗАКЦИИ
    // ─────────────────────────────────────────────────────────────

    /**
     * Создаёт новую транзакцию для текущего залогиненного пользователя.
     *
     * @param accountId   UUID счёта (обязательно)
     * @param type        "INCOME" или "EXPENSE" (обязательно)
     * @param title       Название (1–50 символов, обязательно)
     * @param amount      Сумма (> 0, обязательно)
     * @param tagId       UUID тега (null — без тега)
     * @param description Описание (null — без описания)
     * @param callback    Результат операции
     */
    public void addTransaction(
            @NonNull String accountId,
            @NonNull String type,
            @NonNull String title,
            double amount,
            @Nullable String tagId,
            @Nullable String description,
            @NonNull TransactionCallback callback
    ) {
        String userId;
        try {
            userId = SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return;
        }

        final String userIdFinal = userId;

        executorService.execute(() -> {
            // Валидация
            try {
                TransactionValidator.validateTransaction(amount, title.trim());
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
                deliverFailure(callback, "Тип транзакции должен быть INCOME или EXPENSE");
                return;
            }

            // Проверяем, что счёт существует и не удалён
            if (database.accountDao().getAccountByIdSync(accountId) == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }

            // Проверяем тег, если передан
            if (tagId != null && database.tagDao().getTagByIdSync(tagId) == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            String now = isoNow();
            TransactionEntity tx = new TransactionEntity();
            tx.id          = UUID.randomUUID().toString();
            tx.accountId   = accountId;
            tx.userId      = userIdFinal;
            tx.tagId       = tagId;
            tx.amount      = amount;
            tx.type        = type;
            tx.title       = title.trim();
            tx.description = (description != null) ? description.trim() : null;
            tx.timestamp   = now;
            tx.isSynced    = false;
            tx.isDeleted   = false;
            tx.updatedAt   = now;

            transactionRepository.insertTransaction(tx, new DataCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void data) {
                    deliverSuccess(callback, tx);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка сохранения транзакции: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  УДАЛЕНИЕ ТРАНЗАКЦИИ
    // ─────────────────────────────────────────────────────────────

    /**
     * Soft-delete транзакции. Только автор транзакции может её удалить.
     *
     * @param transactionId UUID транзакции
     * @param callback      Результат операции
     */
    public void deleteTransaction(
            @NonNull String transactionId,
            @NonNull TransactionCallback callback
    ) {
        String currentUserId;
        try {
            currentUserId = SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return;
        }

        final String currentUserIdFinal = currentUserId;

        executorService.execute(() -> {
            TransactionEntity tx = database.transactionDao().getTransactionByIdSync(transactionId);
            if (tx == null) {
                deliverFailure(callback, "Транзакция не найдена");
                return;
            }
            if (!currentUserIdFinal.equals(tx.userId)) {
                deliverFailure(callback, "Нет прав для удаления этой транзакции");
                return;
            }

            database.transactionDao().deleteTransaction(transactionId);
            tx.isDeleted = true;
            deliverSuccess(callback, tx);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  НАЗНАЧЕНИЕ ТЕГА ТРАНЗАКЦИИ
    // ─────────────────────────────────────────────────────────────

    /**
     * Назначает тег транзакции. Только автор транзакции может её изменить.
     *
     * @param transactionId UUID транзакции
     * @param tagId         UUID тега
     * @param callback      Результат операции
     */
    public void assignTag(
            @NonNull String transactionId,
            @NonNull String tagId,
            @NonNull TransactionCallback callback
    ) {
        String currentUserId;
        try {
            currentUserId = SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return;
        }

        final String currentUserIdFinal = currentUserId;

        executorService.execute(() -> {
            TransactionEntity tx = database.transactionDao().getTransactionByIdSync(transactionId);
            if (tx == null) {
                deliverFailure(callback, "Транзакция не найдена");
                return;
            }
            if (!currentUserIdFinal.equals(tx.userId)) {
                deliverFailure(callback, "Нет прав для изменения этой транзакции");
                return;
            }
            if (database.tagDao().getTagByIdSync(tagId) == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            tx.tagId     = tagId;
            tx.isSynced  = false;
            tx.updatedAt = isoNow();
            database.transactionDao().updateTransaction(tx);
            deliverSuccess(callback, tx);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  УДАЛЕНИЕ ТЕГА У ТРАНЗАКЦИИ
    // ─────────────────────────────────────────────────────────────

    /**
     * Убирает тег с транзакции (tagId становится null).
     * Только автор транзакции может её изменить.
     *
     * @param transactionId UUID транзакции
     * @param callback      Результат операции
     */
    public void removeTag(
            @NonNull String transactionId,
            @NonNull TransactionCallback callback
    ) {
        String currentUserId;
        try {
            currentUserId = SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return;
        }

        final String currentUserIdFinal = currentUserId;

        executorService.execute(() -> {
            TransactionEntity tx = database.transactionDao().getTransactionByIdSync(transactionId);
            if (tx == null) {
                deliverFailure(callback, "Транзакция не найдена");
                return;
            }
            if (!currentUserIdFinal.equals(tx.userId)) {
                deliverFailure(callback, "Нет прав для изменения этой транзакции");
                return;
            }
            if (tx.tagId == null) {
                deliverFailure(callback, "У транзакции нет тега");
                return;
            }

            tx.tagId     = null;
            tx.isSynced  = false;
            tx.updatedAt = isoNow();
            database.transactionDao().updateTransaction(tx);
            deliverSuccess(callback, tx);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ФИЛЬТРАЦИЯ ТРАНЗАКЦИЙ
    // ─────────────────────────────────────────────────────────────

    /**
     * Возвращает LiveData со списком транзакций, отфильтрованных по любой комбинации
     * параметров из {@link TransactionFilter}.
     *
     * Фильтры (все опциональны, применяются одновременно через AND):
     *   - accountId   — по счёту
     *   - tagId       — по тегу
     *   - type        — по типу ("INCOME" / "EXPENSE")
     *   - dateFrom    — транзакции не раньше этой даты (ISO 8601)
     *   - dateTo      — транзакции не позже этой даты (ISO 8601)
     *   - searchQuery — нечёткий поиск по title и description:
     *                   запрос разбивается на слова, каждое слово проверяется
     *                   через LIKE %слово% в обоих полях (OR внутри слов)
     *
     * LiveData автоматически обновляется при изменении данных в базе.
     *
     * @param filter Набор фильтров (пустой filter вернёт все не удалённые транзакции)
     * @return LiveData со списком транзакций, отсортированных по timestamp DESC
     */
    public LiveData<List<TransactionEntity>> getFilteredTransactions(
            @NonNull TransactionFilter filter
    ) {
        SupportSQLiteQuery query = buildQuery(filter);
        return database.transactionDao().getFilteredTransactions(query);
    }

    /**
     * Синхронная версия фильтрации — для фоновых задач и тестов.
     *
     * @param filter Набор фильтров
     * @return List транзакций
     */
    public List<TransactionEntity> getFilteredTransactionsSync(
            @NonNull TransactionFilter filter
    ) {
        SupportSQLiteQuery query = buildQuery(filter);
        return database.transactionDao().getFilteredTransactionsSync(query);
    }

    // ─────────────────────────────────────────────────────────────
    //  ПОСТРОЕНИЕ ДИНАМИЧЕСКОГО SQL-ЗАПРОСА
    // ─────────────────────────────────────────────────────────────

    /**
     * Строит динамический SQL-запрос из набора фильтров.
     *
     * Логика поиска по searchQuery:
     *   Запрос разбивается на токены по пробелам.
     *   Для каждого токена добавляется условие:
     *     (title LIKE ? OR description LIKE ?)
     *   Токены объединяются через AND, т.е. все слова должны быть найдены
     *   хотя бы в одном из полей (title или description).
     *
     *   Пример: "кофе продукты" →
     *     (title LIKE '%кофе%' OR description LIKE '%кофе%')
     *     AND
     *     (title LIKE '%продукты%' OR description LIKE '%продукты%')
     *
     * Параметры передаются через bind args (защита от SQL-инъекций).
     */
    private static SupportSQLiteQuery buildQuery(@NonNull TransactionFilter f) {
        StringBuilder sql  = new StringBuilder(
                "SELECT * FROM transactions WHERE isDeleted = 0");
        List<Object> args = new ArrayList<>();

        if (f.accountId != null) {
            sql.append(" AND accountId = ?");
            args.add(f.accountId);
        }

        if (f.tagId != null) {
            sql.append(" AND tagId = ?");
            args.add(f.tagId);
        }

        if (f.type != null) {
            sql.append(" AND type = ?");
            args.add(f.type);
        }

        if (f.dateFrom != null) {
            sql.append(" AND timestamp >= ?");
            args.add(f.dateFrom);
        }

        if (f.dateTo != null) {
            sql.append(" AND timestamp <= ?");
            args.add(f.dateTo);
        }

        if (f.searchQuery != null && !f.searchQuery.trim().isEmpty()) {
            String[] tokens = f.searchQuery.trim().split("\\s+");
            for (String token : tokens) {
                // Каждое слово должно встретиться хотя бы в title или description
                sql.append(" AND (title LIKE ? OR description LIKE ?)");
                String like = "%" + token + "%";
                args.add(like);
                args.add(like);
            }
        }

        sql.append(" ORDER BY timestamp DESC");

        return new SimpleSQLiteQuery(sql.toString(), args.toArray());
    }

    // ─────────────────────────────────────────────────────────────
    //  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ─────────────────────────────────────────────────────────────

    private void deliverSuccess(TransactionCallback callback, TransactionEntity tx) {
        mainThreadExecutor.execute(() -> callback.onResult(TransactionResult.success(tx)));
    }

    private void deliverFailure(TransactionCallback callback, String message) {
        mainThreadExecutor.execute(() -> callback.onResult(TransactionResult.failure(message)));
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    // ─────────────────────────────────────────────────────────────
    //  TransactionResult и TransactionCallback
    // ─────────────────────────────────────────────────────────────

    public static class TransactionResult {

        private final boolean success;
        @Nullable private final TransactionEntity transaction;
        @Nullable private final String errorMessage;

        private TransactionResult(boolean success,
                                  @Nullable TransactionEntity transaction,
                                  @Nullable String errorMessage) {
            this.success     = success;
            this.transaction = transaction;
            this.errorMessage = errorMessage;
        }

        public static TransactionResult success(@NonNull TransactionEntity tx) {
            return new TransactionResult(true, tx, null);
        }

        public static TransactionResult failure(@NonNull String errorMessage) {
            return new TransactionResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }

        @NonNull
        public TransactionEntity getTransaction() {
            if (transaction == null)
                throw new IllegalStateException("No transaction in failed TransactionResult");
            return transaction;
        }

        @NonNull
        public String getErrorMessage() {
            if (errorMessage == null)
                throw new IllegalStateException("No error in successful TransactionResult");
            return errorMessage;
        }
    }

    public interface TransactionCallback {
        void onResult(@NonNull TransactionResult result);
    }
}