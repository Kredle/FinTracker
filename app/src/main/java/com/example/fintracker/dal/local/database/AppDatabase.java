package com.example.fintracker.dal.local.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.LimitEntity;
import com.example.fintracker.dal.local.entities.SharedAccountMemberEntity;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.local.entities.UserEntity;

@Database(
    entities = {
        UserEntity.class,
        AccountEntity.class,
        SharedAccountMemberEntity.class,
        TagEntity.class,
        LimitEntity.class,
        TransactionEntity.class
    },
    version = 2,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    // Database name constant
    private static final String DATABASE_NAME = "fintracker_db";

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // Lock object for thread-safe singleton
    private static final Object LOCK = new Object();

    /**
     * Manual Migration from version 1 to version 2.
     * Changes:
     * - Updated UserEntity: changed 'username' to 'name', changed 'updatedAt' from long to String
     * - Updated TransactionEntity: changed from int id to String UUID, added many new fields
     * - Added new tables: accounts, shared_account_members, tags, limits
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Step 1: Create new tables

            // Create accounts table
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `accounts` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT, " +
                "`ownerId` TEXT, " +
                "`isShared` INTEGER NOT NULL, " +
                "`balance` REAL NOT NULL, " +
                "`isSynced` INTEGER NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`updatedAt` TEXT, " +
                "PRIMARY KEY(`id`))"
            );

            // Create shared_account_members table
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `shared_account_members` (" +
                "`id` TEXT NOT NULL, " +
                "`accountId` TEXT, " +
                "`userId` TEXT, " +
                "`role` TEXT, " +
                "`isSynced` INTEGER NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`updatedAt` TEXT, " +
                "PRIMARY KEY(`id`))"
            );

            // Create tags table
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `tags` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT, " +
                "`iconName` TEXT, " +
                "`ownerId` TEXT, " +
                "`isSynced` INTEGER NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`updatedAt` TEXT, " +
                "PRIMARY KEY(`id`))"
            );

            // Create limits table
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `limits` (" +
                "`id` TEXT NOT NULL, " +
                "`accountId` TEXT, " +
                "`userId` TEXT, " +
                "`tagId` TEXT, " +
                "`amountLimit` REAL NOT NULL, " +
                "`period` TEXT, " +
                "`isSynced` INTEGER NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`updatedAt` TEXT, " +
                "PRIMARY KEY(`id`))"
            );

            // Step 2: Migrate users table
            // Create new users table with updated schema
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `users_new` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT, " +
                "`email` TEXT, " +
                "`password` TEXT, " +
                "`hourlyRate` REAL NOT NULL, " +
                "`isBankSyncEnabled` INTEGER NOT NULL, " +
                "`isSynced` INTEGER NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`updatedAt` TEXT, " +
                "PRIMARY KEY(`id`))"
            );

            // Copy existing data (if any exists, convert updatedAt from long to String)
            database.execSQL(
                "INSERT INTO `users_new` (id, name, email, password, hourlyRate, isBankSyncEnabled, isSynced, isDeleted, updatedAt) " +
                "SELECT id, username, email, password, hourlyRate, isBankSyncEnabled, isSynced, isDeleted, " +
                "datetime(updatedAt/1000, 'unixepoch') " + // Convert milliseconds to ISO 8601
                "FROM `users`"
            );

            // Drop old users table and rename new one
            database.execSQL("DROP TABLE `users`");
            database.execSQL("ALTER TABLE `users_new` RENAME TO `users`");

            // Step 3: Migrate transactions table
            // Create new transactions table with updated schema
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `transactions_new` (" +
                "`id` TEXT NOT NULL, " +
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
                "`updatedAt` TEXT, " +
                "PRIMARY KEY(`id`))"
            );

            // Copy existing data with UUID conversion (old int id to new String id)
            // Note: This generates new UUIDs for existing transactions
            database.execSQL(
                "INSERT INTO `transactions_new` (id, accountId, userId, tagId, amount, type, title, description, timestamp, bankMessageHash, isSynced, isDeleted, updatedAt) " +
                "SELECT " +
                "lower(hex(randomblob(16))), " + // Generate UUID for id
                "NULL, " + // accountId (new field, set to NULL)
                "NULL, " + // userId (new field, set to NULL)
                "tag, " + // Map old 'tag' field to 'tagId'
                "amount, " +
                "'EXPENSE', " + // Default type (new field)
                "title, " +
                "NULL, " + // description (new field, set to NULL)
                "datetime('now'), " + // timestamp (new field, use current time)
                "NULL, " + // bankMessageHash (new field)
                "0, " + // isSynced (new field, default false)
                "0, " + // isDeleted (new field, default false)
                "datetime('now') " + // updatedAt (new field)
                "FROM `transactions`"
            );

            // Drop old transactions table and rename new one
            database.execSQL("DROP TABLE `transactions`");
            database.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`");
        }
    };

    /**
     * Thread-safe Singleton pattern to get database instance.
     * Uses double-checked locking for optimal performance.
     *
     * @param context Application context
     * @return AppDatabase instance
     */
    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    // Apply manual migration from version 1 to 2
                    .addMigrations(MIGRATION_1_2)

                    // For development/testing only - uncomment to reset database on schema changes
                    // .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * For testing purposes only - allows resetting the singleton instance.
     * Should not be used in production code.
     */
    public static void destroyInstance() {
        synchronized (LOCK) {
            if (INSTANCE != null && INSTANCE.isOpen()) {
                INSTANCE.close();
            }
            INSTANCE = null;
        }
    }

    // DAO abstract methods - Add these as you create your DAOs
    /**
     * Provides access to UserDao for user-related database operations.
     * @return UserDao instance
     */
    public abstract UserDao userDao();

    // Example:
    // public abstract AccountDao accountDao();
    // public abstract SharedAccountMemberDao sharedAccountMemberDao();
    // public abstract TagDao tagDao();
    // public abstract LimitDao limitDao();
    // public abstract TransactionDao transactionDao();
}


