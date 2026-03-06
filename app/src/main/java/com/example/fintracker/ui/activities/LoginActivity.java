package com.example.fintracker.ui.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fintracker.bll.validators.AccountValidator;
import com.example.fintracker.bll.validators.TagValidator;
import com.example.fintracker.bll.validators.UserValidator;
import com.example.fintracker.dal.local.dao.AccountDao;
import com.example.fintracker.dal.local.dao.TagDao;
import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.local.entities.UserEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LoginActivity
 *
 * Main login screen for the application.
 *
 * Debug builds: Runs an automated authentication test script to verify core functionality.
 * Release builds: Shows normal login UI without test execution.
 *
 * The test script (if enabled) runs on a background thread to avoid blocking the main thread
 * with database operations (Room requires this best practice).
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "AUTH_TEST";

    // Executor service for managing background database operations
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Uncomment and set actual layout when created
        // setContentView(R.layout.activity_login);

        // Initialize executor service for database operations
        executorService = Executors.newSingleThreadExecutor();

        // Run test scripts only in debug builds
        if (isDebugBuild()) {
            runAuthenticationTestScript();
            runAccountAndTagTestScript();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Properly shut down the executor to prevent resource leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    /**
     * Checks if the application is running in debug mode.
     * Uses ApplicationInfo flags to determine debuggability without requiring BuildConfig.
     *
     * @return true if app is debuggable, false otherwise
     */
    private boolean isDebugBuild() {
        return (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /**
     * Runs a comprehensive authentication test script (DEBUG builds only).
     *
     * Tests:
     * 1. Email validation - rejects invalid email format
     * 2. User registration - creates user with duplicate prevention
     * 3. Login with email - flexible login using email
     * 4. Login with username - flexible login using username
     *
     * This method executes on a background thread to follow Room Database best practices
     * and avoid ANR (Application Not Responding) errors.
     *
     * NOTE: This code only runs in debug builds (BuildConfig.DEBUG).
     * In production/release builds, this is never executed.
     */
    private void runAuthenticationTestScript() {
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(LoginActivity.this);
            UserDao userDao = db.userDao();

            Log.d(TAG, "=== STARTING AUTHENTICATION TEST SCRIPT ===");

            // ====== TEST 1: EMAIL VALIDATION ======
            Log.d(TAG, "\n[TEST 1] Attempting registration with INVALID EMAIL...");
            try {
                // Attempt to validate invalid email
                UserValidator.isValidEmail("bad-email");

                // Should not reach here
                Log.d(TAG, "FAILED: Invalid email was accepted (validation didn't throw exception)");
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "PASSED: Validation correctly rejected invalid email");
                Log.d(TAG, "   Error: " + e.getMessage());
            }

            // ====== TEST 2: USER REGISTRATION ======
            Log.d(TAG, "\n[TEST 2] Attempting registration with VALID data...");
            try {
                // Validate all fields (no PII in logs)
                UserValidator.validateRegistration("john.doe@example.com", "johndoe", "password123");
                Log.d(TAG, "   Validation PASSED: All fields are valid");

                // Check if user already exists
                boolean userExists = userDao.checkIfUserExists("john.doe@example.com", "johndoe");
                if (userExists) {
                    Log.d(TAG, "   User already exists in database, skipping insertion");
                } else {
                    Log.d(TAG, "   User doesn't exist yet, proceeding with registration");

                    // Create new user entity
                    UserEntity newUser = new UserEntity();
                    newUser.id = UUID.randomUUID().toString();
                    newUser.email = "john.doe@example.com";
                    newUser.name = "johndoe";
                    newUser.password = "password123";
                    newUser.hourlyRate = 50.0;
                    newUser.isBankSyncEnabled = true;
                    newUser.isSynced = false;
                    newUser.isDeleted = false;
                    newUser.updatedAt = "2026-03-05T21:46:23Z";

                    // Insert user into database
                    userDao.insertUser(newUser);

                    // Log success without PII details
                    Log.d(TAG, "   PASSED: User successfully registered!");
                    Log.d(TAG, "   User ID: " + newUser.id);
                }

            } catch (IllegalArgumentException e) {
                Log.d(TAG, "   FAILED: Registration validation failed");
                Log.d(TAG, "   Error: " + e.getMessage());
            } catch (Exception e) {
                Log.d(TAG, "   FAILED: Unexpected error during registration");
                Log.d(TAG, "   Error: " + e.getMessage());
            }

            // ====== TEST 3: LOGIN WITH EMAIL ======
            Log.d(TAG, "\n[TEST 3] Attempting LOGIN using EMAIL and password...");
            try {
                UserEntity loginUser = userDao.getUserByEmailOrName("john.doe@example.com", "password123");

                if (loginUser != null) {
                    // Log success without logging actual email/password
                    Log.d(TAG, "   PASSED: Login successful with email!");
                    Log.d(TAG, "   User ID: " + loginUser.id);
                } else {
                    Log.d(TAG, "   FAILED: Login unsuccessful - user not found");
                }

            } catch (Exception e) {
                Log.d(TAG, "   FAILED: Error during email login");
                Log.d(TAG, "   Error: " + e.getMessage());
            }

            // ====== TEST 4: LOGIN WITH USERNAME ======
            Log.d(TAG, "\n[TEST 4] Attempting LOGIN using USERNAME and password...");
            try {
                UserEntity loginUser = userDao.getUserByEmailOrName("johndoe", "password123");

                if (loginUser != null) {
                    // Log success without logging actual username/password
                    Log.d(TAG, "   PASSED: Login successful with username!");
                    Log.d(TAG, "   User ID: " + loginUser.id);
                } else {
                    Log.d(TAG, "   FAILED: Login unsuccessful - user not found");
                }

            } catch (Exception e) {
                Log.d(TAG, "   FAILED: Error during username login");
                Log.d(TAG, "   Error: " + e.getMessage());
            }

            Log.d(TAG, "\n=== AUTHENTICATION TEST SCRIPT COMPLETED ===\n");
        });
    }

    /**
     * Runs a comprehensive Account and Tag Management test script (DEBUG builds only).
     *
     * Tests:
     * 1. Account balance validation - rejects negative balance
     * 2. Account creation - creates valid account linked to user
     * 3. Tag creation - creates valid tag linked to user
     * 4. Data retrieval - fetches and displays created accounts and tags
     *
     * This method executes on a background thread to follow Room Database best practices
     * and avoid ANR (Application Not Responding) errors.
     *
     * NOTE: This code only runs in debug builds.
     * In production/release builds, this is never executed.
     */
    private void runAccountAndTagTestScript() {
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(LoginActivity.this);
            UserDao userDao = db.userDao();
            AccountDao accountDao = db.accountDao();
            TagDao tagDao = db.tagDao();

            Log.d("Account_And_Tags_TEST", "\n=== STARTING ACCOUNT AND TAG MANAGEMENT TEST SCRIPT ===");

            // Retrieve or create test user
            UserEntity testUser = userDao.getUserByEmail("john.doe@example.com");
            String userId;

            if (testUser != null) {
                userId = testUser.id;
                Log.d("Account_And_Tags_TEST", "\n[SETUP] Using existing test user");
                Log.d("Account_And_Tags_TEST", "   User ID: " + userId);
            } else {
                // Create a test user if not exists
                Log.d("Account_And_Tags_TEST", "\n[SETUP] Creating test user for Account/Tag tests");
                testUser = new UserEntity();
                testUser.id = UUID.randomUUID().toString();
                testUser.email = "john.doe@example.com";
                testUser.name = "johndoe";
                testUser.password = "password123";
                testUser.hourlyRate = 50.0;
                testUser.isBankSyncEnabled = true;
                testUser.isSynced = false;
                testUser.isDeleted = false;
                testUser.updatedAt = "2026-03-05T21:46:23Z";
                userDao.insertUser(testUser);
                userId = testUser.id;
                Log.d("Account_And_Tags_TEST", "   User created with ID: " + userId);
            }

            // ====== TEST 1: ACCOUNT BALANCE VALIDATION ======
            Log.d("Account_And_Tags_TEST", "\n[TEST 1] Attempting to create account with NEGATIVE balance...");
            try {
                // Attempt to validate negative balance
                AccountValidator.isValidBalance(-100.0);

                // Should not reach here
                Log.d("Account_And_Tags_TEST", "FAILED: Negative balance was accepted (validation didn't throw exception)");
            } catch (IllegalArgumentException e) {
                Log.d("Account_And_Tags_TEST", "PASSED: Validation correctly rejected negative balance");
                Log.d("Account_And_Tags_TEST", "   Error: " + e.getMessage());
            }

            // ====== TEST 2: ACCOUNT CREATION ======
            Log.d("Account_And_Tags_TEST", "\n[TEST 2] Creating account with VALID data...");
            try {
                // Validate account data
                AccountValidator.validateAccountCreation("Credit Card", 5000.0);
                Log.d("Account_And_Tags_TEST", "   Validation PASSED: All account fields are valid");

                // Create account entity
                AccountEntity newAccount = new AccountEntity();
                newAccount.id = UUID.randomUUID().toString();
                newAccount.name = "Credit Card";
                newAccount.ownerId = userId;
                newAccount.isShared = false;
                newAccount.balance = 5000.0;
                newAccount.isSynced = false;
                newAccount.isDeleted = false;
                newAccount.updatedAt = "2026-03-05T21:46:23Z";

                // Insert account
                accountDao.insertAccount(newAccount);

                Log.d("Account_And_Tags_TEST", "   PASSED: Account successfully created!");
                Log.d("Account_And_Tags_TEST", "   Account ID: " + newAccount.id);
                Log.d("Account_And_Tags_TEST", "   Account Name: Credit Card");
                Log.d("Account_And_Tags_TEST", "   Initial Balance: $5000.0");

                // ====== TEST 3: TAG CREATION ======
                Log.d("Account_And_Tags_TEST", "\n[TEST 3] Creating tag with VALID data...");
                try {
                    // Validate tag data
                    TagValidator.validateTagCreation("Groceries");
                    Log.d("Account_And_Tags_TEST", "   Validation PASSED: Tag name is valid");

                    // Create tag entity
                    TagEntity newTag = new TagEntity();
                    newTag.id = UUID.randomUUID().toString();
                    newTag.name = "Groceries";
                    newTag.iconName = "ic_groceries";
                    newTag.ownerId = userId;
                    newTag.isSynced = false;
                    newTag.isDeleted = false;
                    newTag.updatedAt = "2026-03-05T21:46:23Z";

                    // Insert tag
                    tagDao.insertTag(newTag);

                    Log.d("Account_And_Tags_TEST", "   PASSED: Tag successfully created!");
                    Log.d("Account_And_Tags_TEST", "   Tag ID: " + newTag.id);
                    Log.d("Account_And_Tags_TEST", "   Tag Name: Groceries");

                    // ====== TEST 4: DATA RETRIEVAL ======
                    Log.d("Account_And_Tags_TEST", "\n[TEST 4] Retrieving all accounts and tags for user...");
                    try {
                        // Fetch all accounts for the user
                        List<AccountEntity> userAccounts = accountDao.getAccountsByUserId(userId);
                        Log.d("Account_And_Tags_TEST", "   PASSED: Successfully retrieved accounts");
                        Log.d("Account_And_Tags_TEST", "   Total Accounts: " + userAccounts.size());
                        for (int i = 0; i < userAccounts.size(); i++) {
                            AccountEntity account = userAccounts.get(i);
                            Log.d("Account_And_Tags_TEST", "     [" + (i + 1) + "] " + account.name + " - Balance: $" + account.balance);
                        }

                        // Fetch all tags for the user
                        List<TagEntity> userTags = tagDao.getTagsByUserId(userId);
                        Log.d("Account_And_Tags_TEST", "   PASSED: Successfully retrieved user tags");
                        Log.d("Account_And_Tags_TEST", "   Total User Tags: " + userTags.size());
                        for (int i = 0; i < userTags.size(); i++) {
                            TagEntity tag = userTags.get(i);
                            Log.d("Account_And_Tags_TEST", "     [" + (i + 1) + "] " + tag.name);
                        }

                        // Fetch default tags
                        List<TagEntity> defaultTags = tagDao.getDefaultTags();
                        Log.d("Account_And_Tags_TEST", "   PASSED: Successfully retrieved default tags");
                        Log.d("Account_And_Tags_TEST", "   Total Default Tags: " + defaultTags.size());

                    } catch (Exception e) {
                        Log.d("Account_And_Tags_TEST", "   FAILED: Error during data retrieval");
                        Log.d("Account_And_Tags_TEST", "   Error: " + e.getMessage());
                        e.printStackTrace();
                    }

                } catch (IllegalArgumentException e) {
                    Log.d("Account_And_Tags_TEST", "   FAILED: Tag validation failed");
                    Log.d("Account_And_Tags_TEST", "   Error: " + e.getMessage());
                } catch (Exception e) {
                    Log.d("Account_And_Tags_TEST", "   FAILED: Unexpected error during tag creation");
                    Log.d("Account_And_Tags_TEST", "   Error: " + e.getMessage());
                    e.printStackTrace();
                }

            } catch (IllegalArgumentException e) {
                Log.d("Account_And_Tags_TEST", "   FAILED: Account validation failed");
                Log.d("Account_And_Tags_TEST", "   Error: " + e.getMessage());
            } catch (Exception e) {
                Log.d("Account_And_Tags_TEST", "   FAILED: Unexpected error during account creation");
                Log.d("Account_And_Tags_TEST", "   Error: " + e.getMessage());
                e.printStackTrace();
            }

            Log.d("Account_And_Tags_TEST", "\n=== ACCOUNT AND TAG MANAGEMENT TEST SCRIPT COMPLETED ===\n");
        });
    }
}