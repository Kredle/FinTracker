package com.example.fintracker.ui.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fintracker.bll.validators.UserValidator;
import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;

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

        // Run test script only in debug builds
        if (isDebugBuild()) {
            runAuthenticationTestScript();
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
}