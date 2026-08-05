#!/usr/bin/env pwsh
# Verify all installations

$env:Path = "C:\Users\amazon\AppData\Local\jdk\jdk-17.0.14+7\bin;C:\Users\amazon\AppData\Local\gradle\gradle-8.5\bin;C:\Users\amazon\AppData\Local\android-sdk-platform-tools\platform-tools;" + $env:Path

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   EGYPTIANAGENT PREREQUISITES VERIFICATION" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$allPassed = $true

# Java JDK 17 Verification
Write-Host "--- Java (JDK 17) ---" -ForegroundColor Yellow
try {
    $javaOutput = & "C:\Users\amazon\AppData\Local\jdk\jdk-17.0.14+7\bin\java.exe" -version 2>&1
    Write-Host $javaOutput -ForegroundColor Green
    Write-Host "✅ Java JDK 17: PASSED" -ForegroundColor Green
} catch {
    Write-Host "❌ Java JDK 17: FAILED - $_" -ForegroundColor Red
    $allPassed = $false
}

# ADB Verification
Write-Host "`n--- ADB (Android Debug Bridge) ---" -ForegroundColor Yellow
try {
    $adbOutput = & "C:\Users\amazon\AppData\Local\android-sdk-platform-tools\platform-tools\adb.exe" version 2>&1
    Write-Host $adbOutput -ForegroundColor Green
    Write-Host "✅ ADB: PASSED" -ForegroundColor Green
} catch {
    Write-Host "❌ ADB: FAILED - $_" -ForegroundColor Red
    $allPassed = $false
}

# Gradle Wrapper Verification
Write-Host "`n--- Gradle Wrapper ---" -ForegroundColor Yellow
try {
    Set-Location "K:\business\projects_v2\EgyptianAgent"
    $gradleOutput = .\gradlew.bat --version 2>&1
    Write-Host $gradleOutput -ForegroundColor Green
    Write-Host "✅ Gradle Wrapper: PASSED" -ForegroundColor Green
} catch {
    Write-Host "❌ Gradle Wrapper: FAILED - $_" -ForegroundColor Red
    $allPassed = $false
}

# Git Verification
Write-Host "`n--- Git ---" -ForegroundColor Yellow
try {
    $gitOutput = git --version 2>&1
    Write-Host $gitOutput -ForegroundColor Green
    Write-Host "✅ Git: PASSED" -ForegroundColor Green
} catch {
    Write-Host "❌ Git: FAILED - $_" -ForegroundColor Red
    $allPassed = $false
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "   ALL INSTALLATIONS COMPLETE!" -ForegroundColor Green
} else {
    Write-Host "   SOME INSTALLATIONS FAILED!" -ForegroundColor Red
}
Write-Host "========================================`n" -ForegroundColor Cyan
