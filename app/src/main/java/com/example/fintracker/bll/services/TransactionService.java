package com.example.fintracker.bll.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.repositories.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ПРИМЕР: Как использовать SessionManager в других сервисах.
 *
 * После того как пользователь залогинился через AuthService,
 * любой другой сервис просто берёт userId из SessionManager.
 *
 * Этот файл — только иллюстрация паттерна, не обязателен к использованию.
 */
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(@NonNull Application application) {
        this.transactionRepository = new TransactionRepository(application);
    }

    /**
     * Создаёт транзакцию для текущего залогиненного пользователя.
     * SessionManager.getInstance().requireUserId() бросит исключение,
     * если пользователь не залогинен — это правильное поведение,
     * т.к. экраны транзакций не должны быть доступны без логина.
     */
    public void createExpense(
            @NonNull String accountId,
            @Nullable String tagId,
            double amount,
            @NonNull String title,
            @Nullable String description
    ) {
        // Берём userId из сессии — не нужно передавать везде параметром
        String currentUserId = SessionManager.getInstance().requireUserId();

        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());

        TransactionEntity transaction = new TransactionEntity();
        transaction.id = UUID.randomUUID().toString();
        transaction.accountId = accountId;
        transaction.userId = currentUserId;       // ← из сессии
        transaction.tagId = tagId;
        transaction.amount = amount;
        transaction.type = "EXPENSE";
        transaction.title = title;
        transaction.description = description;
        transaction.timestamp = now;
        transaction.isSynced = false;
        transaction.isDeleted = false;
        transaction.updatedAt = now;

        transactionRepository.insertTransaction(transaction);
    }

    /**
     * Получает транзакции для конкретного аккаунта.
     * Здесь accountId приходит параметром (пользователь выбирает аккаунт в UI).
     */
    public LiveData<List<TransactionEntity>> getTransactions(@NonNull String accountId) {
        return transactionRepository.getTransactionsByAccountId(accountId);
    }
}