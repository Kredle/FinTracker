package com.example.fintracker.bll.dto;

public class TransactionDto {
    public String title;
    public double amount;

    public TransactionDto(String title, double amount) {
        this.title = title;
        this.amount = amount;
    }
}