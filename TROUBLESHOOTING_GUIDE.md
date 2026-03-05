## 🐛 Authentication Layer - Troubleshooting Guide

This guide helps resolve common issues when working with the new authentication system.

---

## 🔴 Common Issues & Solutions

### Issue 1: "Cannot resolve symbol 'UserValidator'"

**Error Message:**
```
Cannot resolve symbol 'UserValidator'
```

**Root Causes:**
1. Package structure is incorrect
2. Gradle hasn't recompiled
3. IDE cache is stale

**Solutions:**

**Solution A: Rebuild Project**
```
File → Invalidate Caches → Clear cache and restart
OR
Build → Clean Project → Build Project
OR
Press: Ctrl + Shift + F9
```

**Solution B: Verify File Location**
```
Expected path:
app/src/main/java/com/example/fintracker/bll/validators/UserValidator.java

If file is elsewhere:
1. Delete the file
2. Recreate it in correct location
3. Rebuild project
```

**Solution C: Check Import Statement**
```java
// Correct:
import com.example.fintracker.bll.validators.UserValidator;

// If IDE offers autocomplete, accept it to ensure correct import
```

---

### Issue 2: "Cannot resolve symbol 'UserDao'"

**Error Message:**
```
Cannot resolve symbol 'UserDao'
```

**Root Causes:**
1. UserDao.java not created
2. Package path incorrect
3. Room annotation processor didn't run

**Solutions:**

**Solution A: Verify File Exists**
```
Expected path:
app/src/main/java/com/example/fintracker/dal/local/dao/UserDao.java
```

**Solution B: Force Annotation Processing**
```
1. Go to Build → Clean Project
2. Delete build/ and .gradle/ folders (optional)
3. Go to Build → Rebuild Project
4. Wait for "BUILD SUCCESSFUL" message
```

**Solution C: Check AppDatabase Import**
```java
// AppDatabase.java should have:
import com.example.fintracker.dal.local.dao.UserDao;

// And expose the DAO:
public abstract UserDao userDao();
```

---

### Issue 3: "Room cannot resolve column name 'password'"

**Error Message:**
```
Room cannot resolve column name 'password'
There is a mismatch between the schema (file *.db) and the java class
```

**Root Causes:**
1. Database schema changed but Room can't find migration
2. Schema JSON not exported
3. AppDatabase version mismatch

**Solutions:**

**Solution A: Verify Schema Export Configuration**

Check your `build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        // Room schema export configuration
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
    }
}
```

**Solution B: Check Migration**

Verify `AppDatabase.java` has:
```java
@Database(
    entities = {
        UserEntity.class,
        AccountEntity.class,
        // ... other entities
    },
    version = 2,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Migration SQL here
        }
    };
    
    // ...
    
    public static AppDatabase getInstance(Context context) {
        // ...
        .addMigrations(MIGRATION_1_2)
        // ...
    }
}
```

**Solution C: Reset Database (Development Only)**

```java
// In AppDatabase.getInstance():
INSTANCE = Room.databaseBuilder(
        context.getApplicationContext(),
        AppDatabase.class,
        DATABASE_NAME
)
.addMigrations(MIGRATION_1_2)
.fallbackToDestructiveMigration()  // ← Add this temporarily
.build();
```

Then remove `.fallbackToDestructiveMigration()` after testing.

---

### Issue 4: Logcat Shows No Output

**Problem:** Test script runs but no log messages appear

**Root Causes:**
1. Logcat filter is wrong
2. App isn't actually running
3. Breakpoint is stopping execution
4. Logging tag doesn't match

**Solutions:**

**Solution A: Check Logcat Filter**
```
1. Open Logcat (bottom of Android Studio)
2. Look for filter dropdown (currently may show "Show only selected application")
3. Click dropdown and select your app: "com.example.fintracker"
4. In filter field, type: "AUTH_TEST"
5. Should see logs starting with "D/AUTH_TEST:"
```

**Solution B: Verify App is Running**
```
1. Check if emulator/device shows the app is open
2. Look for "D/AUTH_TEST:" in logcat
3. If nothing appears, run app again with Shift+F10
4. Wait 2-3 seconds for logs to appear
```

**Solution C: Check Breakpoints**
```
Run menu → Remove All Breakpoints
OR
Press Ctrl+Shift+F8 to see all breakpoints and disable them
```

