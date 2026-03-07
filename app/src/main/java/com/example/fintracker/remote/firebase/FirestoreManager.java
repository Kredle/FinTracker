package com.example.fintracker.remote.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fintracker.dal.local.entities.AccountEntity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * FirestoreManager handles all Cloud Firestore operations.
 * Manages syncing local entities to the cloud.
 */
public class FirestoreManager {

    private static final String TAG = "FIREBASE_TEST";
    private final FirebaseFirestore firestore;

    public FirestoreManager() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Syncs an account to Cloud Firestore.
     *
     * @param account The AccountEntity to sync
     */
    public void syncAccountToCloud(@NonNull AccountEntity account) {
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("id", account.id);
        accountData.put("name", account.name);
        accountData.put("ownerId", account.ownerId);
        accountData.put("isShared", account.isShared);
        accountData.put("balance", account.balance);
        accountData.put("isSynced", account.isSynced);
        accountData.put("isDeleted", account.isDeleted);
        accountData.put("updatedAt", account.updatedAt);

        firestore.collection("accounts")
                .document(account.id)
                .set(accountData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "SUCCESS: Account synced to cloud!");
                    Log.d(TAG, "Account ID: " + account.id);
                    Log.d(TAG, "Account Name: " + account.name);
                    Log.d(TAG, "Balance: " + account.balance);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FAILED: Could not sync account to cloud", e);
                    Log.e(TAG, "Error message: " + e.getMessage());
                });
    }
}

