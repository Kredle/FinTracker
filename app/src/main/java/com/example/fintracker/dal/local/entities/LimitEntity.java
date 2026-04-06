package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "limits")
public class LimitEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String accountId;
    public String userId;
    public String tagId;
    public double amountLimit;
    public String period;

    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;

    public LimitEntity() {
        this.id = "";
        this.accountId = "";
        this.userId = null;
        this.tagId = null;
        this.amountLimit = 0.0;
        this.period = "";
        this.updatedAt = "";
    }
}
