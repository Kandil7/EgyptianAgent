#!/usr/bin/env pwsh
# Install ADB manually without admin rights

$adbDir = "$env:LOCALAPPDATA\android-sdk-platform-tools"
$zipFile = "$env:TEMP\platform-tools.zip"

Write-Host "Creating directory: $adbDir" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $adbDir | Out-Null

Write-Host "Downloading ADB platform-tools..." -ForegroundColor Cyan
Invoke-WebRequest -Uri "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" -OutFile $zipFile

Write-Host "Extracting to $adbDir..." -ForegroundColor Cyan
Expand-Archive -Path $zipFile -DestinationPath $adbDir -Force

Write-Host "Cleaning up..." -ForegroundColor Cyan
Remove-Item $zipFile -Force

# Add to PATH for current session
$env:Path = "$adbDir\platform-tools;" + $env:Path

Write-Host "`n✅ ADB installed successfully to $adbDir\platform-tools" -ForegroundColor Green
Write-Host "Note: Run 'refreshenv' or restart terminal for permanent PATH update" -ForegroundColor Yellow
