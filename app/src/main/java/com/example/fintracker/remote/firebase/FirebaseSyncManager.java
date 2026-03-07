package com.example.fintracker.remote.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fintracker.dal.local.dao.AccountDao;
import com.example.fintracker.dal.local.dao.TagDao;
import com.example.fintracker.dal.local.dao.TransactionDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * FirebaseSyncManager handles one-way synchronization of unsynced local Room data
 * to Cloud Firestore using synchronous operations.
 */
public class FirebaseSyncManager {

    private static final String TAG = "FIREBASE_SYNC";
    private final FirebaseFirestore firestore;
    private final AccountDao accountDao;
    private final TagDao tagDao;
    private final TransactionDao transactionDao;

    public FirebaseSyncManager(@NonNull Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        AppDatabase database = AppDatabase.getInstance(context);
        this.accountDao = database.accountDao();
        this.tagDao = database.tagDao();
        this.transactionDao = database.transactionDao();
    }

    /**
     * Syncs all unsynced local data to Cloud Firestore synchronously.
     *
     * @return true when all writes succeed, false if any part of the sync fails
     */
    public boolean syncUnsyncedDataToCloud() {
        Log.d(TAG, "Starting synchronous sync of unsynced data to cloud...");

        boolean accountsOk = syncUnsyncedAccounts();
        boolean tagsOk = syncUnsyncedTags();
        boolean transactionsOk = syncUnsyncedTransactions();
        boolean allSynced = accountsOk && tagsOk && transactionsOk;

        if (allSynced) {
            Log.d(TAG, "Sync process completed for all entities");
        } else {
            Log.w(TAG, "Sync completed with failures; unsynced data remains for retry");
        }
        return allSynced;
    }

    private boolean syncUnsyncedAccounts() {
        boolean allSynced = true;
        try {
            List<AccountEntity> unsyncedAccounts = accountDao.getUnsyncedAccounts();
            Log.d(TAG, "Found " + unsyncedAccounts.size() + " unsynced accounts");

            for (AccountEntity account : unsyncedAccounts) {
                try {
                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("id", account.id);
                    accountData.put("name", account.name);
                    accountData.put("ownerId", account.ownerId);
                    accountData.put("isShared", account.isShared);
                    accountData.put("balance", account.balance);
                    accountData.put("isSynced", true);
                    accountData.put("isDeleted", account.isDeleted);
                    accountData.put("updatedAt", account.updatedAt);

                    Tasks.await(firestore.collection("accounts")
                            .document(account.id)
                            .set(accountData));

                    account.isSynced = true;
                    accountDao.updateAccount(account);
                    Log.d(TAG, "Account synced: " + account.name + " (ID: " + account.id + ")");
                } catch (ExecutionException | InterruptedException e) {
                    allSynced = false;
                    Log.e(TAG, "Failed to sync account: " + account.name, e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching unsynced accounts", e);
            return false;
        }
        return allSynced;
    }

    private boolean syncUnsyncedTags() {
        boolean allSynced = true;
        try {
            List<TagEntity> unsyncedTags = tagDao.getUnsyncedTags();
            Log.d(TAG, "Found " + unsyncedTags.size() + " unsynced tags");

            for (TagEntity tag : unsyncedTags) {
                try {
                    Map<String, Object> tagData = new HashMap<>();
                    tagData.put("id", tag.id);
                    tagData.put("name", tag.name);
                    tagData.put("iconName", tag.iconName);
                    tagData.put("ownerId", tag.ownerId);
                    tagData.put("isSynced", true);
                    tagData.put("isDeleted", tag.isDeleted);
                    tagData.put("updatedAt", tag.updatedAt);

                    Tasks.await(firestore.collection("tags")
                            .document(tag.id)
                            .set(tagData));

                    tag.isSynced = true;
                    tagDao.updateTag(tag);
                    Log.d(TAG, "Tag synced: " + tag.name + " (ID: " + tag.id + ")");
                } catch (ExecutionException | InterruptedException e) {
                    allSynced = false;
                    Log.e(TAG, "Failed to sync tag: " + tag.name, e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching unsynced tags", e);
            return false;
        }
        return allSynced;
    }

    private boolean syncUnsyncedTransactions() {
        boolean allSynced = true;
        try {
            List<TransactionEntity> unsyncedTransactions = transactionDao.getUnsyncedTransactions();
            Log.d(TAG, "Found " + unsyncedTransactions.size() + " unsynced transactions");

            for (TransactionEntity transaction : unsyncedTransactions) {
                try {
                    Map<String, Object> transactionData = new HashMap<>();
                    transactionData.put("id", transaction.id);
                    transactionData.put("accountId", transaction.accountId);
                    transactionData.put("userId", transaction.userId);
                    transactionData.put("tagId", transaction.tagId);
                    transactionData.put("amount", transaction.amount);
                    transactionData.put("type", transaction.type);
                    transactionData.put("title", transaction.title);
                    transactionData.put("description", transaction.description);
                    transactionData.put("timestamp", transaction.timestamp);
                    transactionData.put("bankMessageHash", transaction.bankMessageHash);
                    transactionData.put("isSynced", true);
                    transactionData.put("isDeleted", transaction.isDeleted);
                    transactionData.put("updatedAt", transaction.updatedAt);

                    Tasks.await(firestore.collection("transactions")
                            .document(transaction.id)
                            .set(transactionData));

                    transaction.isSynced = true;
                    transactionDao.updateTransaction(transaction);
                    Log.d(TAG, "Transaction synced: " + transaction.title + " (ID: " + transaction.id + ")");
                } catch (ExecutionException | InterruptedException e) {
                    allSynced = false;
                    Log.e(TAG, "Failed to sync transaction: " + transaction.title, e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching unsynced transactions", e);
            return false;
        }
        return allSynced;
    }
}
