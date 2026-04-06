package com.example.fintracker.bll.services.bank;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonoBankNotificationParser {

    public static final String MONOBANK_PACKAGE = "com.monobank.mobile";

    public static class ParsedTransaction {
        public final String type;
        public final double amount;
        public final String description;
        public final String rawText;

        public ParsedTransaction(
                @NonNull String type,
                double amount,
                @NonNull String description,
                @NonNull String rawText
        ) {
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.rawText = rawText;
        }

        @Override
        public String toString() {
            return type + " " + amount + " | " + description;
        }
    }

    @Nullable
    public static ParsedTransaction parse(@Nullable String title, @NonNull String text) {
        if (title == null) title = "";
        
        String fullText = title + " | " + text;

        Pattern amountPattern = Pattern.compile("[+-]?\\s*([\\d,]+(?:[.,]\\d{1,2})?)\\s*грн");
        Matcher amountMatcher = amountPattern.matcher(fullText);

        if (!amountMatcher.find()) {
            return null;
        }

        String amountStr = amountMatcher.group(1).replace(",", ".");
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount < 0) amount = -amount;
        } catch (NumberFormatException e) {
            return null;
        }

        String type = "EXPENSE";
        
        if (fullText.toLowerCase().contains("+") || 
            fullText.toLowerCase().contains("поповнення") ||
            fullText.toLowerCase().contains("переклад") ||
            fullText.toLowerCase().contains("сплачено") ||
            fullText.toLowerCase().contains("зарахунок")) {
            type = "INCOME";
        }

        String description = extractDescription(fullText);
        if (description.isEmpty()) {
            description = type.equals("INCOME") ? "Поповнення MonoBank" : "Покупка MonoBank";
        }

        return new ParsedTransaction(type, amount, description, fullText);
    }

    @NonNull
    private static String extractDescription(@NonNull String text) {
        String[] parts = text.split("\\|");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            
            if (part.contains("грн") || 
                part.toLowerCase().contains("баланс") ||
                part.isEmpty()) {
                continue;
            }
            
            if (!part.isEmpty()) {
                return part.length() > 100 ? part.substring(0, 97) + "..." : part;
            }
        }
        
        return "";
    }
}

