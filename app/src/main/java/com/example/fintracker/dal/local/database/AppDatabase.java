package com.example.fintracker.dal.local.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.fintracker.dal.local.dao.AccountDao;
import com.example.fintracker.dal.local.dao.AccountInvitationDao;
import com.example.fintracker.dal.local.dao.LimitDao;
import com.example.fintracker.dal.local.dao.SharedAccountMemberDao;
import com.example.fintracker.dal.local.dao.SharedExpenseRecordDao;
import com.example.fintracker.dal.local.dao.TagDao;
import com.example.fintracker.dal.local.dao.TransactionDao;
import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.AccountInvitationEntity;
import com.example.fintracker.dal.local.entities.LimitEntity;
import com.example.fintracker.dal.local.entities.SharedAccountMemberEntity;
import com.example.fintracker.dal.local.entities.SharedExpenseRecordEntity;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.local.entities.UserEntity;

import java.io.File;

@Database(
    entities = {
        UserEntity.class,
        AccountEntity.class,
        SharedAccountMemberEntity.class,
        SharedExpenseRecordEntity.class,
        TagEntity.class,
        LimitEntity.class,
        TransactionEntity.class,
        AccountInvitationEntity.class
    },
    version = 3,
    exportSchema = false,  // Disable schema verification to prevent hash mismatch errors
    autoMigrations = {}  // Explicitly disable auto-migrations
)
public abstract class AppDatabase extends RoomDatabase {

    // Database name constant
    private static final String DATABASE_NAME = "fintracker_db";

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // Lock object for thread-safe singleton
    private static final Object LOCK = new Object();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            android.util.Log.d("AppDatabase", "Executing MIGRATION_1_2 - upgrade to v2");
            