**Solution D: Verify onCreate() is Called**
```java
// Add this temporary debug line to verify onCreate runs:
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("DEBUG", "LoginActivity onCreate called");  // ← Add this
    runAuthenticationTestScript();
}
```

---

### Issue 5: "User already exists" on Every Run

**Problem:** Second run always shows duplicate user error

**Root Causes:**
1. Database persists between app runs (normal behavior)
2. User from previous test still in database
3. Trying to insert same user data twice

**Solutions:**

**Solution A: Delete App Data**
```
1. Settings → Apps → FinTracker
2. Storage & Cache → Clear Storage
3. Rerun app
```

**Solution B: Uninstall & Reinstall**
```
1. Run → Stop
2. Build → Clean Project
3. Run app again (Shift+F10)
4. This forces complete reinstall
```

**Solution C: Reset Database via Android Studio**
```
1. View → Tool Windows → App Inspection
2. Database Inspector → fintracker_db → users
3. Right-click on table → Delete All
4. Rerun app
```

**Solution D: Modify Test Script to Handle Duplicates**

In `LoginActivity.java`, modify Test 2:
```java
// Check if user exists
boolean userExists = userDao.checkIfUserExists(email, username);
if (userExists) {
    Log.d(TAG, "⚠️  User already exists in database, skipping insertion");
} else {
    // Insert new user
    userDao.insertUser(newUser);
    Log.d(TAG, "✅ PASSED: User successfully registered!");
}
```

---

### Issue 6: "Syntax error in Room SQL query"

**Error Message:**
```
Syntax error in Room SQL query: ...
```

**Root Causes:**
1. Invalid SQL in @Query annotation
2. Wrong parameter syntax
3. Missing LIMIT or other keywords

**Solutions:**

**Solution A: Check UserDao Query**

Verify in `UserDao.java`:
```java
// ✅ CORRECT:
@Query("SELECT * FROM users WHERE (email = :login OR name = :login) AND password = :password LIMIT 1")
UserEntity getUserByEmailOrName(String login, String password);

// ❌ WRONG (no LIMIT):
@Query("SELECT * FROM users WHERE (email = :login OR name = :login) AND password = :password")

// ❌ WRONG (wrong parameter name):
@Query("SELECT * FROM users WHERE (email = :EMAIL OR name = :USERNAME) AND password = :PASSWORD")
UserEntity getUserByEmailOrName(String login, String password);  // Parameter names don't match!
```

**Solution B: Verify Colon Syntax**

In Room queries, use `:paramName` for parameters:
```java
// ✅ CORRECT:
@Query("SELECT * FROM users WHERE email = :email")

// ❌ WRONG (using ? instead of :name):
@Query("SELECT * FROM users WHERE email = ?")

// ❌ WRONG (missing colon):
@Query("SELECT * FROM users WHERE email = email")
```

---

### Issue 7: Thread Exception - "Cannot access database on main thread"

**Error Message:**
```
java.lang.IllegalStateException: Cannot access database on the main thread
```

**Root Causes:**
1. Database query called on main thread
2. Missing Executors.newSingleThreadExecutor()
3. Missing runOnUiThread() when updating UI

**Solutions:**

**Solution A: Always Use Background Thread**
```java
// ✅ CORRECT:
Executors.newSingleThreadExecutor().execute(() -> {
    // All Room database calls here
    UserDao userDao = AppDatabase.getInstance(context).userDao();
    UserEntity user = userDao.getUserById(id);
});

// ❌ WRONG (on main thread):
UserDao userDao = AppDatabase.getInstance(context).userDao();
UserEntity user = userDao.getUserById(id);  // CRASH!
```

**Solution B: Use runOnUiThread() for UI Updates**
```java
// ✅ CORRECT:
Executors.newSingleThreadExecutor().execute(() -> {
    // Background thread - do database work
    UserEntity user = userDao.getUserById(id);
    
    // Switch back to main thread for UI updates
    runOnUiThread(() -> {
        textView.setText(user.name);
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
    });
});
```

**Solution C: Fix LoginActivity Test Script**

Test script already has this correctly:
```java
// ✅ Already correct:
Executors.newSingleThreadExecutor().execute(() -> {
    AppDatabase db = AppDatabase.getInstance(LoginActivity.this);
    UserDao userDao = db.userDao();
    // All database operations in here
});
```

---

### Issue 8: "IllegalArgumentException: Email cannot be null or empty"

**Problem:** Validation fails even with valid input

