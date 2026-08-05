#!/usr/bin/env pwsh
# Fix PATH for EgyptianAgent tools

Write-Host "=== EgyptianAgent PATH Fix ===" -ForegroundColor Cyan

# Find ADB installation
$adbPaths = @(
    "$env:LOCALAPPDATA\android-sdk-platform-tools",
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools",
    "C:\Program Files\Android\platform-tools",
    "$env:ProgramFiles\Android\platform-tools"
)

$adbPath = $null
foreach ($path in $adbPaths) {
    if (Test-Path "$path\adb.exe") {
        $adbPath = $path
        Write-Host "Found ADB at: $adbPath" -ForegroundColor Green
        break
    }
}

if (!$adbPath) {
    Write-Host "ADB not found. Installing..." -ForegroundColor Yellow
    & "$PSScriptRoot\..\setup\windows_setup.ps1" -InstallAdbOnly
    # Try again after install
    foreach ($path in $adbPaths) {
        if (Test-Path "$path\adb.exe") {
            $adbPath = $path
            Write-Host "Found ADB at: $adbPath" -ForegroundColor Green
            break
        }
    }
}

# Add to current session PATH
if ($adbPath -and $env:Path -notlike "*$adbPath*") {
    $env:Path = "$adbPath;$env:Path"
    Write-Host "Added ADB to current session PATH" -ForegroundColor Green
}

# Add to user PATH permanently
if ($adbPath) {
    $currentPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    if ($currentPath -notlike "*$adbPath*") {
        [System.Environment]::SetEnvironmentVariable("Path", "$adbPath;$currentPath", "User")
        Write-Host "Added ADB to user PATH (permanent)" -ForegroundColor Green
    }
}

# Verify
Write-Host "`nTesting ADB..." -ForegroundColor Cyan
try {
    $adbVersion = & adb version 2>&1
    Write-Host "ADB is working!" -ForegroundColor Green
    Write-Host $adbVersion -ForegroundColor Gray
} catch {
    Write-Host "ADB still not working. Please restart PowerShell and try again." -ForegroundColor Red
    Write-Host "`nOr use the full path:" -ForegroundColor Yellow
    Write-Host "& '$adbPath\adb.exe' devices" -ForegroundColor Cyan
}

Write-Host "`n=== Complete ===" -ForegroundColor Cyan
