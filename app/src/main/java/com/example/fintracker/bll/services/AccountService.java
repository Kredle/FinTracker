package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.validators.AccountValidator;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.repositories.AccountRepository;
import com.example.fintracker.dal.repositories.DataCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AccountService — бизнес-логика для работы со счетами.
 *
 * Все операции требуют активной сессии (залогиненного пользователя).
 *
 * ИСПОЛЬЗОВАНИЕ:
 *
 *   AccountService accountService = new AccountService(application);
 *
 *   // Создать счёт:
 *   accountService.createAccount("Карта", 0.0, result -> {
 *       if (result.isSuccess()) {
 *           AccountEntity acc = result.getAccount();
 *       } else {
 *           showError(result.getErrorMessage());
 *       }
 *   });
 *
 *   // Переименовать счёт:
 *   accountService.renameAccount(accountId, "Новое название", result -> {
 *       if (result.isSuccess()) { ... }
 *   });
 *
 *   // Получить все счета текущего пользователя (LiveData):
 *   accountService.getMyAccounts().observe(this, accounts -> { ... });
 */
public class AccountService {

    private final AppDatabase database;
    private final AccountRepository accountRepository;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public AccountService(@NonNull Application application) {
        this.database = AppDatabase.getInstance(application);
        this.accountRepository = new AccountRepository(application);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public AccountService(
            @NonNull AppDatabase database,
            @NonNull AccountRepository accountRepository,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.database = database;
        this.accountRepository = accountRepository;
        this.executorService = executorService;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    // ─────────────────────────────────────────────────────────────
    //  СОЗДАНИЕ СЧЁТА
    // ─────────────────────────────────────────────────────────────

    /**
     * Создаёт новый счёт для текущего залогиненного пользователя.
     *
     * @param name           Название счёта (1–30 символов, без пробелов по краям)
     * @param initialBalance Начальный баланс (≥ 0)
     * @param callback       Результат операции
     */
    public void createAccount(
            @NonNull String name,
            double initialBalance,
            @NonNull AccountCallback callback
    ) {
        // Проверяем сессию до запуска фона — быстрый сброс без лишнего потока
        String ownerId;
        try {
            ownerId = SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return;
        }

        final String ownerIdFinal = ownerId;

        executorService.execute(() -> {
            // Валидация
            try {
                AccountValidator.validateAccountCreation(name.trim(), initialBalance);
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            // Проверяем дубликат по имени у этого пользователя
            AccountEntity duplicate = database.accountDao()
                    .getAccountByNameAndOwnerSync(name.trim(), ownerIdFinal);
            if (duplicate != null) {
                deliverFailure(callback, "Счёт с таким названием уже существует");
                return;
            }

            String now = isoNow();
            AccountEntity account = new AccountEntity();
            account.id        = UUID.randomUUID().toString();
            account.name      = name.trim();
            account.ownerId   = ownerIdFinal;
            account.isShared  = false;
            account.balance   = initialBalance;
            account.isSynced  = false;
            account.isDeleted = false;
            account.updatedAt = now;

            accountRepository.insertAccount(account, new DataCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void data) {
                    deliverSuccess(callback, account);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка создания счёта: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ПЕРЕИМЕНОВАНИЕ СЧЁТА
    // ─────────────────────────────────────────────────────────────

    /**
     * Переименовывает существующий счёт.
     * Только владелец счёта может его переименовать.
     *
     * @param accountId ID счёта (UUID)
     * @param newName   Новое название (1–30 символов, без пробелов по краям)
     * @param callback  Результат операции
     */
    public void renameAccount(
            @NonNull String accountId,
            @NonNull String newName,
            @NonNull AccountCallback callback
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
            // Валидация нового названия
            try {
                AccountValidator.isValidAccountName(newName.trim());
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            // Получаем счёт синхронно
            AccountEntity account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }

            // Проверяем, что текущий пользователь — владелец
            if (!currentUserIdFinal.equals(account.ownerId)) {
                deliverFailure(callback, "Нет прав для переименования этого счёта");
                return;
            }

            // Проверяем дубликат нового имени (исключаем сам счёт)
            AccountEntity duplicate = database.accountDao()
                    .getAccountByNameAndOwnerSync(newName.trim(), currentUserIdFinal);
            if (duplicate != null && !duplicate.id.equals(accountId)) {
                deliverFailure(callback, "Счёт с таким названием уже существует");
                return;
            }

            account.name      = newName.trim();
            account.isSynced  = false;
            account.updatedAt = isoNow();

            accountRepository.updateAccount(account);
            deliverSuccess(callback, account);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ПОЛУЧЕНИЕ СЧЕТОВ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ
    // ─────────────────────────────────────────────────────────────

    /**
     * Возвращает LiveData со списком счетов текущего залогиненного пользователя.
     * Автоматически обновляется при изменениях в базе.
     *
     * @throws IllegalStateException если пользователь не залогинен
     */
    public LiveData<List<AccountEntity>> getMyAccounts() {
        String userId = SessionManager.getInstance().requireUserId();
        return accountRepository.getAccountsByUserId(userId);
    }

    // ─────────────────────────────────────────────────────────────
    //  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ─────────────────────────────────────────────────────────────

    private void deliverSuccess(AccountCallback callback, AccountEntity account) {
        mainThreadExecutor.execute(() -> callback.onResult(AccountResult.success(account)));
    }

    private void deliverFailure(AccountCallback callback, String message) {
        mainThreadExecutor.execute(() -> callback.onResult(AccountResult.failure(message)));
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    // ─────────────────────────────────────────────────────────────
    //  AccountResult и AccountCallback
    // ─────────────────────────────────────────────────────────────

    /**
     * Результат операции над счётом.
     */
    public static class AccountResult {

        private final boolean success;
        @Nullable private final AccountEntity account;
        @Nullable private final String errorMessage;

        private AccountResult(boolean success,
                              @Nullable AccountEntity account,
                              @Nullable String errorMessage) {
            this.success = success;
            this.account = account;
            this.errorMessage = errorMessage;
        }

        public static AccountResult success(@NonNull AccountEntity account) {
            return new AccountResult(true, account, null);
        }

        public static AccountResult failure(@NonNull String errorMessage) {
            return new AccountResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }

        @NonNull
        public AccountEntity getAccount() {
            if (account == null) throw new IllegalStateException("No account in failed AccountResult");
            return account;
        }

        @NonNull
        public String getErrorMessage() {
            if (errorMessage == null) throw new IllegalStateException("No error in successful AccountResult");
            return errorMessage;
        }
    }

    /**
     * Callback для получения результата операции над счётом.
     */
    public interface AccountCallback {
        void onResult(@NonNull AccountResult result);
    }
}