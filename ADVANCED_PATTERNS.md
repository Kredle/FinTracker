## 🎓 Advanced Usage Patterns & Best Practices

This guide covers advanced authentication scenarios and production-ready patterns for the FinTracker authentication layer.

---

## 📋 Table of Contents

1. Password Hashing & Security
2. Session Management
3. Error Handling Patterns
4. Performance Optimization
5. Testing Strategies
6. Firebase Integration
7. JWT Token Management
8. Database Migrations

---

## 🔐 Pattern 1: Password Hashing & Security

### Current Implementation (Demo)
```java
// ⚠️ WARNING: Plain text passwords (DEMO ONLY)
user.password = "password123";  // DON'T use in production!
```

### Production Implementation with BCrypt

**Step 1: Add BCrypt Dependency**
```kotlin
// In build.gradle.kts
dependencies {
    // For Android:
    implementation("org.mindrot:jbcrypt:0.4")
    // OR
    implementation("at.favre.lib:bcrypt:0.10.1")
}
```

**Step 2: Create PasswordHasher Utility**
```java
package com.example.fintracker.bll.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    
    /**
     * Hashes a plaintext password using BCrypt.
     * 
     * @param plainPassword The plaintext password
     * @return The hashed password (safe to store in database)
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    
    /**
     * Verifies a plaintext password against a hash.
     * 
     * @param plainPassword The plaintext password to check
     * @param hashedPassword The stored hash from database
     * @return true if password matches the hash
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
```

**Step 3: Update UserEntity**
```java
// In UserEntity.java - add column for password salt (optional)
@Entity(tableName = "users")
public class UserEntity {
    // ... existing fields ...
    public String password;      // Now stores hashed password
    public String passwordSalt;  // Optional: store salt separately
}
```

**Step 4: Update Registration Flow**
```java
private void registerUser(String email, String username, String plainPassword) {
    Executors.newSingleThreadExecutor().execute(() -> {
        try {
            // Validate
            UserValidator.validateRegistration(email, username, plainPassword);
            
            AppDatabase db = AppDatabase.getInstance(this);
            UserDao userDao = db.userDao();
            
            // Check if exists
            if (userDao.checkIfUserExists(email, username)) {
                showError("User already exists");
                return;
            }
            
            // Hash password
            String hashedPassword = PasswordHasher.hashPassword(plainPassword);
            
            // Create user with hashed password
            UserEntity user = new UserEntity();
            user.id = UUID.randomUUID().toString();
            user.email = email;
            user.name = username;
            user.password = hashedPassword;  // ← Hashed, not plain text
            user.updatedAt = Instant.now().toString();
            
            userDao.insertUser(user);
            showSuccess("Registration successful!");
            
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    });
}
```

**Step 5: Update Login Flow**
```java
private void loginUser(String loginInput, String plainPassword) {
    Executors.newSingleThreadExecutor().execute(() -> {
        try {
            AppDatabase db = AppDatabase.getInstance(this);
            UserDao userDao = db.userDao();
            
            // Get user by email or username
            UserEntity user = userDao.getUserByEmail(loginInput);
            if (user == null) {
                user = userDao.getUserByUsername(loginInput);
            }
            
            if (user != null && PasswordHasher.verifyPassword(plainPassword, user.password)) {
                // ✅ Login successful - password matches hash
                showSuccess("Welcome " + user.name);
                saveSessionToken(user.id);
                navigateToMainActivity();
            } else {
                // ❌ Login failed - password doesn't match
                showError("Invalid email/username or password");
            }
            
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    });
}
```

---

## 📱 Pattern 2: Session Management

### Implement Session/Login Token

**Step 1: Create SessionManager**
```java
package com.example.fintracker.bll.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    
    private static final String PREF_NAME = "fintracker_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_LOGIN_TOKEN = "login_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    
    private SharedPreferences preferences;
    
    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Save user session after successful login.
     */
    public void saveSession(String userId, String token) {
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        
        preferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_LOGIN_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime)
            .apply();
    }
    
    /**
     * Get stored user ID (null if not logged in).
     */
    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }
    
    /**
     * Get login token (null if expired or not logged in).
     */
    public String getLoginToken() {
        long expiryTime = preferences.getLong(KEY_TOKEN_EXPIRY, 0);
        
        // Check if token is expired
        if (expiryTime < System.currentTimeMillis()) {
            clearSession();
            return null;
        }
        
        return preferences.getString(KEY_LOGIN_TOKEN, null);
    }
    
    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return getLoginToken() != null;
    }
    
    /**
     * Clear session (logout).
     */
    public void clearSession() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_LOGIN_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply();
    }
}
```

