package com.example.fintracker.bll.services.bank;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MonoBankNotificationParser — парсит реальные push-уведомления MonoBank.
 *
 * ─────────────────────────────────────────────────────────────────
 * РЕАЛЬНЫЕ ФОРМАТЫ УВЕДОМЛЕНИЙ:
 *
 *   Пополнение (INCOME):
 *     title: "👆💳 1.00₴"
 *     text:  "Від: Максим Задорожний\nБаланс 1 257.57₴"
 *     Признак: text начинается с "Від:"
 *
 *   Трата (EXPENSE):
 *     title: "🚕 20.00₴ • 7 год"
 *     text:  "Львівавтодор Баланс 1 219.17₴"
 *     Признак: text НЕ содержит "Від:", содержит название магазина + "Баланс"
 *
 *   Трата с кешбеком (EXPENSE, кешбек игнорируется):
 *     title: "🍞 77.20₴ Кешбек 0.77₴ • 19 год"
 *     text:  "АТБ Баланс 517.22₴"
 *     Признак: title содержит "Кешбек" → берём только первую сумму
 *
 * ─────────────────────────────────────────────────────────────────
 * ЧТО НЕ ЗАПИСЫВАЕМ:
 *   - Кешбек (отдельные уведомления о начислении кешбека, если есть)
 *   - Уведомления без суммы
 * ─────────────────────────────────────────────────────────────────
 */
public class MonoBankNotificationParser {

    public static final String MONOBANK_PACKAGE = "com.myflexapp.monobank";

