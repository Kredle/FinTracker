package com.example.fintracker.dal.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tags")
public class TagEntity {
    @PrimaryKey
    @NonNull
    public String id; // UUID

    public String name;
    public String iconName;
    public String ownerId;

    // For Firebase <=> offline sync
    public boolean isSynced = false;
    public boolean isDeleted = false;
    public String updatedAt;

    // Конструктор без аргументів для Room
    public TagEntity() {
        this.id = "";
        this.name = "";
        this.iconName = "";
        this.ownerId = "";
        this.updatedAt = "";
    }
}
