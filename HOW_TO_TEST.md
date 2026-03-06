# How to Test FinTracker

This guide shows you exactly how to run tests for the FinTracker Android app.

---

## Prerequisites

Before running tests, make sure you have:
- ✅ Android Studio installed
- ✅ An Android device connected via USB (with USB debugging enabled) **OR** an emulator running

### Check if Device is Connected

**Option 1: Using Android Studio (Easiest)**
- Open Android Studio → **Device Manager** (phone icon in toolbar)
- You should see your connected device or running emulator listed

**Option 2: Using ADB (Advanced)**
```powershell
adb devices
```

**Note:** If `adb` command is not found, that's okay! You don't need it to run tests. The helper scripts and Android Studio will detect devices automatically. The `adb` tool is located in Android SDK platform-tools and is optional for testing.

---

## Method 1: Using Helper Script (Easiest)

Simply run this command in PowerShell from the project root:

```powershell
.\run-tests.ps1
```

That's it! The script will:
1. Set up JAVA_HOME automatically
2. Build the app and test APK
3. Install on your device/emulator
4. Run all tests
5. Show you the results

---

## Method 2: Using Android Studio (Recommended for Development)

### Run All Tests
1. In Android Studio, navigate to `app/src/androidTest/java/com/example/fintracker/`
2. Right-click on `AppDatabaseTest.java`
3. Select **Run 'AppDatabaseTest'**
4. Watch the tests execute in the Run panel

### Run a Single Test
1. Open `AppDatabaseTest.java`
2. Find the test you want to run (e.g., `testUserRegistrationAndLogin`)
3. Click the **green play button** (▶) next to the test method
4. Select **Run 'testName'**

### Debug a Test
1. Set a breakpoint by clicking the line number
2. Right-click the test
3. Select **Debug 'testName'**
4. Step through the code using the debugger

---

## Method 3: Manual Command Line

If you prefer full control:

```powershell
# Set JAVA_HOME first
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Run tests
.\gradlew.bat connectedAndroidTest
```

---

## Understanding Test Results

### In Android Studio
After running tests in Android Studio, you'll see:
- ✅ Green checkmarks for passed tests
- ❌ Red X for failed tests
- Click on any test to see details and failure messages

### From Command Line
After running `.\run-tests.ps1`, check:

**1. Console Output**
Look for this at the end:
```
BUILD SUCCESSFUL in 45s
```

**2. HTML Report (Detailed)**
Open this file in your browser:
```
app\build\reports\androidTests\connected\index.html
```

**3. Test Results (Raw Data)**
Check this folder:
```
app\build\outputs\androidTest-results\connected\
```

---

## What Tests Are We Running?

Our test suite (`AppDatabaseTest.java`) includes:

### User Authentication Tests (5 tests)
- ✅ Invalid email rejection
- ✅ Valid email acceptance
- ✅ Username with whitespace rejection
- ✅ User registration and login flow
- ✅ Duplicate user prevention

### Account Management Tests (6 tests)
- ✅ Negative balance rejection
- ✅ NaN balance rejection
- ✅ Infinite balance rejection
- ✅ Account creation and retrieval
- ✅ Account balance update
- ✅ Soft-deleted account update prevention
- ✅ Idempotent account creation

### Tag Management Tests (4 tests)
- ✅ Invalid tag name rejection
- ✅ Tag name with whitespace rejection
- ✅ Tag creation and retrieval
- ✅ Idempotent tag creation
- ✅ Default tags retrieval

**Total: 15+ tests**

---

## Common Issues & Solutions

### ❌ "adb: The term 'adb' is not recognized"

**Problem:** The `adb` command is not in your system PATH.

**Solution:** You don't need `adb` to run tests! Just use:
- `.\run-tests.ps1` (runs tests automatically)
- Android Studio Device Manager to check connected devices
- Android Studio to run tests directly

