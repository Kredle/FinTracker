package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.validators.AccountValidator;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.SharedAccountMemberEntity;
import com.example.fintracker.dal.repositories.AccountRepository;
import com.example.fintracker.dal.repositories.DataCallback;
import com.example.fintracker.dal.repositories.SharedAccountMemberRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SharedAccountService — бизнес-логика для работы с совместными счетами.
 *
 * Роли участников:
 *   ADMIN — создатель счёта; может добавлять/удалять участников, удалять счёт.
 *   USER  — обычный участник; только читает и создаёт транзакции.
 *
 * ИСПОЛЬЗОВАНИЕ:
 *
 *   SharedAccountService service = new SharedAccountService(application);
 *
 *   // Создать совместный счёт (текущий юзер становится ADMIN):
 *   service.createSharedAccount("Семейный бюджет", 0.0, result -> { ... });
 *
 *   // Добавить участника (только ADMIN):
 *   service.addMember(accountId, userId, result -> { ... });
 *
 *   // Удалить участника (только ADMIN):
 *   service.removeMember(accountId, userId, result -> { ... });
 *
 *   // Удалить совместный счёт вместе со всеми транзакциями (только ADMIN):
 *   service.deleteSharedAccount(accountId, result -> { ... });
 *
 *   // Получить всех участников счёта:
 *   service.getMembers(accountId).observe(this, members -> { ... });
 *
 *   // Проверить, является ли счёт совместным:
 *   service.isSharedAccount(accountId, result -> { ... });
 *
 *   // Проверить, является ли текущий юзер админом счёта:
 *   service.isCurrentUserAdmin(accountId, result -> { ... });
 */
