package com.example.fintracker.dal.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface TransactionDao {
    @Insert
    long insert(com.example.fintracker.dal.local.entities.TransactionEntity transaction);
}