package com.example.fintracker.ui.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fintracker.bll.validators.UserValidator;
import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;

import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * LoginActivity
 *
 * This activity demonstrates the full authentication flow:
 * 1. User registration with validation
 * 2. Duplicate user prevention
 * 3. User login with email or username
 *
 * The test script runs on a background thread to avoid blocking the main thread
 * with database operations (Room requires this best practice).
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "AUTH_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_login); // Uncomment when you create the layout

        // Run authentication test script on background thread
        runAuthenticationTestScript();
    }

    /**
     * Runs a comprehensive authentication test script.
     * Demonstrates: validation failure, successful registration, and successful login.
     * All operations run on a background thread to follow Room Database best practices.
     */
    private void runAuthenticationTestScript() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(LoginActivity.this);
            UserDao userDao = db.userDao();

            Log.d(TAG, "=== STARTING AUTHENTICATION TEST SCRIPT ===");

            // ====== STEP 1: FAILING VALIDATION TEST ======
            Log.d(TAG, "\n[TEST 1] Attempting registration with INVALID EMAIL...");
            try {
                String invalidEmail = "bad-email";
                String username = "testuser123";
                String password = "password123";

                // Attempt to validate invalid email
                UserValidator.isValidEmail(invalidEmail);

                // Should not reach here
                Log.d(TAG, "FAILED: Invalid email was accepted (validation didn't throw exception)");
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "PASSED: Validation correctly rejected invalid email");
                Log.d(TAG, "Error Message: " + e.getMessage());
            }

            // ====== STEP 2: SUCCESSFUL REGISTRATION ======
            Log.d(TAG, "\n[TEST 2] Attempting registration with VALID data...");
            try {
                String email = "john.doe@example.com";
                String username = "johndoe";
                String password = "password123";

                // Validate all fields
                UserValidator.validateRegistration(email, username, password);
                Log.d(TAG, "Validation PASSED: All fields are valid");

                // Check if user already exists
                boolean userExists = userDao.checkIfUserExists(email, username);
                if (userExists) {
                    Log.d(TAG, "User already exists in database, skipping insertion");
                } else {
                    Log.d(TAG, "User doesn't exist yet, proceeding with registration");

                    // Create new user entity
                    UserEntity newUser = new UserEntity();
                    newUser.id = UUID.randomUUID().toString();
                    newUser.email = email;
                    newUser.name = username;
                    newUser.password = password;
                    newUser.hourlyRate = 50.0;
                    newUser.isBankSyncEnabled = true;
                    newUser.isSynced = false;
                    newUser.isDeleted = false;
                    newUser.updatedAt = "2026-03-05T21:46:23Z";

                    // Insert user into database
                    userDao.insertUser(newUser);
                    Log.d(TAG, "   PASSED: User successfully registered!");
                    Log.d(TAG, "   User ID: " + newUser.id);
                    Log.d(TAG, "   Email: " + newUser.email);
                    Log.d(TAG, "   Username: " + newUser.name);
                }

            } catch (IllegalArgumentException e) {
                Log.d(TAG, "   FAILED: Registration validation failed");
                Log.d(TAG, "   Error: " + e.getMessage());
            } catch (Exception e) {
                Log.d(TAG, "   FAILED: Unexpected error during registration");
                Log.d(TAG, "   Error: " + e.getMessage());
            }

            // ====== STEP 3: SUCCESSFUL LOGIN WITH EMAIL ======
            Log.d(TAG, "\n[TEST 3] Attempting LOGIN using EMAIL and password...");
            try {
                String email = "john.doe@example.com";
                String password = "password123";

                UserEntity loginUser = userDao.getUserByEmailOrName(email, password);

                if (loginUser != null) {
                    Log.d(TAG, "   PASSED: Login successful with EMAIL!");
                    Log.d(TAG, "   User ID: " + loginUser.id);
                    Log.d(TAG, "   Email: " + loginUser.email);
                    Log.d(TAG, "   Username: " + loginUser.name);
                    Log.d(TAG, "   Hourly Rate: $" + loginUser.hourlyRate);
                } else {
                    Log.d(TAG, "   FAILED: Login unsuccessful - user not found with email/password");
                }

            } catch (Exception e) {
                Log.d(TAG, "   FAILED: Error during email login");
                Log.d(TAG, "   Error: " + e.getMessage());
            }

            // ====== STEP 4: SUCCESSFUL LOGIN WITH USERNAME ======
            Log.d(TAG, "\n[TEST 4] Attempting LOGIN using USERNAME and password...");
            try {
                String username = "johndoe";
                String password = "password123";

                UserEntity loginUser = userDao.getUserByEmailOrName(username, password);

                if (loginUser != null) {
                    Log.d(TAG, "   PASSED: Login successful with USERNAME!");
                    Log.d(TAG, "   User ID: " + loginUser.id);
                    Log.d(TAG, "   Email: " + loginUser.email);
                    Log.d(TAG, "   Username: " + loginUser.name);
                    Log.d(TAG, "   Hourly Rate: $" + loginUser.hourlyRate);
                } else {
                    Log.d(TAG, "   FAILED: Login unsuccessful - user not found with username/password");
                }

            } catch (Exception e) {
                Log.d(TAG, "   FAILED: Error during username login");
                Log.d(TAG, "   Error: " + e.getMessage());
            }

            Log.d(TAG, "\n=== AUTHENTICATION TEST SCRIPT COMPLETED ===\n");
        });
    }
}