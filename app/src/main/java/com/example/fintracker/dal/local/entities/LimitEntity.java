package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "limits")
public class LimitEntity {
    @PrimaryKey
    @NonNull
    public String id; // UUID

    public String accountId;
    public String userId; // Nullable
    public String tagId; // Nullable
    public double amountLimit;
    public String period;

    // For Firebase <=> offline sync
    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;
}

