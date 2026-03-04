package com.example.fintracker.dal.local.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.example.fintracker.dal.local.entities.TransactionEntity;
import com.example.fintracker.dal.local.entities.UserEntity;

@Database(entities = {UserEntity.class, TransactionEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract com.example.fintracker.dal.local.dao.UserDao userDao();
    public abstract com.example.fintracker.dal.local.dao.TransactionDao transactionDao();
}