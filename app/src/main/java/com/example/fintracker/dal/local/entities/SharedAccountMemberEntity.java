package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shared_account_members")
public class SharedAccountMemberEntity {
    @PrimaryKey
    @NonNull
    public String id; // UUID

    public String accountId;
    public String userId;
    public String role; // e.g., "ADMIN" or "USER"

    // For Firebase <=> offline sync
    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;
}

