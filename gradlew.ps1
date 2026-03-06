# PowerShell script to run Gradle tasks
# This script sets up JAVA_HOME automatically using Android Studio's bundled JDK

# Set JAVA_HOME to Android Studio's JBR (JetBrains Runtime)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green
Write-Host ""

# Check if parameters were provided
if ($args.Count -eq 0) {
    Write-Host "Usage: .\gradlew.ps1 <gradle-task> [options]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Cyan
    Write-Host "  .\gradlew.ps1 assembleDebug"
    Write-Host "  .\gradlew.ps1 connectedAndroidTest"
    Write-Host "  .\gradlew.ps1 test --tests 'com.example.fintracker.UnitTest'"
    Write-Host "  .\gradlew.ps1 clean build"
    Write-Host ""
    Write-Host "Note: Use 'test' for unit tests (supports --tests filter)" -ForegroundColor Yellow
    Write-Host "      Use 'connectedAndroidTest' for instrumented tests (runs all tests)" -ForegroundColor Yellow
    exit 0
}

# Run gradlew with all provided arguments
Write-Host "Running: .\gradlew.bat $args" -ForegroundColor Cyan
Write-Host ""
.\gradlew.bat @args

# Check exit code
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[SUCCESS] Gradle task completed successfully!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[FAILED] Gradle task failed. Check output above for details." -ForegroundColor Red
}