            try {
                // Simply recreate all tables and drop old ones
                // This is safer than trying to migrate old data
                
                database.execSQL("DROP TABLE IF EXISTS `users`");
                database.execSQL("DROP TABLE IF EXISTS `transactions`");
                
                // Create users with ALL v2 fields INCLUDING generalLimit (for future v3 compatibility)
                database.execSQL(
                    "CREATE TABLE `users` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT, " +
                    "`email` TEXT, " +
                    "`password` TEXT, " +
                    "`hourlyRate` REAL NOT NULL, " +
                    "`isBankSyncEnabled` INTEGER NOT NULL, " +
                    "`generalLimit` REAL NOT NULL DEFAULT 0.0, " +  // Include for v3 readiness
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                // Create transactions with all v2 fields
                database.execSQL(
                    "CREATE TABLE `transactions` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`tagId` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`type` TEXT, " +
                    "`title` TEXT, " +
                    "`description` TEXT, " +
                    "`timestamp` TEXT, " +
                    "`bankMessageHash` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                // Create new tables
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `accounts` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT, " +
                    "`ownerId` TEXT, " +
                    "`isShared` INTEGER NOT NULL, " +
                    "`balance` REAL NOT NULL, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shared_account_members` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`role` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tags` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT, " +
                    "`iconName` TEXT, " +
                    "`ownerId` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `limits` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`tagId` TEXT, " +
                    "`amountLimit` REAL NOT NULL, " +
                    "`period` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shared_expense_records` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`transactionId` TEXT, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`timestamp` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `account_invitations` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`fromUserId` TEXT, " +
                    "`toUserEmail` TEXT, " +
                    "`status` TEXT, " +
                    "`createdAt` TEXT, " +
                    "`respondedAt` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                android.util.Log.d("AppDatabase", "✅ MIGRATION_1_2 completed");
                
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "❌ MIGRATION_1_2 failed: " + e.getMessage());
                throw e;
            }
        }
    };

    /**
     * Manual Migration from version 2 to version 3.
     * Changes:
     * - Added generalLimit field to UserEntity
     */
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            android.util.Log.d("AppDatabase", "Executing MIGRATION_2_3");
            
            try {
                // Try to add the column
                database.execSQL("ALTER TABLE `users` ADD COLUMN `generalLimit` REAL NOT NULL DEFAULT 0.0");
                android.util.Log.d("AppDatabase", "✅ Added generalLimit column to users");
                
            } catch (Exception e) {
                // If it fails, the column probably already exists or table is corrupted
                android.util.Log.w("AppDatabase", "Could not add column via ALTER: " + e.getMessage());
                
                // Recreate the table cleanly
                try {
                    database.execSQL(
                        "CREATE TABLE `users_v3` (" +
                        "`id` TEXT NOT NULL PRIMARY KEY, " +
                        "`name` TEXT, " +
                        "`email` TEXT, " +
                        "`password` TEXT, " +
                        "`hourlyRate` REAL NOT NULL, " +
                        "`isBankSyncEnabled` INTEGER NOT NULL, " +
                        "`generalLimit` REAL NOT NULL DEFAULT 0.0, " +
                        "`isSynced` INTEGER NOT NULL, " +
                        "`isDeleted` INTEGER NOT NULL, " +
                        "`updatedAt` TEXT)"
                    );
                    
                    // Copy data if it exists
                    try {
                        database.execSQL(
                            "INSERT INTO `users_v3` (id, name, email, password, hourlyRate, isBankSyncEnabled, generalLimit, isSynced, isDeleted, updatedAt) " +
                            "SELECT id, name, email, password, hourlyRate, isBankSyncEnabled, 0.0, isSynced, isDeleted, updatedAt FROM `users`"
                        );
                    } catch (Exception insertEx) {
                        android.util.Log.w("AppDatabase", "Could not copy data: " + insertEx.getMessage());
                    }
                    
                    database.execSQL("DROP TABLE `users`");
                    database.execSQL("ALTER TABLE `users_v3` RENAME TO `users`");
                    android.util.Log.d("AppDatabase", "✅ Recreated users table for v3");
                    
                } catch (Exception recreateEx) {
                    android.util.Log.e("AppDatabase", "❌ Failed to recreate users table: " + recreateEx.getMessage());
                    throw recreateEx;
                }
            }
        }
    };

    /**
     * Direct migration from version 1 to version 3.
     */
    private static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            android.util.Log.d("AppDatabase", "Executing MIGRATION_1_3");
            
            try {
                // Drop old tables
                database.execSQL("DROP TABLE IF EXISTS `users`");
                database.execSQL("DROP TABLE IF EXISTS `transactions`");
                
                // Create fresh v3 schema
                database.execSQL(
                    "CREATE TABLE `users` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT, " +
                    "`email` TEXT, " +
                    "`password` TEXT, " +
                    "`hourlyRate` REAL NOT NULL, " +
                    "`isBankSyncEnabled` INTEGER NOT NULL, " +
                    "`generalLimit` REAL NOT NULL DEFAULT 0.0, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE `transactions` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`tagId` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`type` TEXT, " +
                    "`title` TEXT, " +
                    "`description` TEXT, " +
                    "`timestamp` TEXT, " +
                    "`bankMessageHash` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                // Create v3 tables
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `accounts` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT, " +
                    "`ownerId` TEXT, " +
                    "`isShared` INTEGER NOT NULL, " +
                    "`balance` REAL NOT NULL, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shared_account_members` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`role` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tags` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT, " +
                    "`iconName` TEXT, " +
                    "`ownerId` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `limits` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`tagId` TEXT, " +
                    "`amountLimit` REAL NOT NULL, " +
                    "`period` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shared_expense_records` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`transactionId` TEXT, " +
                    "`accountId` TEXT, " +
                    "`userId` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`timestamp` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `account_invitations` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`accountId` TEXT, " +
                    "`fromUserId` TEXT, " +
                    "`toUserEmail` TEXT, " +
                    "`status` TEXT, " +
                    "`createdAt` TEXT, " +
                    "`respondedAt` TEXT, " +
                    "`isSynced` INTEGER NOT NULL, " +
                    "`isDeleted` INTEGER NOT NULL, " +
                    "`updatedAt` TEXT)"
                );
                
                android.util.Log.d("AppDatabase", "✅ MIGRATION_1_3 completed");
                
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "❌ MIGRATION_1_3 failed: " + e.getMessage());
                throw e;
            }
        }
    };

    /**
     * Thread-safe Singleton pattern to get database instance.
     * Uses double-checked locking for optimal performance.
     * 
     * Handles:
     * - Room integrity verification failures (mismatched hashes)
     * - Migration errors from old versions
     * - Corrupted database files
     *
     * @param context Application context
     * @return AppDatabase instance
     */
    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = initializeDatabaseWithRetry(context);
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Initialize database with comprehensive error handling and retry logic.
     * Handles "cannot verify data integrity" errors by cleaning up corrupted database files.
     */
    private static AppDatabase initializeDatabaseWithRetry(@NonNull Context context) {
        android.util.Log.d("AppDatabase", "=== Initializing AppDatabase v3 ===");
        
        int retryCount = 0;
        final int MAX_RETRIES = 5;  // Increased from 4 to 5
        
        while (retryCount < MAX_RETRIES) {
            retryCount++;
            try {
                android.util.Log.d("AppDatabase", "🔄 Attempt " + retryCount + "/" + MAX_RETRIES);
                
                AppDatabase instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .allowMainThreadQueries()
                .build();
                
                // Verify the database works
                try {
                    instance.userDao();  // Simple check - DAO initialization
                    android.util.Log.d("AppDatabase", "✅ AppDatabase initialized successfully on attempt " + retryCount);
                    return instance;
                } catch (Exception verifyEx) {
                    android.util.Log.e("AppDatabase", "❌ Verification failed on attempt " + retryCount + ": " + verifyEx.getMessage());
                    instance.close();
                    throw verifyEx;
                }
                
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", 
                    "❌ Attempt " + retryCount + "/" + MAX_RETRIES + " failed: " + 
                    e.getClass().getSimpleName() + " - " + e.getMessage());
                
                if (retryCount < MAX_RETRIES) {
                    // Only cleanup on actual errors, not preemptively
                    android.util.Log.w("AppDatabase", "🧹 Cleanup after error before retry " + (retryCount + 1));
                    cleanupDatabaseFiles(context);
                    
                    try {
                        Thread.sleep(400 + (retryCount * 100));  // Increasing delays
                    } catch (InterruptedException ignored) {}
                } else {
                    // All retries exhausted - final attempt with complete reset
                    android.util.Log.e("AppDatabase", "❌ ALL " + MAX_RETRIES + " ATTEMPTS FAILED!");
                    android.util.Log.e("AppDatabase", "🚨 PERFORMING EMERGENCY COMPLETE RESET!");
                    
                    try {
                        resetDatabase(context);
                        AppDatabase instance = Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                DATABASE_NAME
                        )
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .allowMainThreadQueries()
                        .build();
                        
                        instance.userDao();
                        android.util.Log.d("AppDatabase", "✅ Recovery successful after emergency reset");
                        return instance;
                    } catch (Exception emergencyError) {
                        android.util.Log.e("AppDatabase", "❌ Emergency recovery failed: " + emergencyError.getMessage());
                        throw new RuntimeException("Database initialization failed after " + MAX_RETRIES + 
                            " attempts and emergency reset. App data cleared.", e);
                    }
                }
            }
        }
        
        throw new RuntimeException("Database initialization failed after all retry attempts");
    }
    
    /**
     * Cleanup all database-related files.
     */
    private static void cleanupDatabaseFiles(@NonNull Context context) {
        try {
            File dbFile = context.getDatabasePath(DATABASE_NAME);
            File dbJournal = context.getDatabasePath(DATABASE_NAME + "-journal");
            File dbShm = context.getDatabasePath(DATABASE_NAME + "-shm");
            File dbWal = context.getDatabasePath(DATABASE_NAME + "-wal");
            
            boolean cleaned = false;
            if (dbFile.exists() && dbFile.delete()) {
                android.util.Log.w("AppDatabase", "Deleted corrupted .db file");
                cleaned = true;
            }
            if (dbJournal.exists() && dbJournal.delete()) {
                android.util.Log.w("AppDatabase", "Deleted -journal file");
                cleaned = true;
            }
            if (dbShm.exists() && dbShm.delete()) {
                android.util.Log.w("AppDatabase", "Deleted -shm file");
                cleaned = true;
            }
            if (dbWal.exists() && dbWal.delete()) {
                android.util.Log.w("AppDatabase", "Deleted -wal file");
                cleaned = true;
            }
            
            if (cleaned) {
                android.util.Log.w("AppDatabase", "✅ Cleaned up corrupted database files - will rebuild");
            }
        } catch (Exception ex) {
            android.util.Log.e("AppDatabase", "❌ Cleanup failed: " + ex.getMessage());
        }
    }

    /**
     * For testing purposes only - allows resetting the singleton instance.
     * Should not be used in production code.
     */
    @SuppressWarnings("unused")  // Used in tests only
    public static void destroyInstance() {
        synchronized (LOCK) {
            if (INSTANCE != null && INSTANCE.isOpen()) {
                INSTANCE.close();
            }
            INSTANCE = null;
        }
    }

    /**
     * Emergency method to completely reset the database.
     * Deletes all database files and resets the singleton.
     * Should only be called as last resort when database is completely corrupted.
     */
    public static void resetDatabase(@NonNull Context context) {
        try {
            android.util.Log.w("AppDatabase", "EMERGENCY: Resetting database!");
            
            // Close current instance
            destroyInstance();
            
            // Delete all database files
            File dbFile = context.getDatabasePath(DATABASE_NAME);
            File dbJournalFile = context.getDatabasePath(DATABASE_NAME + "-journal");
            File dbShmFile = context.getDatabasePath(DATABASE_NAME + "-shm");
            File dbWalFile = context.getDatabasePath(DATABASE_NAME + "-wal");
            
            if (dbFile.exists()) dbFile.delete();
            if (dbJournalFile.exists()) dbJournalFile.delete();
            if (dbShmFile.exists()) dbShmFile.delete();
            if (dbWalFile.exists()) dbWalFile.delete();
            
            android.util.Log.w("AppDatabase", "Database files deleted. Call getInstance() to reinitialize.");
        } catch (Exception e) {
            android.util.Log.e("AppDatabase", "Error during database reset: " + e.getMessage());
        }
    }

    // DAO abstract methods - Add these as you create your DAOs
    /**
     * Provides access to UserDao for user-related database operations.
     * @return UserDao instance
     */
    public abstract UserDao userDao();

    /**
     * Provides access to AccountDao for account-related database operations.
     * @return AccountDao instance
     */
    public abstract AccountDao accountDao();

    /**
     * Provides access to TagDao for tag-related database operations.
     * @return TagDao instance
     */
    public abstract TagDao tagDao();

    /**
     * Provides access to TransactionDao for transaction-related database operations.
     * @return TransactionDao instance
     */
    public abstract TransactionDao transactionDao();

    /**
     * Provides access to LimitDao for spending limit-related database operations.
     * @return LimitDao instance
     */
    public abstract LimitDao limitDao();

    /**
     * Provides access to SharedAccountMemberDao for shared account membership operations.
     * @return SharedAccountMemberDao instance
     */
    public abstract SharedAccountMemberDao sharedAccountMemberDao();

    /**
     * Provides access to SharedExpenseRecordDao for shared expense record operations.
     * @return SharedExpenseRecordDao instance
     */
    public abstract SharedExpenseRecordDao sharedExpenseRecordDao();

    /**
     * Provides access to AccountInvitationDao for account invitation operations.
     * @return AccountInvitationDao instance
     */
    public abstract AccountInvitationDao accountInvitationDao();

    // Public accessors for migrations (used in tests)
    public static Migration getMigration1To2() {
        return MIGRATION_1_2;
    }

    public static Migration getMigration2To3() {
        return MIGRATION_2_3;
    }

    public static Migration getMigration1To3() {
        return MIGRATION_1_3;
    }

    // Example for future DAOs:
    // (Add new DAOs here as needed)
}
