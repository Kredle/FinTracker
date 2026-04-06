package com.example.fintracker.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fintracker.bll.services.AuthResult;
import com.example.fintracker.bll.services.AuthService;

public class AuthViewModel extends AndroidViewModel {

    private final AuthService authService;
    private final MutableLiveData<AuthResult> authResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authService = new AuthService(application);
    }

    public LiveData<AuthResult> getAuthResult() {
        return authResultLiveData;
    }

    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }

    public void register(@NonNull String email,
                         @NonNull String username,
                         @NonNull String password) {
        loadingLiveData.setValue(true);
        authService.register(email, username, password, result -> {
            loadingLiveData.setValue(false);
            authResultLiveData.setValue(result);
        });
    }

    public void login(@NonNull String login, @NonNull String password) {
        loadingLiveData.setValue(true);
        authService.login(login, password, result -> {
            loadingLiveData.setValue(false);
            authResultLiveData.setValue(result);
        });
    }

    public void logout() {
        authService.logout();
    }
}

