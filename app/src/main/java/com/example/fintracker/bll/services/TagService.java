package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.bll.validators.TagValidator;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.repositories.DataCallback;
import com.example.fintracker.dal.repositories.TagRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TagService — бизнес-логика для работы с тегами (категориями транзакций).
 *
 * Все операции изменения/удаления требуют активной сессии и проверяют,
 * что пользователь является владельцем тега. Системные теги (ownerId == null)
 * изменять нельзя.
 *
 * ИСПОЛЬЗОВАНИЕ:
 *
 *   TagService tagService = new TagService(application);
 *
 *   // Создать тег:
 *   tagService.createTag("Еда", "ic_food", result -> {
 *       if (result.isSuccess()) { ... }
 *       else { showError(result.getErrorMessage()); }
 *   });
 *
 *   // Переименовать тег:
 *   tagService.renameTag(tagId, "Продукты", result -> { ... });
 *
 *   // Сменить иконку:
 *   tagService.changeTagIcon(tagId, "ic_grocery", result -> { ... });
 *
 *   // Переименовать и сменить иконку за один вызов:
 *   tagService.updateTag(tagId, "Продукты", "ic_grocery", result -> { ... });
 *
 *   // Удалить тег (soft-delete):
 *   tagService.deleteTag(tagId, result -> { ... });
 *
 *   // Получить все доступные теги (пользовательские + системные):
 *   tagService.getAvailableTags().observe(this, tags -> { ... });
 */
public class TagService {

    private final AppDatabase database;
    private final TagRepository tagRepository;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;

    public TagService(@NonNull Application application) {
        this.database = AppDatabase.getInstance(application);
        this.tagRepository = new TagRepository(application);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new android.os.Handler(
                android.os.Looper.getMainLooper())::post;
    }