**Step 2: Use SessionManager in Activities**
```java
public class LoginActivity extends AppCompatActivity {
    
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        // Redirect to home if already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
    }
    
    private void loginUser(String loginInput, String password) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                UserEntity user = userDao.getUserByEmailOrName(loginInput, password);
                
                if (user != null) {
                    // Generate login token (in production, use JWT)
                    String token = UUID.randomUUID().toString();
                    
                    // Save session
                    sessionManager.saveSession(user.id, token);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Welcome " + user.name, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    });
                } else {
                    showError("Invalid credentials");
                }
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        });
    }
}
```

**Step 3: Check Session in MainActivity**
```java
public class MainActivity extends AppCompatActivity {
    
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        // Redirect to login if not logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Get current user ID
        String userId = sessionManager.getUserId();
        loadUserProfile(userId);
    }
    
    private void onLogoutClick() {
        sessionManager.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
```

---

## 🛡️ Pattern 3: Comprehensive Error Handling

### Custom Exception Classes
```java
package com.example.fintracker.bll.exceptions;

/**
 * Thrown when validation fails.
 */
public class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}

/**
 * Thrown when user not found in database.
 */
public class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) {
        super(message);
    }
}

/**
 * Thrown when database operation fails.
 */
public class DatabaseException extends Exception {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Enhanced UserValidator with Custom Exceptions
```java
public class UserValidator {
    
    public static boolean isValidEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be null or empty");
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            throw new ValidationException("Email format is invalid");
        }
        return true;
    }
    
    public static boolean isValidUsername(String username) throws ValidationException {
        if (username == null) {
            throw new ValidationException("Username cannot be null");
        }
        if (username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (username.length() < 3) {
            throw new ValidationException("Username must be at least 3 characters");
        }
        if (username.length() > 25) {
            throw new ValidationException("Username must not exceed 25 characters");
        }
        return true;
    }
    
    // ... other methods ...
}
```

### Error Handling in Registration
```java
private void registerUser(String email, String username, String password) {
    Executors.newSingleThreadExecutor().execute(() -> {
        try {
            // Validate input
            UserValidator.validateRegistration(email, username, password);
            
            AppDatabase db = AppDatabase.getInstance(this);
            UserDao userDao = db.userDao();
            
            // Check if user exists
            if (userDao.checkIfUserExists(email, username)) {
                throw new UserNotFoundException("User already exists with this email or username");
            }
            
            // Create and insert user
            UserEntity user = new UserEntity();
            user.id = UUID.randomUUID().toString();
            user.email = email;
            user.name = username;
            user.password = PasswordHasher.hashPassword(password);
            user.updatedAt = Instant.now().toString();
            
            userDao.insertUser(user);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
            
        } catch (ValidationException e) {
            showError("Validation Error: " + e.getMessage());
        } catch (UserNotFoundException e) {
            showError("Account Error: " + e.getMessage());
        } catch (DatabaseException e) {
            showError("Database Error: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected Error: " + e.getMessage());
            Log.e("REGISTRATION", "Unexpected error", e);
        }
    });
}
```

---

## ⚡ Pattern 4: Performance Optimization

### Implement Caching
```java
package com.example.fintracker.bll.managers;

import android.util.LruCache;
import com.example.fintracker.dal.local.entities.UserEntity;

public class UserCache {
    
    private static final int CACHE_SIZE = 10;
    private static UserCache instance;
    private final LruCache<String, UserEntity> cache;
    
    private UserCache() {
        cache = new LruCache<>(CACHE_SIZE);
    }
    
    public static UserCache getInstance() {
        if (instance == null) {
            instance = new UserCache();
        }
        return instance;
    }
    
    /**
     * Get user from cache.
     */
    public UserEntity get(String userId) {
        return cache.get(userId);
    }
    
    /**
     * Put user in cache.
     */
    public void put(String userId, UserEntity user) {
        cache.put(userId, user);
    }
    
    /**
     * Clear cache.
     */
    public void clear() {
        cache.evictAll();
    }
}
```

### Usage Example
```java
// Retrieve user with caching
UserEntity getUser(String userId) {
    // Try cache first
    UserEntity cached = UserCache.getInstance().get(userId);
    if (cached != null) {
        return cached;
    }
    
    // Query database if not in cache
    UserEntity user = userDao.getUserById(userId);
    if (user != null) {
        UserCache.getInstance().put(userId, user);
    }
    return user;
}
```

---

## 🧪 Pattern 5: Testing Strategies

### Unit Test Example
```java
package com.example.fintracker.bll.validators;

import org.junit.Test;
import static org.junit.Assert.*;

public class UserValidatorTest {
    
