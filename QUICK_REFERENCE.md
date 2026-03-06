# Quick Reference: Running Tests

## TL;DR - Just Run the Tests

```powershell
.\run-tests.ps1
```

Or if you want to use the wrapper for any Gradle command:

```powershell
.\gradlew.ps1 connectedAndroidTest
```

---

## Important: Android Instrumented Test Limitations

⚠️ **The `--tests` option does NOT work with `connectedAndroidTest`**

### What Works ✅
```powershell
# Run all instrumented tests
.\gradlew.ps1 connectedAndroidTest

# Run unit tests with filter
.\gradlew.ps1 test --tests "com.example.fintracker.UnitTest"
```

### What Doesn't Work ❌
```powershell
# This will FAIL with "Unknown command-line option '--tests'"
.\gradlew.ps1 connectedAndroidTest --tests "com.example.fintracker.AppDatabaseTest"
```

---

## Why?

Android Gradle Plugin's `connectedAndroidTest` task doesn't support test filtering via command line. This is a known limitation.

---

## Solutions for Running Specific Instrumented Tests

### Option 1: Use Android Studio (Recommended)
1. Open `AppDatabaseTest.java`
2. Click the green play button next to the class or specific test method
3. Select "Run 'testName'"

**Advantages:**
- Can run individual tests
- Better debugging support
- Visual test results
- No command-line hassles

### Option 2: Temporarily Comment Out Other Tests
If you have multiple test classes in `androidTest/` and want to run only one:
1. Temporarily rename or comment out other test classes
2. Run `.\gradlew.ps1 connectedAndroidTest`
3. Restore the other test classes

### Option 3: Use Test Annotations
Add `@Ignore` annotation to tests you don't want to run:

```java
import org.junit.Ignore;

@Ignore("Temporarily disabled")
@Test
public void testSomething() {
    // ...
}
```

### Option 4: Run All Tests
Just run all instrumented tests. Since we only have `AppDatabaseTest` right now, this is the simplest approach.

---

## Test Execution Flow

```
.\run-tests.ps1
    ↓
Sets JAVA_HOME automatically
    ↓
Runs: .\gradlew.bat connectedAndroidTest
    ↓
Gradle builds the test APK
    ↓
Installs APK on connected device/emulator
    ↓
Runs ALL tests in app/src/androidTest/
    ↓
Reports results
```

---

## Current Test Suite

As of now, we have:
- **AppDatabaseTest.java** (14 test methods covering User, Account, and Tag operations)
- **ExampleInstrumentedTest.java** (default Android Studio test)

Total: ~15 tests will run

---

## Verifying Test Results

After running tests, check:

1. **Console Output**: Look for "BUILD SUCCESSFUL"
2. **Test Report**: Open `app/build/reports/androidTests/connected/index.html` in a browser
3. **Test Results**: Check `app/build/outputs/androidTest-results/connected/`

---

## Common Issues

### Issue: "No connected devices"
```
> No connected devices!
```

**Solution:** 
- Connect an Android device via USB (enable USB debugging), OR
- Start an emulator: Android Studio → Device Manager → Run emulator

### Issue: Tests fail on device
```
> Process crashed
```

**Solution:**
- Check Logcat for actual error messages
- Ensure device API level matches `minSdk` in build.gradle
- Try cleaning: `.\gradlew.ps1 clean`

### Issue: "JAVA_HOME is not set"
```
ERROR: JAVA_HOME is not set
```

**Solution:**
Always use the helper scripts (`.\run-tests.ps1` or `.\gradlew.ps1`) which set JAVA_HOME automatically.

---

## Pro Tips

1. **Keep tests fast**: In-memory database tests run quickly
2. **Use @Before/@After**: Clean setup/teardown for each test
3. **Test one thing**: Each test should verify a single behavior
4. **Use descriptive names**: `testUserRegistrationWithInvalidEmail` is better than `test1`
5. **Check Logcat**: Real-time logs during test execution show valuable debugging info

---

## Next Steps

After tests pass:
1. ✅ Review test coverage report
2. ✅ Add more edge case tests
3. ✅ Integrate with CI/CD (GitHub Actions, etc.)
4. ✅ Add unit tests for validators (faster than instrumented tests)

---

**Remember:** Use `.\run-tests.ps1` for a hassle-free experience! 🚀

