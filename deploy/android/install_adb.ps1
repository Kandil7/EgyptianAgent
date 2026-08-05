# EgyptianAgent ADB Quick Install
# Run this in PowerShell: .\scripts\utils\install_adb.ps1

$ErrorActionPreference = "Stop"
Write-Host "=== EgyptianAgent ADB Install ===" -ForegroundColor Cyan

# Create tools directory
$toolsDir = "$PSScriptRoot\..\..\tools"
$adbDir = "$toolsDir\adb"
Write-Host "Creating tools directory: $toolsDir"
New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

# Download
$downloadUrl = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
$downloadPath = "$toolsDir\platform-tools.zip"
Write-Host "Downloading from: $downloadUrl"
Invoke-WebRequest -Uri $downloadUrl -OutFile $downloadPath -UseBasicParsing
Write-Host "Download complete" -ForegroundColor Green

# Extract
Write-Host "Extracting..."
Expand-Archive -Path $downloadPath -DestinationPath $toolsDir -Force
Write-Host "Extracted" -ForegroundColor Green

# Move to final location
if (Test-Path "$toolsDir\platform-tools\platform-tools") {
    Write-Host "Moving to $adbDir"
    if (Test-Path $adbDir) { Remove-Item $adbDir -Recurse -Force }
    Move-Item "$toolsDir\platform-tools\platform-tools" $adbDir -Force
}

# Cleanup
Write-Host "Cleaning up..."
Remove-Item $downloadPath -Force
Remove-Item "$toolsDir\platform-tools" -Recurse -Force -ErrorAction SilentlyContinue

# Add to PATH for this session
$env:Path = "$adbDir;$env:Path"

# Add to user PATH permanently
$currentUserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentUserPath -notlike "*$adbDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$adbDir;$currentUserPath", "User")
    Write-Host "Added to user PATH" -ForegroundColor Green
}

# Verify
Write-Host "`nTesting ADB..." -ForegroundColor Cyan
& "$adbDir\adb.exe" version

Write-Host "`n=== ADB Installed Successfully! ===" -ForegroundColor Green
Write-Host "ADB location: $adbDir"
Write-Host "`nRestart PowerShell or run: `$env:Path = `"$adbDir;`$env:Path`""
Write-Host "Then run: adb devices"
