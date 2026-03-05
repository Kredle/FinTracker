package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class AccountEntity {
    @PrimaryKey
    @NonNull
    public String id; // UUID

    public String name;
    public String ownerId;
    public boolean isShared;
    public double balance;

    // For Firebase <=> offline sync
    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;
}

