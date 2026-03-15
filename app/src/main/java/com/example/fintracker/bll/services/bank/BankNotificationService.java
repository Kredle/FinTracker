package com.example.fintracker.bll.services.bank;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.utils.PasswordHasher;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.local.entities.UserEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BankNotificationService — слушает push-уведомления MonoBank и автоматически
 * записывает транзакции в базу данных.
 *
 * ТРЕБОВАНИЯ:
 *   1. Пользователь должен быть залогинен (SessionManager).
 *   2. У пользователя должен быть включён флаг isBankSyncEnabled.
 *   3. Должен быть выбран целевой счёт (сохранён в SharedPreferences).
 *   4. Системное разрешение "Доступ к уведомлениям" должно быть выдано
 *      вручную через Настройки → Приложения → Специальный доступ.
 *
 * НАСТРОЙКА ЦЕЛЕВОГО СЧЁТА:
 *   BankNotificationService.setTargetAccount(context, accountId);
 *
 * ВКЛЮЧЕНИЕ / ОТКЛЮЧЕНИЕ:
 *   Управляется флагом isBankSyncEnabled в UserEntity.
 *   Меняется через UserService или напрямую в базе.
 *
 * ЗАЩИТА ОТ ДУБЛИРОВАНИЯ:
 *   SHA-256 хэш от текста уведомления сохраняется в поле bankMessageHash.
 *   Перед записью проверяется, нет ли уже транзакции с таким хэшем.
 */
public class BankNotificationService extends NotificationListenerService {

    private static final String TAG = "BANK_NOTIFICATION";

    // SharedPreferences — хранит ID целевого счёта
    private static final String PREFS_NAME      = "bank_sync_prefs";
    private static final String KEY_ACCOUNT_ID  = "target_account_id";

    private ExecutorService executor;

    // ─────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "BankNotificationService запущен");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

    // ─────────────────────────────────────────────────────────────
    //  Обработка уведомлений
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        // Фильтруем: только MonoBank
        if (!MonoBankNotificationParser.MONOBANK_PACKAGE.equals(sbn.getPackageName())) return;

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        String title = extras.getString(Notification.EXTRA_TITLE);
        CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (textCs == null) {
            // Пробуем BigText (развёрнутое уведомление)
            textCs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }
        if (textCs == null) return;

        String text = textCs.toString();
        Log.d(TAG, "MonoBank уведомление: title='" + title + "' text='" + text + "'");

