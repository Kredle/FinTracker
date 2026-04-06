package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey
    @NonNull
    public String id; // UUID

    public String accountId;
    public String userId;
    public String tagId; // Nullable
    public double amount;
    public String type; // e.g., "INCOME" or "EXPENSE"
    public String title;
    public String description; // Nullable
    public String timestamp;
    public String bankMessageHash; // Nullable

    // For Firebase <=> offline sync
    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;

    // Конструктор без аргументів для Room
    public TransactionEntity() {
        this.id = "";
        this.accountId = "";
        this.userId = "";
        this.tagId = null;
        this.amount = 0.0;
        this.type = "";
        this.title = "";
        this.description = null;
        this.timestamp = "";
        this.bankMessageHash = null;
        this.updatedAt = "";
    }
}