**Root Causes:**
1. Trim() removes all characters
2. EditText has leading/trailing whitespace
3. Empty string being passed

**Solutions:**

**Solution A: Debug Input Values**
```java
String email = emailEditText.getText().toString().trim();
String username = usernameEditText.getText().toString().trim();
String password = passwordEditText.getText().toString();

Log.d("DEBUG", "Email: '" + email + "' (length: " + email.length() + ")");
Log.d("DEBUG", "Username: '" + username + "' (length: " + username.length() + ")");
Log.d("DEBUG", "Password: '" + password + "' (length: " + password.length() + ")");

UserValidator.validateRegistration(email, username, password);
```

**Solution B: Verify EditText has Content**
```java
if (email.isEmpty()) {
    Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
    return;
}

UserValidator.isValidEmail(email);
```

---

### Issue 9: Build Fails with "Could not find androidx.room:room-compiler"

**Error Message:**
```
Could not find androidx.room:room-compiler:x.x.x
```

**Root Causes:**
1. Room dependency not in build.gradle.kts
2. Wrong repository configured
3. Gradle sync failed

**Solutions:**

**Solution A: Check Dependencies**

Your `build.gradle.kts` should have:
```kotlin
dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.common.jvm)
    annotationProcessor(libs.room.compiler)  // ← Most important
}
```

**Solution B: Sync Gradle**
```
File → Sync Now
OR
Right-click on "Gradle Scripts" → Sync Now
```

**Solution C: Check libs.versions.toml**

In `gradle/libs.versions.toml`:
```toml
[versions]
room = "2.x.x"  # Use your version

[libraries]
room-common-jvm = { group = "androidx.room", name = "room-common-jvm", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
```

---

### Issue 10: "Duplicate class warnings"

**Error Message:**
```
warning: Type com.example.fintracker.dal.local.dao.UserDao
  defined multiple times
```

**Root Causes:**
1. UserDao.java file created in two locations
2. Cache not cleared
3. Build artifacts corrupted

**Solutions:**

**Solution A: Find and Remove Duplicates**
```
1. Ctrl+Shift+F (Find in Files)
2. Search for: "class UserDao"
3. Check for duplicate files
4. Delete all but one (correct location)
5. File → Invalidate Caches → Clear Cache and Restart
```

**Solution B: Clean Build**
```
Build → Clean Project
Build → Rebuild Project
```

---

## ✅ Verification Checklist

After applying a fix, verify with this checklist:

- [ ] Project compiles without errors (green checkmark in gutter)
- [ ] No red squiggly lines under class names
- [ ] Logcat filter shows "AUTH_TEST" tag
- [ ] All 4 test cases show ✅ PASSED
- [ ] Database Inspector shows users table with data
- [ ] No "Cannot access database on main thread" exceptions
- [ ] Test runs to completion without hanging

---

## 📞 Getting Help

If you're still stuck:

1. **Check the exact error message** - Google it!
2. **Search GitHub Issues** - Room database issues are well documented
3. **Check Logcat** - Scroll up to find the root cause
4. **Rebuild project** - 90% of issues resolve with a clean build
5. **Restart Android Studio** - IDE cache corruption is common

---

## 🔍 Debug Tips

### Enable Verbose Logging
```java
Log.d("AUTH_DEBUG", "Starting validation for email: " + email);
Log.d("AUTH_DEBUG", "Database instance: " + db);
Log.d("AUTH_DEBUG", "UserDao instance: " + userDao);
```

### Use Android Studio Debugger
```
1. Click line number to add breakpoint
2. Run app with Shift+F9 (Debug mode)
3. Execution stops at breakpoint
4. Inspect variables and step through code
```

### Use Database Inspector
```
1. View → Tool Windows → App Inspection
2. Click Database Inspector tab
3. Browse tables and data in real-time
4. Run SQL queries directly
```

### Check Emulator/Device Logs
```
adb logcat | grep AUTH_TEST
```

---

## 🎯 Quick Reference

| Error | Solution |
|-------|----------|
| "Cannot resolve symbol" | Rebuild project (Ctrl+Shift+F9) |
| No Logcat output | Filter by "AUTH_TEST" tag |
| "User already exists" | Clear app data or uninstall |
| Main thread error | Use Executors.newSingleThreadExecutor() |
| Invalid SQL syntax | Check @Query parameters (use :name) |
| Build fails | Sync Gradle and check dependencies |

---

Happy debugging! 🚀

