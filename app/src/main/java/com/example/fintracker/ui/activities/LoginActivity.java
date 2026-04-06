package com.example.fintracker.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.fintracker.R;
import com.example.fintracker.bll.services.AccountInvitationService;
import com.example.fintracker.bll.services.AuthService;
import com.example.fintracker.bll.session.SessionManager;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.fintracker.remote.firebase.FirebaseSyncWorker;

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

    private static final String TAG = "AUTH_TEST";
    
    // UI Components
    private EditText loginEmail;
    private EditText loginPassword;
    private Button loginButton;
    private Button registerButton;

    private static final String TEST_EMAIL    = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LoginActivity onCreate started");
        try {
            setContentView(R.layout.activity_login);
            Log.d(TAG, "LoginActivity setContentView successful");
            
            // Initialize UI components
            initializeViews();
            setupListeners();
            
            Log.d(TAG, "LoginActivity initialization completed - user must login manually");
            // REMOVED: Automatic login check - users MUST login or register manually
        } catch (Exception e) {
            Log.e(TAG, "Critical error in LoginActivity onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Критична помилка при завантаженні: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Initialize UI components from layout
     */
    private void initializeViews() {
        try {
            loginEmail = findViewById(R.id.login_email);
            loginPassword = findViewById(R.id.login_password);
            loginButton = findViewById(R.id.login_button);
            registerButton = findViewById(R.id.register_button);
            
            if (loginEmail == null || loginPassword == null || loginButton == null || registerButton == null) {
                Log.e(TAG, "Деякі UI елементи не знайдені в layout!");
                Toast.makeText(this, "Помилка при завантаженні UI", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Ошибка при инициализации UI компонентів: " + e.getMessage(), e);
        }
    }
    
    /**
     * Setup event listeners for buttons
     */
    private void setupListeners() {
        try {
            if (loginButton != null) {
                loginButton.setOnClickListener(v -> performLogin());
            }
            if (registerButton != null) {
                registerButton.setOnClickListener(v -> navigateToRegister());
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Ошибка при установке слушателей: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform user login
     */
    private void performLogin() {
        try {
            if (loginEmail == null || loginPassword == null) {
                Toast.makeText(this, "UI не ініціалізовано", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Будь ласка, заповніть усі поля", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show loading state
            loginButton.setEnabled(false);
            loginButton.setText("Вхід...");
            
            AuthService authService = new AuthService(getApplication());
            authService.login(email, password, loginResult -> {
                // Reset button state
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Увійти");
                });
                
                if (loginResult.isSuccess()) {
                    Log.d(TAG, "Вхід успішний!");
                    // Sync invitations from cloud
                    AccountInvitationService invitationService = new AccountInvitationService(AppDatabase.getInstance(getApplication()).accountInvitationDao());
                    invitationService.syncInvitationsFromCloud(email, new AccountInvitationService.Callback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            Log.d(TAG, "Invitations synced from cloud");
                        }
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to sync invitations: " + error);
                        }
                    });
                    navigateToHome();
                } else {
                    runOnUiThread(() -> 
                        Toast.makeText(LoginActivity.this, 
                            "Помилка входу: " + loginResult.getErrorMessage(), 
                            Toast.LENGTH_LONG).show()
                    );
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Помилка при вході: " + e.getMessage(), e);
            // Reset button state on error
            runOnUiThread(() -> {
                loginButton.setEnabled(true);
                loginButton.setText("Увійти");
                Toast.makeText(this, "Помилка при вході: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }
    
    /**
     * Navigate to registration screen
     */
    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
    
    /**
     * Navigate to home screen after successful login
     */
    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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

    private void testLogin(){
        Log.d(TAG, "▶ Вхід: " + TEST_EMAIL + " / " + TEST_USERNAME);
        AuthService authService = new AuthService(getApplication());
        authService.login(TEST_EMAIL, TEST_PASSWORD, loginResult -> {

            if (loginResult.isSuccess()) {
                Log.d(TAG, "✅ Вхід успішний!");
                Log.d(TAG, "   Залогінено: " + loginResult.getUser().name);
                Log.d(TAG, "   SessionManager.isLoggedIn() = "
                        + SessionManager.getInstance().isLoggedIn());
                Log.d(TAG, "   SessionManager.getCurrentUserId() = "
                        + SessionManager.getInstance().getCurrentUserId());
            } else {
                Log.e(TAG, "❌ Вхід не вдався: " + loginResult.getErrorMessage());
            }
        });
    }
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
