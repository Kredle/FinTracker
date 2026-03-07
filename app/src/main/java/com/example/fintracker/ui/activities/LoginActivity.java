package com.example.fintracker.ui.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.repositories.AccountRepository;
import com.example.fintracker.dal.repositories.DataCallback;
import com.example.fintracker.dal.repositories.TagRepository;
import com.example.fintracker.dal.repositories.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * LoginActivity
 * <p>
 * Main login screen for the application.
 * Handles user authentication and registration.
 * <p>
 * DEMONSTRATES: Automatic Firebase sync - just use repositories normally!
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Uncomment and set actual layout when created
        // setContentView(R.layout.activity_login);

        // TODO: Initialize UI components and set up login/registration logic

        // TEMPORARY: Create test data using REPOSITORIES (production approach)
        createTestDataUsingRepositories();
    }

    /**
     * Creates test data using Repositories (production approach).
     * <p>
     * HOW AUTOMATIC SYNC WORKS:
     * 1. You save data via Repository (with isSynced = false)
     * 2. Background WorkManager automatically syncs to Firebase
     * 3. You don't call any sync method manually!
     * <p>
     * REMOVE THIS METHOD after verifying Firebase sync works!
     */
    private void createTestDataUsingRepositories() {
        // Initialize repositories
        AccountRepository accountRepo = new AccountRepository(getApplication());
        TagRepository tagRepo = new TagRepository(getApplication());
        TransactionRepository transactionRepo = new TransactionRepository(getApplication());

        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
        String testOwnerId = "test-user-" + System.currentTimeMillis();

        // Create test account
        AccountEntity testAccount = new AccountEntity();
        testAccount.id = UUID.randomUUID().toString();
        testAccount.name = "Test Savings Account";
        testAccount.ownerId = testOwnerId;
        testAccount.isShared = false;
        testAccount.balance = 1500.75;
        testAccount.isSynced = false; // ← Will be synced automatically by WorkManager!
        testAccount.isDeleted = false;
        testAccount.updatedAt = timestamp;

        // Create test tag
        TagEntity testTag = new TagEntity();
        testTag.id = UUID.randomUUID().toString();
        testTag.name = "Test Groceries";
        testTag.iconName = "shopping_cart";
        testTag.ownerId = testOwnerId;
        testTag.isSynced = false; // ← Will be synced automatically by WorkManager!
        testTag.isDeleted = false;
        testTag.updatedAt = timestamp;

        // Create test transaction
        TransactionEntity testTransaction = new TransactionEntity();
        testTransaction.id = UUID.randomUUID().toString();
        testTransaction.accountId = testAccount.id;
        testTransaction.userId = testOwnerId;
        testTransaction.tagId = testTag.id;
        testTransaction.amount = 45.99;
        testTransaction.type = "EXPENSE";
        testTransaction.title = "Test Grocery Shopping";
        testTransaction.description = "Weekly groceries from supermarket";
        testTransaction.timestamp = timestamp;
        testTransaction.bankMessageHash = null;
        testTransaction.isSynced = false; // ← Will be synced automatically by WorkManager!
        testTransaction.isDeleted = false;
        testTransaction.updatedAt = timestamp;

        // Save via repositories (just like your friends would do in production!)
        accountRepo.insertAccount(testAccount, new DataCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void data) {
                Log.d(TAG, "Account saved to Room (ID: " + testAccount.id + ")");
                Log.d(TAG, "WorkManager will sync to Firebase automatically...");
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                Log.e(TAG, "Failed to save account", throwable);
            }
        });

        tagRepo.insertTag(testTag, new DataCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void data) {
                Log.d(TAG, "Tag saved to Room (ID: " + testTag.id + ")");
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                Log.e(TAG, "Failed to save tag", throwable);
            }
        });

        transactionRepo.insertTransaction(testTransaction, new DataCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void data) {
                Log.d(TAG, "Transaction saved to Room (ID: " + testTransaction.id + ")");
                Log.d(TAG, "");
                Log.d(TAG, "NO MANUAL SYNC NEEDED!");
                Log.d(TAG, "WorkManager will automatically sync when WiFi is available");
                Log.d(TAG, "Check Logcat for 'FIREBASE_SYNC' to see automatic sync");
                Log.d(TAG, "Check Firebase Console to verify data uploaded");
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                Log.e(TAG, "Failed to save transaction", throwable);
            }
        });

        // Notice: We didn't call any sync method!
        // The sync happens automatically in the background via WorkManager
    }
}

