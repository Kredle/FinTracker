package com.example.fintracker.dal.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Repository layer for user-related operations.
 * Now uses Firestore as primary storage with Room as offline cache.
 */
public class UserRepository {

    private final UserDao userDao;
    private final ExecutorService executorService;
    private final Executor callbackExecutor;
    private final boolean ownsExecutor;
    private final FirebaseFirestore firestore;

    public UserRepository(@NonNull Application application) {
        this(
                AppDatabase.getInstance(application).userDao(),
                RepositoryExecutors.db(),
                RepositoryExecutors.mainThread(),
                false
        );
    }

    public UserRepository(
            @NonNull UserDao userDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor
    ) {
        this(userDao, executorService, callbackExecutor, true);
    }

    private UserRepository(
            @NonNull UserDao userDao,
            @NonNull ExecutorService executorService,
            @NonNull Executor callbackExecutor,
            boolean ownsExecutor
    ) {
        this.userDao = userDao;
        this.executorService = executorService;
        this.callbackExecutor = callbackExecutor;
        this.ownsExecutor = ownsExecutor;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void insertUser(@NonNull final UserEntity user) {
        insertUser(user, null);
    }

    public void insertUser(
            @NonNull final UserEntity user,
            @Nullable final DataCallback<Void> callback
    ) {
        executorService.execute(() -> {
            try {
                if (user == null || user.id == null || user.id.isEmpty()) {
                    if (callback != null) {
                        callbackExecutor.execute(() -> callback.onError(new Exception("Invalid user data")));
                    }
                    return;
                }

                // OFFLINE-FIRST: Save to local database immediately
                android.util.Log.d("UserRepository", "🟡 insertUser: Saving to LOCAL DB immediately (offline-first)");
                try {
                    userDao.insertUser(user);
                    android.util.Log.d("UserRepository", "✅ insertUser: LOCAL DB save SUCCESS");
                    if (callback != null) {
                        callbackExecutor.execute(() -> callback.onSuccess(null));
                    }
                } catch (Exception e) {
                    android.util.Log.e("UserRepository", "❌ insertUser: LOCAL DB save FAILED: " + e.getMessage(), e);
                    if (callback != null) {
                        callbackExecutor.execute(() -> callback.onError(e));
                    }
                    return; // Don't try Firestore if local save fails
                }

                // BACKGROUND: Try to sync to Firestore (non-blocking)
                android.util.Log.d("UserRepository", "🔵 insertUser: Starting async Firestore sync in background");
                firestore.collection("users").document(user.getId())
                        .set(user)
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("UserRepository", "✅ insertUser: Firestore sync SUCCESS");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("UserRepository", "⚠️ insertUser: Firestore sync FAILED (but local DB is saved): " + e.getMessage());
                            // Don't call callback again - user is already saved locally
                        });
            } catch (Exception e) {
                android.util.Log.e("UserRepository", "❌ insertUser: Exception: " + e.getMessage(), e);
                if (callback != null) {
                    callbackExecutor.execute(() -> callback.onError(e));
                }
            }
        });
    }

    public void getUserByEmailOrName(
            @NonNull final String login,
            @NonNull final String password,
            @NonNull final DataCallback<UserEntity> callback
    ) {
        executorService.execute(() -> {
            try {
                // First, try to get the user from local database
                UserEntity localUser = userDao.getUserByEmailOrName(login, password);
                if (localUser != null && !localUser.isDeleted) {
                    android.util.Log.d("UserRepository", "Login successful from local database: " + localUser.name);
                    // User found locally, return it
                    callbackExecutor.execute(() -> callback.onSuccess(localUser));
                    return;
                }

                android.util.Log.d("UserRepository", "User not found locally, checking Firestore for: " + login);

                // If not found locally, try Firestore
                // First, try to get the user from Firestore by email
                firestore.collection("users")
                        .whereEqualTo("email", login)
                        .whereEqualTo("password", password)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                try {
                                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                    UserEntity user = document.toObject(UserEntity.class);
                                    if (user != null && user.id != null && !user.id.isEmpty()) {
                                        android.util.Log.d("UserRepository", "Login successful from Firestore by email: " + user.name);
                                        // Cache the user in the local database using upsert to handle existing users
                                        userDao.upsertUser(user);
                                        callbackExecutor.execute(() -> callback.onSuccess(user));
                                    } else {
                                        android.util.Log.w("UserRepository", "Invalid user data from Firestore by email");
                                        callbackExecutor.execute(() -> callback.onError(new Exception("Невірний логін або пароль")));
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("UserRepository", "Error deserializing user from Firestore by email", e);
                                    callbackExecutor.execute(() -> callback.onError(new Exception("Помилка даних користувача")));
                                }
                            } else {
                                android.util.Log.d("UserRepository", "User not found by email, trying by name");
                                // If not found by email, try by name
                                firestore.collection("users")
                                        .whereEqualTo("name", login)
                                        .whereEqualTo("password", password)
                                        .get()
                                        .addOnCompleteListener(nameTask -> {
                                            if (nameTask.isSuccessful() && !nameTask.getResult().isEmpty()) {
                                                try {
                                                    DocumentSnapshot document = nameTask.getResult().getDocuments().get(0);
                                                    UserEntity user = document.toObject(UserEntity.class);
                                                    if (user != null && user.id != null && !user.id.isEmpty()) {
                                                        android.util.Log.d("UserRepository", "Login successful from Firestore by name: " + user.name);
                                                        // Cache the user in the local database using upsert to handle existing users
                                                        userDao.upsertUser(user);
                                                        callbackExecutor.execute(() -> callback.onSuccess(user));
                                                    } else {
                                                        android.util.Log.w("UserRepository", "Invalid user data from Firestore by name");
                                                        callbackExecutor.execute(() -> callback.onError(new Exception("Невірний логін або пароль")));
                                                    }
                                                } catch (Exception e) {
                                                    android.util.Log.e("UserRepository", "Error deserializing user from Firestore by name", e);
                                                    callbackExecutor.execute(() -> callback.onError(new Exception("Помилка даних користувача")));
                                                }
                                            } else {
                                                android.util.Log.w("UserRepository", "User not found in Firestore by name either");
                                                callbackExecutor.execute(() -> callback.onError(new Exception("Невірний логін або пароль")));
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("UserRepository", "Error querying users by name", e);
                                            callbackExecutor.execute(() -> callback.onError(new Exception("Помилка з'єднання з сервером")));
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("UserRepository", "Error querying users by email", e);
                            callbackExecutor.execute(() -> callback.onError(new Exception("Помилка з'єднання з сервером")));
                        });
            } catch (Exception e) {
                android.util.Log.e("UserRepository", "Exception in getUserByEmailOrName", e);
                callbackExecutor.execute(() -> callback.onError(new Exception("Внутрішня помилка системи")));
            }
        });
    }

    public void checkIfUserExists(
            @NonNull final String email,
            @NonNull final String username,
            @NonNull final DataCallback<Boolean> callback
    ) {
        executorService.execute(() -> {
            try {
                // First check local database
                android.util.Log.d("UserRepository", "🟡 checkIfUserExists: Checking local DB");
                boolean existsLocally = userDao.checkIfUserExists(email, username);
                if (existsLocally) {
                    android.util.Log.d("UserRepository", "✅ checkIfUserExists: User exists in local DB");
                    callbackExecutor.execute(() -> callback.onSuccess(true));
                    return;
                }
                android.util.Log.d("UserRepository", "🔵 checkIfUserExists: User NOT in local DB, checking Firestore");

                // If not found locally, check Firestore with timeout
                final android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                final Runnable timeoutRunnable = () -> {
                    android.util.Log.e("UserRepository", "⏰ checkIfUserExists: Email query TIMEOUT");
                    callbackExecutor.execute(() -> callback.onSuccess(false));
                };

                timeoutHandler.postDelayed(timeoutRunnable, 5000); // 5 second timeout

                android.util.Log.d("UserRepository", "🟡 checkIfUserExists: Starting email query");
                firestore.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnCompleteListener(emailTask -> {
                            android.util.Log.d("UserRepository", "🟢 checkIfUserExists: Email query completed");
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            if (emailTask.isSuccessful() && !emailTask.getResult().isEmpty()) {
                                android.util.Log.d("UserRepository", "✅ checkIfUserExists: User found by email");
                                callbackExecutor.execute(() -> callback.onSuccess(true));
                            } else {
                                android.util.Log.d("UserRepository", "🔵 checkIfUserExists: Email not found, checking by name");
                                // Check by name with timeout
                                final Runnable nameTimeoutRunnable = () -> {
                                    android.util.Log.e("UserRepository", "⏰ checkIfUserExists: Name query TIMEOUT");
                                    callbackExecutor.execute(() -> callback.onSuccess(false));
                                };
                                timeoutHandler.postDelayed(nameTimeoutRunnable, 5000);

                                android.util.Log.d("UserRepository", "🟡 checkIfUserExists: Starting name query");
                                firestore.collection("users")
                                        .whereEqualTo("name", username)
                                        .get()
                                        .addOnCompleteListener(nameTask -> {
                                            android.util.Log.d("UserRepository", "🟢 checkIfUserExists: Name query completed");
                                            timeoutHandler.removeCallbacks(nameTimeoutRunnable);
                                            if (nameTask.isSuccessful()) {
                                                boolean exists = !nameTask.getResult().isEmpty();
                                                android.util.Log.d("UserRepository", exists ? "✅ checkIfUserExists: User found by name" : "❌ checkIfUserExists: User not found");
                                                callbackExecutor.execute(() -> callback.onSuccess(exists));
                                            } else {
                                                android.util.Log.e("UserRepository", "❌ checkIfUserExists: Name query failed");
                                                callbackExecutor.execute(() -> callback.onSuccess(false));
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("UserRepository", "❌ checkIfUserExists: Name query EXCEPTION: " + e.getMessage(), e);
                                            timeoutHandler.removeCallbacks(nameTimeoutRunnable);
                                            callbackExecutor.execute(() -> callback.onSuccess(false));
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("UserRepository", "❌ checkIfUserExists: Email query EXCEPTION: " + e.getMessage(), e);
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            callbackExecutor.execute(() -> callback.onSuccess(false));
                        });
            } catch (Exception e) {
                android.util.Log.e("UserRepository", "❌ checkIfUserExists: Exception: " + e.getMessage(), e);
                callbackExecutor.execute(() -> callback.onError(e));
            }
        });
    }

    public LiveData<UserEntity> getUserById(@NonNull String userId) {
        return userDao.getUserById(userId);
    }

    public LiveData<UserEntity> getUserByEmail(@NonNull String email) {
        return userDao.getUserByEmail(email);
    }

    public void shutdown() {
        if (ownsExecutor) {
            executorService.shutdown();
        }
    }
}
