package com.example.fintracker.bll.services;

public class LimitValidatorService {
    public boolean isLimitExceeded(double currentSpend, double newAmount, double limit) {
        return (currentSpend + newAmount) > limit;
    }
}