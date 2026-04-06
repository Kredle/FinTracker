package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shared_account_members")
public class SharedAccountMemberEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String accountId;
    public String userId;
    public String role;

    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;

    public SharedAccountMemberEntity() {
        this.id = "";
        this.accountId = "";
        this.userId = "";
        this.role = "";
        this.updatedAt = "";
    }
}