        // Обрабатываем в фоне — Room нельзя на главном потоке
        executor.execute(() -> handleNotification(title, text));
    }

    // ─────────────────────────────────────────────────────────────
    //  Логика обработки
    // ─────────────────────────────────────────────────────────────

    private void handleNotification(@Nullable String title, @NonNull String text) {
        // 1. Проверяем сессию
        UserEntity currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Пользователь не залогинен — пропускаем уведомление");
            return;
        }

        // 2. Проверяем, включена ли банковская синхронизация
        if (!currentUser.isBankSyncEnabled) {
            Log.d(TAG, "isBankSyncEnabled = false — пропускаем уведомление");
            return;
        }

        // 3. Получаем целевой счёт
        String accountId = getTargetAccountId(this);
        if (accountId == null) {
            Log.w(TAG, "Целевой счёт не выбран. Вызови BankNotificationService.setTargetAccount()");
            // Fallback: берём первый доступный счёт пользователя
            accountId = findFirstAccountId(currentUser.id);
            if (accountId == null) {
                Log.e(TAG, "У пользователя нет ни одного счёта — транзакция не записана");
                return;
            }
        }

        // 4. Парсим уведомление
        MonoBankNotificationParser.ParsedTransaction parsed =
                MonoBankNotificationParser.parse(title, text);
        if (parsed == null) {
            Log.d(TAG, "Не удалось распарсить уведомление как транзакцию: '" + text + "'");
            return;
        }

        Log.d(TAG, "Распознана транзакция: " + parsed);

        // 5. Вычисляем хэш для защиты от дублей
        String hash = PasswordHasher.hash(parsed.rawText);

        // 6. Проверяем дубликат
        AppDatabase db = AppDatabase.getInstance(this);
        if (isDuplicate(db, hash)) {
            Log.d(TAG, "Дублирующаяся транзакция (hash=" + hash + ") — пропускаем");
            return;
        }

        // 7. Сохраняем транзакцию
        String now = isoNow();
        TransactionEntity tx = new TransactionEntity();
        tx.id              = UUID.randomUUID().toString();
        tx.accountId       = accountId;
        tx.userId          = currentUser.id;
        tx.tagId           = null;          // тег можно назначить вручную позже
        tx.amount          = parsed.amount;
        tx.type            = parsed.type;
        tx.title           = buildTitle(parsed);
        tx.description     = parsed.description;
        tx.timestamp       = now;
        tx.bankMessageHash = hash;
        tx.isSynced        = false;
        tx.isDeleted       = false;
        tx.updatedAt       = now;

        db.transactionDao().insertTransaction(tx);
        Log.i(TAG, "✅ Транзакция записана: " + tx.type + " " + tx.amount
                + " грн | " + tx.title + " | accountId=" + tx.accountId);
    }

    // ─────────────────────────────────────────────────────────────
    //  Вспомогательные методы
    // ─────────────────────────────────────────────────────────────

    /**
     * Проверяет, существует ли уже транзакция с данным bankMessageHash.
     */
    private boolean isDuplicate(@NonNull AppDatabase db, @NonNull String hash) {
        List<TransactionEntity> all = db.transactionDao().getUnsyncedTransactions();
        // Проверяем только среди несинхронизированных (они точно свежие).
        // Для полной проверки нужен отдельный DAO-запрос — добавлен ниже.
        return db.transactionDao().existsByBankMessageHash(hash);
    }

    @Nullable
    private String findFirstAccountId(@NonNull String ownerId) {
        List<AccountEntity> accounts = AppDatabase.getInstance(this)
                .accountDao().getAccountsByUserIdSync(ownerId);
        return accounts.isEmpty() ? null : accounts.get(0).id;
    }

    @NonNull
    private static String buildTitle(@NonNull MonoBankNotificationParser.ParsedTransaction parsed) {
        // Берём первую строку описания как заголовок (обычно это место покупки)
        String[] lines = parsed.description.split("[\\r\\n]");
        String firstLine = lines[0].trim();
        if (firstLine.isEmpty()) {
            return "INCOME".equals(parsed.type) ? "Поповнення MonoBank" : "Покупка MonoBank";
        }
        // Ограничиваем до 50 символов (ограничение TransactionValidator)
        return firstLine.length() > 50 ? firstLine.substring(0, 47) + "..." : firstLine;
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    // ─────────────────────────────────────────────────────────────
    //  Публичное API — включение / отключение
    // ─────────────────────────────────────────────────────────────

    /**
     * Включает компонент сервиса на уровне системы.
     * После вызова система начнёт доставлять уведомления в onNotificationPosted().
     *
     * Вызывай когда пользователь включает isBankSyncEnabled.
     * Предварительно убедись, что разрешение выдано через isNotificationAccessGranted().
     *
     * @param context Application context
     */
    public static void enable(@NonNull Context context) {
        setComponentEnabled(context, true);
        Log.d(TAG, "BankNotificationService включён");
    }

    /**
     * Отключает компонент сервиса на уровне системы.
     * После вызова система перестаёт запускать сервис и доставлять уведомления.
     * Никакие уведомления MonoBank не будут обработаны до следующего вызова enable().
     *
     * Вызывай когда пользователь выключает isBankSyncEnabled.
     *
     * @param context Application context
     */
    public static void disable(@NonNull Context context) {
        setComponentEnabled(context, false);
        Log.d(TAG, "BankNotificationService отключён");
    }

    /**
     * Возвращает true, если компонент сервиса включён на уровне системы.
     * Не гарантирует, что разрешение на уведомления выдано.
     */
    public static boolean isEnabled(@NonNull Context context) {
        ComponentName cn = new ComponentName(context, BankNotificationService.class);
        int state = context.getPackageManager().getComponentEnabledSetting(cn);
        return state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    /**
     * Включает или отключает компонент через PackageManager.
     * Состояние сохраняется между перезапусками приложения — система
     * запоминает его в своей базе.
     */
    private static void setComponentEnabled(@NonNull Context context, boolean enabled) {
        ComponentName cn = new ComponentName(context, BankNotificationService.class);
        int state = enabled
                ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        context.getPackageManager().setComponentEnabledSetting(
                cn, state, android.content.pm.PackageManager.DONT_KILL_APP);
    }

    // ─────────────────────────────────────────────────────────────
    //  Публичное API — настройка счёта и разрешения
    // ─────────────────────────────────────────────────────────────

    /**
     * Сохраняет ID счёта, на который будут записываться банковские транзакции.
     * Вызывай из Activity/ViewModel когда пользователь выбирает счёт.
     *
     * @param context   Application context
     * @param accountId UUID счёта
     */
    public static void setTargetAccount(@NonNull Context context, @NonNull String accountId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_ACCOUNT_ID, accountId).apply();
        Log.d(TAG, "Целевой счёт для банк-синка: " + accountId);
    }

    /**
     * Возвращает сохранённый ID целевого счёта или null.
     */
    @Nullable
    public static String getTargetAccountId(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACCOUNT_ID, null);
    }

    /**
     * Проверяет, выдано ли приложению разрешение на чтение уведомлений.
     * Если false — направь пользователя в Настройки через openNotificationAccessSettings().
     */
    public static boolean isNotificationAccessGranted(@NonNull Context context) {
        ComponentName cn = new ComponentName(context, BankNotificationService.class);
        String flat = android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    /**
     * Открывает системный экран выдачи разрешения на чтение уведомлений.
     * Вызывай когда isNotificationAccessGranted() == false.
     */
    public static void openNotificationAccessSettings(@NonNull Context context) {
        android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}