public class SharedAccountService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER  = "USER";

    private final AppDatabase database;
    private final AccountRepository accountRepository;
    private final SharedAccountMemberRepository memberRepository;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public SharedAccountService(@NonNull Application application) {
        this.database           = AppDatabase.getInstance(application);
        this.accountRepository  = new AccountRepository(application);
        this.memberRepository   = new SharedAccountMemberRepository(application);
        this.executorService    = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public SharedAccountService(
            @NonNull AppDatabase database,
            @NonNull AccountRepository accountRepository,
            @NonNull SharedAccountMemberRepository memberRepository,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.database           = database;
        this.accountRepository  = accountRepository;
        this.memberRepository   = memberRepository;
        this.executorService    = executorService;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    // ─────────────────────────────────────────────────────────────
    //  СОЗДАНИЕ СОВМЕСТНОГО СЧЁТА
    // ─────────────────────────────────────────────────────────────

    /**
     * Создаёт совместный счёт. Текущий залогиненный пользователь автоматически
     * становится участником с ролью ADMIN.
     *
     * @param name           Название счёта (1–30 символов)
     * @param initialBalance Начальный баланс (≥ 0)
     * @param callback       Результат операции
     */
    public void createSharedAccount(
            @NonNull String name,
            double initialBalance,
            @NonNull SharedAccountCallback callback
    ) {
        String ownerId;
        try {
            ownerId = SessionManager.getInstance().requireUserId();
        } catch (IllegalStateException e) {
            deliverFailure(callback, "Необходимо войти в аккаунт");
            return;
        }

        final String ownerIdFinal = ownerId;

        executorService.execute(() -> {
            try {
                AccountValidator.validateAccountCreation(name.trim(), initialBalance);
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            String now = isoNow();

            // Создаём сам счёт
            AccountEntity account = new AccountEntity();
            account.id        = UUID.randomUUID().toString();
            account.name      = name.trim();
            account.ownerId   = ownerIdFinal;
            account.isShared  = true;
            account.balance   = initialBalance;
            account.isSynced  = false;
            account.isDeleted = false;
            account.updatedAt = now;

            try {
                database.accountDao().insertAccount(account);
            } catch (Exception e) {
                deliverFailure(callback, "Ошибка создания счёта: " + e.getMessage());
                return;
            }

            // Создатель становится ADMIN
            SharedAccountMemberEntity adminMember =
                    buildMember(account.id, ownerIdFinal, ROLE_ADMIN, now);
            try {
                database.sharedAccountMemberDao().addMember(adminMember);
            } catch (Exception e) {
                // Откатываем счёт
                database.accountDao().deleteAccount(account.id);
                deliverFailure(callback, "Ошибка добавления создателя в счёт: " + e.getMessage());
                return;
            }

            deliverSuccess(callback, SharedAccountResult.ofAccount(account));
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ДОБАВЛЕНИЕ УЧАСТНИКА
    // ─────────────────────────────────────────────────────────────

    /**
     * Добавляет пользователя в совместный счёт с ролью USER.
     * Доступно только для ADMIN счёта.
     *
     * @param accountId    ID совместного счёта
     * @param targetUserId ID добавляемого пользователя
     * @param callback     Результат операции
     */
    public void addMember(
            @NonNull String accountId,
            @NonNull String targetUserId,
            @NonNull SharedAccountCallback callback
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
            AccountEntity account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            if (!account.isShared) {
                deliverFailure(callback, "Счёт не является совместным");
                return;
            }
            if (!isAdmin(accountId, currentUserIdFinal)) {
                deliverFailure(callback, "Только администратор может добавлять участников");
                return;
            }
            if (database.userDao().getUserByIdSync(targetUserId) == null) {
                deliverFailure(callback, "Пользователь не найден");
                return;
            }

            SharedAccountMemberEntity existing =
                    database.sharedAccountMemberDao().getMemberSync(accountId, targetUserId);
            if (existing != null) {
                deliverFailure(callback, "Пользователь уже является участником этого счёта");
                return;
            }

            SharedAccountMemberEntity member =
                    buildMember(accountId, targetUserId, ROLE_USER, isoNow());

            memberRepository.addMember(member, new DataCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void data) {
                    deliverSuccess(callback, SharedAccountResult.ofMember(member));
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка добавления участника: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  УДАЛЕНИЕ УЧАСТНИКА
    // ─────────────────────────────────────────────────────────────

    /**
     * Удаляет пользователя из совместного счёта.
     * Доступно только для ADMIN. Нельзя удалить последнего ADMIN.
     *
     * @param accountId    ID совместного счёта
     * @param targetUserId ID удаляемого пользователя
     * @param callback     Результат операции
     */
    public void removeMember(
            @NonNull String accountId,
            @NonNull String targetUserId,
            @NonNull SharedAccountCallback callback
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
            AccountEntity account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            if (!account.isShared) {
                deliverFailure(callback, "Счёт не является совместным");
                return;
            }
            if (!isAdmin(accountId, currentUserIdFinal)) {
                deliverFailure(callback, "Только администратор может удалять участников");
                return;
            }

            // Защита от удаления последнего ADMIN
            if (currentUserIdFinal.equals(targetUserId)) {
                long adminCount = database.sharedAccountMemberDao()
                        .getMembersForAccountSync(accountId)
                        .stream()
                        .filter(m -> ROLE_ADMIN.equals(m.role) && !m.isDeleted)
                        .count();
                if (adminCount <= 1) {
                    deliverFailure(callback,
                            "Нельзя покинуть счёт: вы единственный администратор. " +
                                    "Назначьте другого администратора или удалите счёт.");
                    return;
                }
            }

            memberRepository.removeMember(accountId, targetUserId, new DataCallback<Integer>() {
                @Override
                public void onSuccess(@Nullable Integer rowsAffected) {
                    if (rowsAffected == null || rowsAffected == 0) {
                        deliverFailure(callback, "Участник не найден в этом счёте");
                    } else {
                        deliverSuccess(callback, SharedAccountResult.empty());
                    }
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка удаления участника: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ПОВЫШЕНИЕ УЧАСТНИКА ДО АДМИНА
    // ─────────────────────────────────────────────────────────────

    /**
     * Повышает участника совместного счёта до роли ADMIN.
     * Доступно только для ADMIN счёта.
     *
     * @param accountId    ID совместного счёта
     * @param targetUserId ID пользователя, которого нужно повысить
     * @param callback     Результат операции
     */
    public void promoteToAdmin(
            @NonNull String accountId,
            @NonNull String targetUserId,
            @NonNull SharedAccountCallback callback
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
            AccountEntity account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            if (!account.isShared) {
                deliverFailure(callback, "Счёт не является совместным");
                return;
            }
            if (!isAdmin(accountId, currentUserIdFinal)) {
                deliverFailure(callback, "Только администратор может повышать участников");
                return;
            }

            // Проверить, что целевой пользователь является участником
            SharedAccountMemberEntity member = database.sharedAccountMemberDao().getMemberSync(accountId, targetUserId);
            if (member == null) {
                deliverFailure(callback, "Пользователь не является участником этого счёта");
                return;
            }

            // Проверить, что он ещё не админ
            if (ROLE_ADMIN.equals(member.role)) {
                deliverFailure(callback, "Пользователь уже является администратором");
                return;
            }

            // Обновить роль
            memberRepository.updateMemberRole(accountId, targetUserId, ROLE_ADMIN, new DataCallback<Integer>() {
                @Override
                public void onSuccess(@Nullable Integer rowsAffected) {
                    if (rowsAffected == null || rowsAffected == 0) {
                        deliverFailure(callback, "Не удалось обновить роль участника");
                    } else {
                        deliverSuccess(callback, SharedAccountResult.empty());
                    }
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка повышения участника: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  УДАЛЕНИЕ СОВМЕСТНОГО СЧЁТА
    // ─────────────────────────────────────────────────────────────

    /**
     * Удаляет совместный счёт вместе со всеми связанными транзакциями.
     * Доступно только для ADMIN.
     *
     * Что происходит:
     *  1. Soft-delete всех транзакций счёта (isDeleted=true, isSynced=false)
     *  2. Soft-delete всех записей участников счёта
     *  3. Soft-delete самого счёта
     * WorkManager подхватит изменения и синхронизирует удаление с Firebase.
     *
     * @param accountId ID совместного счёта
     * @param callback  Результат операции
     */
    public void deleteSharedAccount(
            @NonNull String accountId,
            @NonNull SharedAccountCallback callback
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
            AccountEntity account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            if (!account.isShared) {
                deliverFailure(callback, "Счёт не является совместным");
                return;
            }
            if (!isAdmin(accountId, currentUserIdFinal)) {
                deliverFailure(callback, "Только администратор может удалить совместный счёт");
                return;
            }

            String now = isoNow();

            // 1. Soft-delete всех транзакций счёта
            AccountService.softDeleteTransactions(database, accountId, now);

            // 2. Soft-delete всех участников
            List<SharedAccountMemberEntity> members =
                    database.sharedAccountMemberDao().getMembersForAccountSync(accountId);
            for (SharedAccountMemberEntity member : members) {
                member.isDeleted = true;
                member.isSynced  = false;
                member.updatedAt = now;
                database.sharedAccountMemberDao().updateSharedAccountMember(member);
            }

            // 3. Soft-delete самого счёта
            account.isDeleted = true;
            account.isSynced  = false;
            account.updatedAt = now;
            database.accountDao().updateAccount(account);

            deliverSuccess(callback, SharedAccountResult.empty());
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ПРОСМОТР УЧАСТНИКОВ
    // ─────────────────────────────────────────────────────────────

    /**
     * Возвращает LiveData со списком всех активных участников совместного счёта.
     *
     * @param accountId ID совместного счёта
     */
    public LiveData<List<SharedAccountMemberEntity>> getMembers(@NonNull String accountId) {
        return memberRepository.getMembersForAccount(accountId);
    }

    // ─────────────────────────────────────────────────────────────
    //  ПРОВЕРКИ: совместный / админ
    // ─────────────────────────────────────────────────────────────

    /**
     * Асинхронно проверяет, является ли счёт совместным (isShared == true).
     *
     * @param accountId ID счёта
     * @param callback  Результат: result.isShared()
     */
    public void isSharedAccount(
            @NonNull String accountId,
            @NonNull SharedAccountCallback callback
    ) {
        executorService.execute(() -> {
            AccountEntity account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            deliverSuccess(callback, SharedAccountResult.ofSharedFlag(account.isShared));
        });
    }

    /**
     * Асинхронно проверяет, является ли текущий залогиненный пользователь
     * администратором (ADMIN) данного счёта.
     *
     * @param accountId ID счёта
     * @param callback  Результат: result.isAdmin()
     */
    public void isCurrentUserAdmin(
            @NonNull String accountId,
            @NonNull SharedAccountCallback callback
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
            AccountEntity account = database.accountDao().getAccountByIdSync(accountId);
            if (account == null) {
                deliverFailure(callback, "Счёт не найден");
                return;
            }
            boolean admin = isAdmin(accountId, currentUserIdFinal);
            deliverSuccess(callback, SharedAccountResult.ofAdminFlag(admin));
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ─────────────────────────────────────────────────────────────

    private boolean isAdmin(@NonNull String accountId, @NonNull String userId) {
        SharedAccountMemberEntity member =
                database.sharedAccountMemberDao().getMemberSync(accountId, userId);
        return member != null && ROLE_ADMIN.equals(member.role);
    }

    private static SharedAccountMemberEntity buildMember(
            @NonNull String accountId,
            @NonNull String userId,
            @NonNull String role,
            @NonNull String now
    ) {
        SharedAccountMemberEntity member = new SharedAccountMemberEntity();
        member.id        = UUID.randomUUID().toString();
        member.accountId = accountId;
        member.userId    = userId;
        member.role      = role;
        member.isSynced  = false;
        member.isDeleted = false;
        member.updatedAt = now;
        return member;
    }

    private void deliverSuccess(SharedAccountCallback callback, SharedAccountResult result) {
        mainThreadExecutor.execute(() -> callback.onResult(result));
    }

    private void deliverFailure(SharedAccountCallback callback, String message) {
        mainThreadExecutor.execute(() -> callback.onResult(SharedAccountResult.failure(message)));
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    // ─────────────────────────────────────────────────────────────
    //  SharedAccountResult и SharedAccountCallback
    // ─────────────────────────────────────────────────────────────

    public static class SharedAccountResult {

        private final boolean success;
        @Nullable private final String errorMessage;
        @Nullable private final AccountEntity account;
        @Nullable private final SharedAccountMemberEntity member;
        @Nullable private final Boolean sharedFlag;
        @Nullable private final Boolean adminFlag;

        private SharedAccountResult(
                boolean success,
                @Nullable String errorMessage,
                @Nullable AccountEntity account,
                @Nullable SharedAccountMemberEntity member,
                @Nullable Boolean sharedFlag,
                @Nullable Boolean adminFlag
        ) {
            this.success      = success;
            this.errorMessage = errorMessage;
            this.account      = account;
            this.member       = member;
            this.sharedFlag   = sharedFlag;
            this.adminFlag    = adminFlag;
        }

        static SharedAccountResult ofAccount(@NonNull AccountEntity account) {
            return new SharedAccountResult(true, null, account, null, null, null);
        }

        static SharedAccountResult ofMember(@NonNull SharedAccountMemberEntity member) {
            return new SharedAccountResult(true, null, null, member, null, null);
        }

        static SharedAccountResult ofSharedFlag(boolean isShared) {
            return new SharedAccountResult(true, null, null, null, isShared, null);
        }

        static SharedAccountResult ofAdminFlag(boolean isAdmin) {
            return new SharedAccountResult(true, null, null, null, null, isAdmin);
        }

        static SharedAccountResult empty() {
            return new SharedAccountResult(true, null, null, null, null, null);
        }

        public static SharedAccountResult failure(@NonNull String errorMessage) {
            return new SharedAccountResult(false, errorMessage, null, null, null, null);
        }

        public boolean isSuccess() { return success; }

        @NonNull
        public String getErrorMessage() {
            if (errorMessage == null) throw new IllegalStateException("No error in successful result");
            return errorMessage;
        }

        /** Доступен после createSharedAccount(). */
        @NonNull
        public AccountEntity getAccount() {
            if (account == null) throw new IllegalStateException("No account in this result");
            return account;
        }

        /** Доступен после addMember(). */
        @NonNull
        public SharedAccountMemberEntity getMember() {
            if (member == null) throw new IllegalStateException("No member in this result");
            return member;
        }

        /** Доступен после isSharedAccount(). */
        public boolean isShared() {
            if (sharedFlag == null) throw new IllegalStateException("No sharedFlag in this result");
            return sharedFlag;
        }

        /** Доступен после isCurrentUserAdmin(). */
        public boolean isAdmin() {
            if (adminFlag == null) throw new IllegalStateException("No adminFlag in this result");
            return adminFlag;
        }
    }

    public interface SharedAccountCallback {
        void onResult(@NonNull SharedAccountResult result);
    }
}

