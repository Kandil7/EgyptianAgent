#!/usr/bin/env pwsh
# Quick ADB Setup for EgyptianAgent

Write-Host "=== EgyptianAgent ADB Setup ===" -ForegroundColor Cyan

# Download location
$downloadUrl = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
$downloadPath = "$env:TEMP\platform-tools.zip"
$installPath = "$env:LOCALAPPDATA\android-sdk-platform-tools"

Write-Host "Downloading ADB from Google..." -ForegroundColor Cyan
try {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $downloadPath -UseBasicParsing
    Write-Host "Download complete" -ForegroundColor Green
} catch {
    Write-Host "Download failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host "Extracting..." -ForegroundColor Cyan
if (Test-Path $installPath) {
    Remove-Item $installPath -Recurse -Force
}
Expand-Archive -Path $downloadPath -DestinationPath $env:TEMP -Force
Move-Item "$env:TEMP\platform-tools" $installPath -Force

# Add to PATH
$env:Path = "$installPath;$env:Path"
[System.Environment]::SetEnvironmentVariable("Path", "$installPath;$([System.Environment]::GetEnvironmentVariable('Path', 'User'))", "User")

Write-Host "`nADB installed at: $installPath" -ForegroundColor Green
Write-Host "Testing ADB..." -ForegroundColor Cyan

& "$installPath\adb.exe" version

Write-Host "`n=== ADB Ready! ===" -ForegroundColor Green
Write-Host "Restart PowerShell or run: `$env:Path = `"$installPath;`$env:Path`"" -ForegroundColor Yellow
