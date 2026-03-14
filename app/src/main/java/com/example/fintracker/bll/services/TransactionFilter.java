package com.example.fintracker.bll.services;

import androidx.annotation.Nullable;

/**
 * TransactionFilter — набор необязательных фильтров для выборки транзакций.
 * Строится через Builder. Все поля опциональны — незаданные поля не влияют на запрос.
 *
 * ИСПОЛЬЗОВАНИЕ:
 *
 *   TransactionFilter filter = new TransactionFilter.Builder()
 *       .accountId("uuid-account")
 *       .type("EXPENSE")
 *       .tagId("uuid-tag")
 *       .dateFrom("2025-01-01T00:00:00Z")
 *       .dateTo("2025-12-31T23:59:59Z")
 *       .searchQuery("кофе")   // ищет по title + description, нечёткое совпадение
 *       .build();
 *
 *   transactionService.getFilteredTransactions(filter, result -> { ... });
 *
 * Все заданные фильтры применяются одновременно (AND-логика).
 * searchQuery разбивается на слова — достаточно совпадения любого слова (OR-логика внутри поиска).
 */
public class TransactionFilter {

    /** UUID счёта. Если задан — возвращаются только транзакции этого счёта. */
    @Nullable public final String accountId;

    /**
     * UUID пользователя. Если задан — только транзакции этого пользователя.
     * Полезно для фильтрации трат конкретного участника в совместном счёте.
     */
    @Nullable public final String userId;

    /** UUID тега. Если задан — только транзакции с этим тегом. */
    @Nullable public final String tagId;

    /**
     * Тип транзакции: "INCOME" или "EXPENSE".
     * Если задан — только транзакции указанного типа.
     */
    @Nullable public final String type;

    /**
     * Нижняя граница даты/времени в формате ISO 8601 ("yyyy-MM-dd'T'HH:mm:ss'Z'").
     * Если задана — только транзакции с timestamp >= dateFrom.
     */
    @Nullable public final String dateFrom;

    /**
     * Верхняя граница даты/времени в формате ISO 8601.
     * Если задана — только транзакции с timestamp <= dateTo.
     */
    @Nullable public final String dateTo;

    /**
     * Поисковый запрос. Разбивается на слова; для каждого слова проверяется
     * вхождение (LIKE %слово%) в поля title и description.
     * Транзакция попадает в результат, если хотя бы одно слово найдено хотя бы в одном поле.
     * Регистр не учитывается (SQLite LIKE регистронезависим для ASCII).
     */
    @Nullable public final String searchQuery;

    private TransactionFilter(Builder b) {
        this.accountId   = b.accountId;
        this.userId      = b.userId;
        this.tagId       = b.tagId;
        this.type        = b.type;
        this.dateFrom    = b.dateFrom;
        this.dateTo      = b.dateTo;
        this.searchQuery = b.searchQuery;
    }

    /** Возвращает true, если ни один фильтр не задан. */
    public boolean isEmpty() {
        return accountId == null && userId == null && tagId == null && type == null
                && dateFrom == null && dateTo == null && searchQuery == null;
    }

    public static class Builder {
        @Nullable private String accountId;
        @Nullable private String userId;
        @Nullable private String tagId;
        @Nullable private String type;
        @Nullable private String dateFrom;
        @Nullable private String dateTo;
        @Nullable private String searchQuery;

        public Builder accountId(@Nullable String accountId) {
            this.accountId = accountId; return this;
        }
        public Builder userId(@Nullable String userId) {
            this.userId = userId; return this;
        }
        public Builder tagId(@Nullable String tagId) {
            this.tagId = tagId; return this;
        }
        public Builder type(@Nullable String type) {
            this.type = type; return this;
        }
        public Builder dateFrom(@Nullable String dateFrom) {
            this.dateFrom = dateFrom; return this;
        }
        public Builder dateTo(@Nullable String dateTo) {
            this.dateTo = dateTo; return this;
        }
        public Builder searchQuery(@Nullable String searchQuery) {
            this.searchQuery = searchQuery; return this;
        }
        public TransactionFilter build() {
            return new TransactionFilter(this);
        }
    }
}