package com.example.fintracker.dal.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fintracker.dal.local.entities.LimitEntity;

import java.util.List;

@Dao
public interface LimitDao {

    @Insert
    void insertLimit(@NonNull LimitEntity limit);

    @Query("SELECT * FROM limits WHERE accountId = :accountId AND isDeleted = 0")
    List<LimitEntity> getLimitsByAccountId(@NonNull String accountId);

    @Query("SELECT * FROM limits WHERE accountId = :accountId AND tagId = :tagId AND isDeleted = 0 LIMIT 1")
    @Nullable
    LimitEntity getLimitByAccountAndTag(@NonNull String accountId, @NonNull String tagId);
}

