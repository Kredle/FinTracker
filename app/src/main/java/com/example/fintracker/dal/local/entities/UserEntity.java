package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String name;
    public String email;
    public String password;
    public double hourlyRate;
    public boolean isBankSyncEnabled;
    public double generalLimit;

    // For Firebase <=> offline sync
    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;

    // Конструктор без аргументів для Room
    public UserEntity() {
        this.id = "";
        this.name = "";
        this.email = "";
        this.password = "";
        this.hourlyRate = 0.0;
        this.isBankSyncEnabled = false;
        this.generalLimit = 0.0;
        this.isSynced = false;
        this.isDeleted = false;
        this.updatedAt = "";
    }

    // Конструктор з параметрами для зручності
    @Ignore
    public UserEntity(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.hourlyRate = 0.0;
        this.isBankSyncEnabled = false;
        this.generalLimit = 0.0;
        this.isSynced = false;
        this.isDeleted = false;
        this.updatedAt = "";
    }

    // Getter for Firestore
    public String getId() {
        return id;
    }
}