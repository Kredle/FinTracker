package com.example.fintracker.remote.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.AccountInvitationEntity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
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

    /**
     * Syncs an account invitation to Cloud Firestore.
     *
     * @param invitation The AccountInvitationEntity to sync
     */
    public void syncInvitationToCloud(@NonNull AccountInvitationEntity invitation) {
        Map<String, Object> invitationData = new HashMap<>();
        invitationData.put("id", invitation.id);
        invitationData.put("accountId", invitation.accountId);
        invitationData.put("fromUserId", invitation.fromUserId);
        invitationData.put("toUserEmail", invitation.toUserEmail);
        invitationData.put("status", invitation.status);
        invitationData.put("createdAt", invitation.createdAt);
        invitationData.put("respondedAt", invitation.respondedAt);
        invitationData.put("isSynced", invitation.isSynced);
        invitationData.put("isDeleted", invitation.isDeleted);
        invitationData.put("updatedAt", invitation.updatedAt);

        firestore.collection("account_invitations")
                .document(invitation.id)
                .set(invitationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "SUCCESS: Invitation synced to cloud!");
                    Log.d(TAG, "Invitation ID: " + invitation.id);
                    Log.d(TAG, "To Email: " + invitation.toUserEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FAILED: Could not sync invitation to cloud", e);
                    Log.e(TAG, "Error message: " + e.getMessage());
                });
    }

    /**
     * Fetches pending invitations for a user from Cloud Firestore.
     *
     * @param email The user's email
     * @param callback Callback with list of invitations
     */
    public void getPendingInvitationsForUser(String email, com.example.fintracker.bll.services.AccountInvitationService.Callback<List<AccountInvitationEntity>> callback) {
        firestore.collection("account_invitations")
                .whereEqualTo("toUserEmail", email)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AccountInvitationEntity> invitations = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        AccountInvitationEntity invitation = new AccountInvitationEntity();
                        invitation.id = doc.getString("id");
                        invitation.accountId = doc.getString("accountId");
                        invitation.fromUserId = doc.getString("fromUserId");
                        invitation.toUserEmail = doc.getString("toUserEmail");
                        invitation.status = doc.getString("status");
                        invitation.createdAt = doc.getString("createdAt");
                        invitation.respondedAt = doc.getString("respondedAt");
                        invitation.isSynced = doc.getBoolean("isSynced") != null ? doc.getBoolean("isSynced") : false;
                        invitation.isDeleted = doc.getBoolean("isDeleted") != null ? doc.getBoolean("isDeleted") : false;
                        invitation.updatedAt = doc.getString("updatedAt");
                        invitations.add(invitation);
                    }
                    Log.d(TAG, "Fetched " + invitations.size() + " pending invitations for " + email);
                    if (callback != null) {
                        callback.onSuccess(invitations);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FAILED: Could not fetch invitations from cloud", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
}