    // Конструктор для тестов
    public TagService(
            @NonNull AppDatabase database,
            @NonNull TagRepository tagRepository,
            @NonNull ExecutorService executorService,
            @NonNull Executor mainThreadExecutor
    ) {
        this.database = database;
        this.tagRepository = tagRepository;
        this.executorService = executorService;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    // ─────────────────────────────────────────────────────────────
    //  СОЗДАНИЕ ТЕГА
    // ─────────────────────────────────────────────────────────────

    /**
     * Создаёт новый пользовательский тег.
     *
     * @param name     Название тега (1–20 символов, без пробелов по краям)
     * @param iconName Имя иконки (может быть null)
     * @param callback Результат операции
     */
    public void createTag(
            @NonNull String name,
            @Nullable String iconName,
            @NonNull TagCallback callback
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
            // Валидация названия
            try {
                TagValidator.validateTagCreation(name.trim());
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            // Проверяем дубликат по имени у этого пользователя
            TagEntity duplicate = database.tagDao()
                    .getTagByNameAndOwnerSync(name.trim(), ownerIdFinal);
            if (duplicate != null) {
                deliverFailure(callback, "Тег с таким названием уже существует");
                return;
            }

            String now = isoNow();
            TagEntity tag = new TagEntity();
            tag.id        = UUID.randomUUID().toString();
            tag.name      = name.trim();
            tag.iconName  = (iconName != null) ? iconName.trim() : null;
            tag.ownerId   = ownerIdFinal;
            tag.isSynced  = false;
            tag.isDeleted = false;
            tag.updatedAt = now;

            tagRepository.insertTag(tag, new DataCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void data) {
                    deliverSuccess(callback, tag);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка создания тега: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ПЕРЕИМЕНОВАНИЕ ТЕГА
    // ─────────────────────────────────────────────────────────────

    /**
     * Переименовывает тег. Системные теги (ownerId == null) переименовать нельзя.
     *
     * @param tagId    ID тега (UUID)
     * @param newName  Новое название (1–20 символов, без пробелов по краям)
     * @param callback Результат операции
     */
    public void renameTag(
            @NonNull String tagId,
            @NonNull String newName,
            @NonNull TagCallback callback
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
            try {
                TagValidator.isValidTagName(newName.trim());
            } catch (IllegalArgumentException e) {
                deliverFailure(callback, e.getMessage());
                return;
            }

            TagEntity tag = database.tagDao().getTagByIdSync(tagId);
            if (tag == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            if (!checkOwnership(tag, currentUserIdFinal, callback)) return;

            // Проверяем дубликат нового имени (исключаем сам тег)
            TagEntity duplicate = database.tagDao()
                    .getTagByNameAndOwnerSync(newName.trim(), currentUserIdFinal);
            if (duplicate != null && !duplicate.id.equals(tagId)) {
                deliverFailure(callback, "Тег с таким названием уже существует");
                return;
            }

            tag.name      = newName.trim();
            tag.isSynced  = false;
            tag.updatedAt = isoNow();

            saveUpdate(tag, callback);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  СМЕНА ИКОНКИ ТЕГА
    // ─────────────────────────────────────────────────────────────

    /**
     * Меняет иконку тега.
     *
     * @param tagId       ID тега (UUID)
     * @param newIconName Новое имя иконки (может быть null — убрать иконку)
     * @param callback    Результат операции
     */
    public void changeTagIcon(
            @NonNull String tagId,
            @Nullable String newIconName,
            @NonNull TagCallback callback
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
            TagEntity tag = database.tagDao().getTagByIdSync(tagId);
            if (tag == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            if (!checkOwnership(tag, currentUserIdFinal, callback)) return;

            tag.iconName  = (newIconName != null) ? newIconName.trim() : null;
            tag.isSynced  = false;
            tag.updatedAt = isoNow();

            saveUpdate(tag, callback);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ОБНОВЛЕНИЕ ТЕГА (имя + иконка за один вызов)
    // ─────────────────────────────────────────────────────────────

    /**
     * Обновляет название и/или иконку тега за один вызов.
     * Если newName или newIconName == null, соответствующее поле остаётся без изменений.
     *
     * @param tagId       ID тега (UUID)
     * @param newName     Новое название (null — не менять)
     * @param newIconName Новое имя иконки (null — не менять; передайте "" чтобы убрать иконку)
     * @param callback    Результат операции
     */
    public void updateTag(
            @NonNull String tagId,
            @Nullable String newName,
            @Nullable String newIconName,
            @NonNull TagCallback callback
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
            // Если оба поля null — нечего делать
            if (newName == null && newIconName == null) {
                deliverFailure(callback, "Не указано ни одно поле для обновления");
                return;
            }

            // Валидация нового имени (если передано)
            if (newName != null) {
                try {
                    TagValidator.isValidTagName(newName.trim());
                } catch (IllegalArgumentException e) {
                    deliverFailure(callback, e.getMessage());
                    return;
                }
            }

            TagEntity tag = database.tagDao().getTagByIdSync(tagId);
            if (tag == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            if (!checkOwnership(tag, currentUserIdFinal, callback)) return;

            // Проверяем дубликат нового имени (если имя меняется)
            if (newName != null) {
                TagEntity duplicate = database.tagDao()
                        .getTagByNameAndOwnerSync(newName.trim(), currentUserIdFinal);
                if (duplicate != null && !duplicate.id.equals(tagId)) {
                    deliverFailure(callback, "Тег с таким названием уже существует");
                    return;
                }
                tag.name = newName.trim();
            }

            // Обновляем иконку:
            //   newIconName == null  → не трогаем
            //   newIconName == ""    → убираем иконку
            //   newIconName == "..." → устанавливаем новую
            if (newIconName != null) {
                tag.iconName = newIconName.trim().isEmpty() ? null : newIconName.trim();
            }

            tag.isSynced  = false;
            tag.updatedAt = isoNow();

            saveUpdate(tag, callback);
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  УДАЛЕНИЕ ТЕГА (soft-delete)
    // ─────────────────────────────────────────────────────────────

    /**
     * Мягко удаляет тег (устанавливает isDeleted = true).
     * Удалённый тег скрывается из UI и не применяется к новым транзакциям,
     * но существующие транзакции с этим тегом не затрагиваются.
     * Данные сохраняются в базе для синхронизации удаления с Firebase.
     *
     * @param tagId    ID тега (UUID)
     * @param callback Результат операции
     */
    public void deleteTag(
            @NonNull String tagId,
            @NonNull TagCallback callback
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
            TagEntity tag = database.tagDao().getTagByIdSync(tagId);
            if (tag == null) {
                deliverFailure(callback, "Тег не найден");
                return;
            }

            if (!checkOwnership(tag, currentUserIdFinal, callback)) return;

            // Помечаем для синхронизации удаления в Firebase перед soft-delete
            tag.isDeleted = true;
            tag.isSynced  = false;
            tag.updatedAt = isoNow();

            tagRepository.updateTag(tag, new DataCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void data) {
                    deliverSuccess(callback, tag);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    deliverFailure(callback, "Ошибка удаления тега: " + throwable.getMessage());
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  ПОЛУЧЕНИЕ ТЕГОВ
    // ─────────────────────────────────────────────────────────────

    /**
     * Возвращает LiveData со всеми доступными тегами:
     * пользовательские + системные (ownerId == null).
     *
     * @throws IllegalStateException если пользователь не залогинен
     */
    public LiveData<List<TagEntity>> getAvailableTags() {
        String userId = SessionManager.getInstance().requireUserId();
        return tagRepository.getAllAvailableTags(userId);
    }

    /**
     * Возвращает LiveData только с пользовательскими тегами (созданными текущим юзером).
     *
     * @throws IllegalStateException если пользователь не залогинен
     */
    public LiveData<List<TagEntity>> getMyTags() {
        String userId = SessionManager.getInstance().requireUserId();
        return tagRepository.getTagsByUserId(userId);
    }

    /**
     * Возвращает LiveData с системными тегами (доступны всем пользователям).
     */
    public LiveData<List<TagEntity>> getDefaultTags() {
        return tagRepository.getDefaultTags();
    }

    // ─────────────────────────────────────────────────────────────
    //  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ─────────────────────────────────────────────────────────────

    /**
     * Проверяет, что текущий пользователь является владельцем тега.
     * Системные теги (ownerId == null или "") нельзя изменять никому.
     * Возвращает true если проверка прошла, false — если отказано (callback уже вызван).
     */
    private boolean checkOwnership(
            @NonNull TagEntity tag,
            @NonNull String currentUserId,
            @NonNull TagCallback callback
    ) {
        if (tag.ownerId == null || tag.ownerId.isEmpty()) {
            deliverFailure(callback, "Системные теги нельзя изменять");
            return false;
        }
        if (!currentUserId.equals(tag.ownerId)) {
            deliverFailure(callback, "Нет прав для изменения этого тега");
            return false;
        }
        return true;
    }

    /** Сохраняет обновлённый тег через репозиторий и доставляет результат. */
    private void saveUpdate(@NonNull TagEntity tag, @NonNull TagCallback callback) {
        tagRepository.updateTag(tag, new DataCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void data) {
                deliverSuccess(callback, tag);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                deliverFailure(callback, "Ошибка сохранения тега: " + throwable.getMessage());
            }
        });
    }

    private void deliverSuccess(TagCallback callback, TagEntity tag) {
        mainThreadExecutor.execute(() -> callback.onResult(TagResult.success(tag)));
    }

    private void deliverFailure(TagCallback callback, String message) {
        mainThreadExecutor.execute(() -> callback.onResult(TagResult.failure(message)));
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    // ─────────────────────────────────────────────────────────────
    //  TagResult и TagCallback
    // ─────────────────────────────────────────────────────────────

    /**
     * Результат операции над тегом.
     */
    public static class TagResult {

        private final boolean success;
        @Nullable private final TagEntity tag;
        @Nullable private final String errorMessage;

        private TagResult(boolean success,
                          @Nullable TagEntity tag,
                          @Nullable String errorMessage) {
            this.success = success;
            this.tag = tag;
            this.errorMessage = errorMessage;
        }

        public static TagResult success(@NonNull TagEntity tag) {
            return new TagResult(true, tag, null);
        }

        public static TagResult failure(@NonNull String errorMessage) {
            return new TagResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }

        @NonNull
        public TagEntity getTag() {
            if (tag == null) throw new IllegalStateException("No tag in failed TagResult");
            return tag;
        }

        @NonNull
        public String getErrorMessage() {
            if (errorMessage == null) throw new IllegalStateException("No error in successful TagResult");
            return errorMessage;
        }
    }

    /**
     * Callback для получения результата операции над тегом.
     */
    public interface TagCallback {
        void onResult(@NonNull TagResult result);
    }
}