    // Сумма в формате "77.20₴" или "1 257.57₴" (пробел-разделитель тысяч)
    // Захватывает: целую часть (с возможным пробелом) + дробную + знак ₴
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(\\d{1,3}(?:\\s\\d{3})*(?:[.,]\\d{1,2})?)\\s*₴"
    );

    // "Баланс 1 219.17₴" — остаток на счёте (нужно для отсечения от merchant name)
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
            "(?i)баланс\\s+[\\d\\s.,]+₴"
    );

    // Кешбек в title: "Кешбек 0.77₴"
    private static final Pattern CASHBACK_PATTERN = Pattern.compile(
            "(?i)кешбек\\s+[\\d.,]+\\s*₴"
    );

    // Время в title: "• 7 год" / "• 15 хв" — отсекаем при парсинге
    private static final Pattern TIME_SUFFIX_PATTERN = Pattern.compile(
            "•\\s*\\d+\\s*(год|хв|с).*$"
    );

    private MonoBankNotificationParser() {}

    /**
     * Парсит уведомление MonoBank.
     *
     * @param title Заголовок уведомления (EXTRA_TITLE)
     * @param text  Текст уведомления (EXTRA_TEXT или EXTRA_BIG_TEXT)
     * @return ParsedTransaction или null если уведомление не является транзакцией
     */
    @Nullable
    public static ParsedTransaction parse(@Nullable String title, @Nullable String text) {
        if (title == null && text == null) return null;
        String t = title != null ? title : "";
        String b = text  != null ? text  : "";

        // ── 1. Пропускаем уведомления о кешбеке без суммы покупки ──
        // Отдельное уведомление о кешбеке имеет вид:
        //   title: "Кешбек 5.00₴"   text: "Нараховано кешбек"
        // У него в title нет суммы покупки ДО слова "Кешбек"
        if (isCashbackOnly(t, b)) return null;

        // ── 2. Определяем тип по тексту ─────────────────────────────
        String type = determineType(t, b);

        // ── 3. Извлекаем сумму из title ─────────────────────────────
        // Если в title есть "Кешбек" — берём только первую сумму (до него)
        String titleForAmount = CASHBACK_PATTERN.matcher(t).replaceAll("").trim();
        titleForAmount = TIME_SUFFIX_PATTERN.matcher(titleForAmount).replaceAll("").trim();

        Double amount = extractFirstAmount(titleForAmount);
        if (amount == null || amount <= 0) return null;

        // ── 4. Извлекаем описание (название магазина / отправителя) ──
        String description = extractDescription(b, type);

        return new ParsedTransaction(amount, type, description, t + "\n" + b);
    }

    // ─────────────────────────────────────────────────────────────
    //  Вспомогательные методы
    // ─────────────────────────────────────────────────────────────

    /**
     * Определяет тип транзакции по тексту уведомления.
     *
     * Пополнение: text начинается с "Від:" (с учётом пробелов и эмодзи)
     * Трата: всё остальное
     */
    @NonNull
    private static String determineType(@NonNull String title, @NonNull String text) {
        // Убираем ведущие пробелы и эмодзи перед проверкой
        String trimmed = text.trim();
        String trimmedT = title.trim();
        if (trimmed.toLowerCase().startsWith("від:") || trimmedT.toLowerCase().startsWith("\uD83D\uDC49")) return "INCOME";
        return "EXPENSE";
    }

    /**
     * Проверяет, является ли уведомление уведомлением ТОЛЬКО о кешбеке
     * (без суммы покупки).
     *
     * Признак: в title нет ни одной суммы ДО слова "Кешбек",
     * или text содержит "нараховано кешбек".
     */
    private static boolean isCashbackOnly(@NonNull String title, @NonNull String text) {
        String tLower = title.toLowerCase();
        String bLower = text.toLowerCase();

        // Явный текст о начислении кешбека
        if (bLower.contains("нараховано кешбек") || bLower.contains("cashback")) {
            return true;
        }

        // В title есть "кешбек", но НЕТ суммы покупки перед ним
        if (tLower.contains("кешбек")) {
            // Берём часть title ДО слова "кешбек"
            int idx = tLower.indexOf("кешбек");
            String beforeCashback = title.substring(0, idx);
            // Если до "Кешбек" нет ни одной суммы — это чисто кешбек-уведомление
            return extractFirstAmount(beforeCashback) == null;
        }

        return false;
    }

    /**
     * Извлекает первую сумму вида "77.20₴" или "1 257.57₴" из строки.
     */
    @Nullable
    private static Double extractFirstAmount(@NonNull String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (!m.find()) return null;
        String raw = m.group(1)
                .replace(" ", "")   // убираем пробел-разделитель тысяч
                .replace(",", "."); // нормализуем десятичный разделитель
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Извлекает описание транзакции из текста уведомления.
     *
     * Для INCOME:  "Від: Максим Задорожний\nБаланс ..." → "Від: Максим Задорожний"
     * Для EXPENSE: "Львівавтодор Баланс 1 219.17₴"     → "Львівавтодор"
     */
    @NonNull
    private static String extractDescription(@NonNull String text, @NonNull String type) {
        // Убираем строку с балансом
        String withoutBalance = BALANCE_PATTERN.matcher(text).replaceAll("").trim();

        if ("INCOME".equals(type)) {
            // Берём первую строку ("Від: Ім'я")
            String[] lines = withoutBalance.split("[\\r\\n]+");
            return lines[0].trim().isEmpty() ? "Поповнення" : lines[0].trim();
        } else {
            // Берём всё до "Баланс" — это название магазина
            // Убираем переносы строк
            String oneLine = withoutBalance.replaceAll("[\\r\\n]+", " ").trim();
            return oneLine.isEmpty() ? "Покупка" : oneLine;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ParsedTransaction
    // ─────────────────────────────────────────────────────────────

    public static class ParsedTransaction {

        /** Сумма транзакции (всегда > 0). */
        public final double amount;

        /** "INCOME" или "EXPENSE". */
        @NonNull public final String type;

        /**
         * Описание: название магазина (для трат) или "Від: Ім'я" (для пополнений).
         * Используется как title транзакции.
         */
        @NonNull public final String description;

        /**
         * Исходный текст (title + text уведомления).
         * SHA-256 хэш от этого поля хранится в bankMessageHash для защиты от дублей.
         */
        @NonNull public final String rawText;

        ParsedTransaction(
                double amount,
                @NonNull String type,
                @NonNull String description,
                @NonNull String rawText
        ) {
            this.amount      = amount;
            this.type        = type;
            this.description = description;
            this.rawText     = rawText;
        }

        @Override
        public String toString() {
            return "ParsedTransaction{" +
                    "type='" + type + '\'' +
                    ", amount=" + amount +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}