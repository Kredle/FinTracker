# Testing Cheat Sheet

## The Absolute Easiest Way

Make sure your device/emulator is running, then:

```powershell
.\run-tests.ps1
```

Done! ✓

(Check Android Studio → Device Manager to see connected devices)

---

## Other Ways

### In Android Studio
1. Open `AppDatabaseTest.java`
2. Click green play button ▶
3. Select "Run"

### Manual Command Line
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedAndroidTest
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "adb not recognized" | Don't need it! Use `.\run-tests.ps1` or Android Studio |
| "No connected devices" | Start emulator in Android Studio → Device Manager |
| "JAVA_HOME is not set" | Use `.\run-tests.ps1` instead |
| Tests fail | Check `app\build\reports\androidTests\connected\index.html` |

---

## Test Report Location

```
app\build\reports\androidTests\connected\index.html
```

Open this file in your browser after running tests.

---

## Need More Help?

→ Read **[HOW_TO_TEST.md](HOW_TO_TEST.md)** for complete guide