**Optional:** If you really want to use `adb` commands, add it to PATH:
```powershell
$env:PATH = "$env:PATH;$env:LOCALAPPDATA\Android\Sdk\platform-tools"
```

### ❌ "No connected devices!"

**Problem:** Gradle can't find an Android device or emulator.

**Solution:**
1. Connect your Android phone via USB and enable USB debugging
   - Go to Settings → About Phone → Tap "Build Number" 7 times
   - Go to Settings → Developer Options → Enable "USB Debugging"
   
   OR

2. Start an emulator:
   - Open Android Studio
   - Click **Device Manager** (phone icon in toolbar)
   - Click ▶ next to an emulator to start it
   - Wait for it to fully boot (you should see the home screen)
   - Run tests again

### ❌ "JAVA_HOME is not set"

**Problem:** Java is not configured.

**Solution:** Always use `.\run-tests.ps1` or `.\gradlew.ps1` - they handle this automatically!

### ❌ Tests fail with "Process crashed"

**Problem:** The test app crashed on the device.

**Solutions:**
1. Check if your device API level is compatible (minSdk = 24)
2. View Logcat in Android Studio for error details
3. Try cleaning: `.\gradlew.ps1 clean`
4. Restart the emulator/device

### ❌ "Build failed with an exception"

**Problem:** Compilation error in the code.

**Solutions:**
1. Open the project in Android Studio
2. Look at the "Build" panel for error details
3. Fix any red underlined errors
4. Sync Gradle: File → Sync Project with Gradle Files

### ❌ Tests take forever / hang

**Problem:** Device is slow or unresponsive.

**Solutions:**
1. Restart the emulator
2. Use a faster emulator (with more RAM/CPU cores)
3. Run tests on a physical device instead

---

## Quick Test During Development

When you're actively coding, use this workflow:

1. **Make changes** to your code
2. **Run specific test** in Android Studio by clicking the green play button
3. **See immediate results** - passed or failed
4. **Fix issues** if any
5. **Repeat**

This is much faster than running all tests every time!

---

## Before Committing Code

Before you commit/push code, always run the full test suite:

```powershell
.\run-tests.ps1
```

Make sure all tests pass (BUILD SUCCESSFUL) before pushing!

---

## Tips for Writing Good Tests

1. **Test one thing per test method**
   - ✅ Good: `testNegativeBalanceRejection()`
   - ❌ Bad: `testAccountAndUserStuff()`

2. **Use descriptive names**
   - The test name should tell you what it tests
   - Use `testWhatIsBeingTested_WhenCondition_ThenExpectedResult`

3. **Follow AAA pattern**
   ```java
   @Test
   public void testExample() {
       // Arrange - Set up test data
       String email = "test@example.com";
       
       // Act - Perform the action
       boolean result = UserValidator.isValidEmail(email);
       
       // Assert - Verify the result
       assertTrue(result);
   }
   ```

4. **Clean up after tests**
   - Use `@Before` for setup
   - Use `@After` for cleanup
   - We use in-memory database, so it auto-cleans

---

## Next Level: Continuous Integration (CI)

Want tests to run automatically on every commit? Set up GitHub Actions:

1. Create `.github/workflows/android-tests.yml`
2. Tests will run automatically on every push
3. You'll get notified if tests fail

---

## Summary: The Easy Way

```powershell
# 1. Make sure device/emulator is connected
adb devices

# 2. Run tests
.\run-tests.ps1

# 3. Check results
# Open: app\build\reports\androidTests\connected\index.html
```

That's all you need! 🎉

---

## Need Help?

- **Test code location:** `app/src/androidTest/java/com/example/fintracker/AppDatabaseTest.java`
- **Validators:** `app/src/main/java/com/example/fintracker/bll/validators/`
- **DAOs:** `app/src/main/java/com/example/fintracker/dal/local/dao/`

Read more:
- `QUICK_REFERENCE.md` - Detailed reference
- `RUNNING_TESTS.md` - Full documentation
- `REFACTORING_SUMMARY.md` - What was changed and why



