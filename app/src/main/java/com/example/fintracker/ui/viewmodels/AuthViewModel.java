package com.example.fintracker.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fintracker.bll.services.AuthResult;
import com.example.fintracker.bll.services.AuthService;

/**
 * AuthViewModel — ViewModel для экранов логина и регистрации.
 *
 * ИСПОЛЬЗОВАНИЕ В ACTIVITY/FRAGMENT:
 *
 *   AuthViewModel vm = new ViewModelProvider(this).get(AuthViewModel.class);
 *
 *   // Наблюдаем за результатом:
 *   vm.getAuthResult().observe(this, result -> {
 *       if (result.isSuccess()) {
 *           startActivity(new Intent(this, DashboardActivity.class));
 *       } else {
 *           Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
 *       }
 *   });
 *
 *   // Кнопка регистрации:
 *   btnRegister.setOnClickListener(v ->
 *       vm.register(etEmail.getText().toString(),
 *                   etUsername.getText().toString(),
 *                   etPassword.getText().toString())
 *   );
 *
 *   // Кнопка входа:
 *   btnLogin.setOnClickListener(v ->
 *       vm.login(etLogin.getText().toString(),
 *                etPassword.getText().toString())
 *   );
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthService authService;
    private final MutableLiveData<AuthResult> authResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authService = new AuthService(application);
    }

    /** LiveData с результатом последней операции аутентификации. */
    public LiveData<AuthResult> getAuthResult() {
        return authResultLiveData;
    }

    /** LiveData для показа прогресс-бара (true = идёт запрос). */
    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }

    /** Запускает регистрацию. Результат придёт в getAuthResult(). */
    public void register(@NonNull String email,
                         @NonNull String username,
                         @NonNull String password) {
        loadingLiveData.setValue(true);
        authService.register(email, username, password, result -> {
            loadingLiveData.setValue(false);
            authResultLiveData.setValue(result);
        });
    }

    /** Запускает вход. Результат придёт в getAuthResult(). */
    public void login(@NonNull String login, @NonNull String password) {
        loadingLiveData.setValue(true);
        authService.login(login, password, result -> {
            loadingLiveData.setValue(false);
            authResultLiveData.setValue(result);
        });
    }

    /** Выходит из аккаунта. */
    public void logout() {
        authService.logout();
    }
}