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

    // Конструктор без аргументів для Room
    public AccountEntity() {
        this.id = "";
        this.name = "";
        this.ownerId = "";
        this.isShared = false;
        this.balance = 0.0;
        this.updatedAt = "";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AccountEntity that = (AccountEntity) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
