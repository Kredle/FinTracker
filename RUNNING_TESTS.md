# Running Tests and Gradle Commands

## Quick Start

Since JAVA_HOME is not set in your system environment, use one of these helper scripts:

### Option 1: Run Tests Directly
```powershell
.\run-tests.ps1
```

### Option 2: Run Any Gradle Command
```powershell
.\gradlew.ps1 <gradle-task> [options]
```

**Examples:**
```powershell
.\gradlew.ps1 assembleDebug
.\gradlew.ps1 connectedAndroidTest
.\gradlew.ps1 test --tests "com.example.fintracker.UnitTest"
.\gradlew.ps1 clean build
```

**Important Note:** The `--tests` filter only works with `test` task (unit tests), not with `connectedAndroidTest` (instrumented tests). When running instrumented tests, all tests in the `androidTest` directory will be executed.

---

## Alternative: Set JAVA_HOME Permanently

If you want to set JAVA_HOME permanently, follow these steps:

### PowerShell Method (User-level)
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "User")
[System.Environment]::SetEnvironmentVariable("PATH", "$env:PATH;C:\Program Files\Android\Android Studio\jbr\bin", "User")
```

### GUI Method
1. Open **System Properties** (Win + Pause/Break or search "Environment Variables")
2. Click **Environment Variables**
3. Under **User variables**, click **New**
4. Variable name: `JAVA_HOME`
5. Variable value: `C:\Program Files\Android\Android Studio\jbr`
6. Click **OK**
7. Edit the **PATH** variable and add: `%JAVA_HOME%\bin`
8. Click **OK** and restart PowerShell

After setting permanently, you can use:
```powershell
.\gradlew.bat <task>
```

---

## Manual Method (Per Session)

If you prefer to set JAVA_HOME manually each time:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat connectedAndroidTest
```

**Note:** This runs all instrumented tests. To run specific tests, use Android Studio's test runner instead.

---

## Running Tests in Android Studio

Alternatively, you can run tests directly from Android Studio:

1. Open `AppDatabaseTest.java`
2. Right-click on the class name or a specific test method
3. Select **Run 'AppDatabaseTest'**
4. Android Studio will handle JAVA_HOME automatically

---

## Test Location

The instrumented tests are located at:
```
app/src/androidTest/java/com/example/fintracker/AppDatabaseTest.java
```

---

## Common Gradle Tasks

| Task | Description |
|------|-------------|
| `.\gradlew.ps1 assembleDebug` | Build debug APK |
| `.\gradlew.ps1 assembleRelease` | Build release APK |
| `.\gradlew.ps1 connectedAndroidTest` | Run all instrumented tests |
| `.\gradlew.ps1 test` | Run all unit tests |
| `.\gradlew.ps1 clean` | Clean build artifacts |
| `.\gradlew.ps1 build` | Build project (debug + release) |

---

## Troubleshooting

### Issue: "JAVA_HOME is not set"
**Solution**: Use the helper scripts (`gradlew.ps1` or `run-tests.ps1`) or set JAVA_HOME permanently.

### Issue: "No connected devices"
**Solution**: 
- Connect an Android device via USB with USB debugging enabled, OR
- Start an Android Emulator from Android Studio

### Issue: Tests fail to compile
**Solution**: 
1. Sync Gradle: `.\gradlew.ps1 build --refresh-dependencies`
2. Clean and rebuild: `.\gradlew.ps1 clean build`

### Issue: PowerShell script execution disabled
**Solution**: Run this once:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## CI/CD Integration

For automated builds in CI/CD pipelines, ensure JAVA_HOME is set in your pipeline configuration:

**GitHub Actions Example:**
```yaml
- name: Set up JDK
  uses: actions/setup-java@v3
  with:
    distribution: 'temurin'
    java-version: '11'

- name: Run Tests
  run: ./gradlew connectedAndroidTest
```

---

## Notes

- The helper scripts automatically use Android Studio's bundled JDK (JBR)
- JBR location: `C:\Program Files\Android\Android Studio\jbr`
- These scripts only set JAVA_HOME for the current PowerShell session
- For permanent setup, use the GUI or PowerShell method described above