    @Test
    public void testValidEmail() throws Exception {
        assertTrue(UserValidator.isValidEmail("user@example.com"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEmail() throws Exception {
        UserValidator.isValidEmail("not-an-email");
    }
    
    @Test
    public void testValidUsername() throws Exception {
        assertTrue(UserValidator.isValidUsername("johndoe"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testShortUsername() throws Exception {
        UserValidator.isValidUsername("ab");
    }
    
    @Test
    public void testValidPassword() throws Exception {
        assertTrue(UserValidator.isValidPassword("securepass123"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWeakPassword() throws Exception {
        UserValidator.isValidPassword("12345");
    }
}
```

### Integration Test Example
```java
package com.example.fintracker.dal.local.dao;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.UserEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UserDaoTest {
    
    private AppDatabase db;
    private UserDao userDao;
    
    @Before
    public void setup() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().getTargetContext(),
            AppDatabase.class
        ).build();
        userDao = db.userDao();
    }
    
    @After
    public void tearDown() {
        db.close();
    }
    
    @Test
    public void testInsertAndRetrieve() {
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = "test@example.com";
        user.name = "testuser";
        user.password = "password123";
        
        userDao.insertUser(user);
        
        UserEntity retrieved = userDao.getUserById(user.id);
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.name);
    }
    
    @Test
    public void testLoginWithEmail() {
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = "john@example.com";
        user.name = "john";
        user.password = "password123";
        
        userDao.insertUser(user);
        
        UserEntity loggedIn = userDao.getUserByEmailOrName("john@example.com", "password123");
        assertNotNull(loggedIn);
        assertEquals("john", loggedIn.name);
    }
}
```

---

## 🔗 Pattern 6: Firebase Integration

### Sync User Data with Firebase
```java
package com.example.fintracker.dal.remote.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseSyncHelper {
    
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    
    public FirebaseSyncHelper() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Sync local user with Firebase.
     */
    public void syncUserToFirebase(UserEntity localUser) {
        firestore.collection("users")
            .document(localUser.id)
            .set(localUser)
            .addOnSuccessListener(aVoid -> {
                // Mark as synced
                localUser.isSynced = true;
                updateLocalUser(localUser);
            })
            .addOnFailureListener(e -> {
                Log.e("Firebase", "Sync failed: " + e.getMessage());
            });
    }
    
    /**
     * Sync transactions from Firebase.
     */
    public void syncTransactionsFromFirebase() {
        firestore.collection("transactions")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (var doc : queryDocumentSnapshots) {
                    // Map Firebase data to local entity
                    // Insert into local database
                }
            });
    }
}
```

---

## 🎫 Pattern 7: JWT Token Management

### Implement JWT Authentication
```java
package com.example.fintracker.bll.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtTokenProvider {
    
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    private static final long TOKEN_VALIDITY = 24 * 60 * 60 * 1000; // 24 hours
    
    /**
     * Generate JWT token for user.
     */
    public static String generateToken(String userId) {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY))
            .signWith(SECRET_KEY)
            .compact();
    }
    
    /**
     * Validate JWT token.
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get user ID from token.
     */
    public static String getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(SECRET_KEY)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
```

**Add dependency:**
```kotlin
dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
}
```

---

## 🔄 Pattern 8: Database Migrations

### Create Migration from v2 to v3 (Add password hash)
```java
private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        // Add new column for password hash
        database.execSQL(
            "ALTER TABLE `users` ADD COLUMN `passwordHash` TEXT"
        );
        
        // Update existing passwords (they're currently plain text)
        database.execSQL(
            "UPDATE `users` SET `passwordHash` = `password`"
        );
        
        // In production, you'd hash the passwords here
    }
};
```

**Update AppDatabase:**
```java
@Database(version = 3, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    
    public static AppDatabase getInstance(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "fintracker_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build();
    }
}
```

---

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│            UI Layer (Activities)                │
│  LoginActivity, RegistrationActivity, etc.      │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│        Business Logic Layer (BLL)               │
│  UserValidator, PasswordHasher, SessionManager  │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│        Data Access Layer (DAL)                  │
│  UserDao, Room Database, AppDatabase            │
└──────────────────┬──────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
┌───────▼────────┐    ┌───────▼────────┐
│  Local Storage │    │  Remote Storage│
│  SQLite (Room) │    │  Firebase      │
└────────────────┘    └────────────────┘
```

---

## ✨ Best Practices Summary

1. **Never store plain text passwords** - Use BCrypt or Argon2
2. **Always validate on background thread** - Prevents ANR (Application Not Responding)
3. **Implement session management** - SessionManager for login persistence
4. **Use custom exceptions** - Makes error handling clearer
5. **Cache frequently accessed data** - Improves performance
6. **Write unit and integration tests** - Ensure reliability
7. **Use JWT tokens** - For secure API communication
8. **Plan database migrations** - Handle schema changes gracefully
9. **Log important events** - For debugging and security monitoring
10. **Handle errors gracefully** - Show meaningful messages to users

---

## 🚀 Next Steps

1. **Implement password hashing** - Highest security priority
2. **Add session management** - For better UX
3. **Create comprehensive tests** - Ensure reliability
4. **Plan Firebase integration** - For cloud sync
5. **Implement JWT tokens** - For API security

Good luck with your FinTracker app! 🎉

