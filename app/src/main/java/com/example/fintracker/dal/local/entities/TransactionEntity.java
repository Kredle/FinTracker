package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String accountId;
    public String userId;
    public String tagId;
    public double amount;
    public String type;
    public String title;
    public String description;
    public String timestamp;
    public String bankMessageHash;

    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;

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

