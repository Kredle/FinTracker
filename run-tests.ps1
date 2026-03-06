# PowerShell script to run Android instrumented tests
# This script sets up JAVA_HOME automatically using Android Studio's bundled JDK

# Set JAVA_HOME to Android Studio's JBR (JetBrains Runtime)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green
Write-Host ""

# Run the tests
# Note: Android Gradle Plugin doesn't support --tests filter for connectedAndroidTest
# Running all instrumented tests in the project
Write-Host "Running all Android instrumented tests..." -ForegroundColor Cyan
Write-Host "(This includes AppDatabaseTest and any other androidTest classes)" -ForegroundColor Yellow
Write-Host ""
.\gradlew.bat connectedAndroidTest

# Check exit code
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[SUCCESS] Tests completed successfully!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[FAILED] Tests failed. Check output above for details." -ForegroundColor Red
}



