# EgyptianAgent ADB Quick Install - Fixed Version
# Run this in PowerShell: .\scripts\utils\install_adb_simple.ps1

$ErrorActionPreference = "Stop"
Write-Host "=== EgyptianAgent ADB Install ===" -ForegroundColor Cyan

# Use the already installed ADB from Android SDK
$adbSource = "C:\Android\Sdk\platform-tools"
$projectToolsDir = "$PSScriptRoot\..\..\tools\adb"

if (Test-Path "$adbSource\adb.exe") {
    Write-Host "ADB found at Android SDK, copying to project..." -ForegroundColor Green
    
    # Create project tools directory
    New-Item -ItemType Directory -Force -Path $projectToolsDir | Out-Null
    
    # Copy ADB files
    Copy-Item -Path "$adbSource\*" -Destination $projectToolsDir -Recurse -Force
    Write-Host "ADB copied to: $projectToolsDir" -ForegroundColor Green
    
    # Add to PATH for this session
    $env:Path = "$projectToolsDir;$env:Path"
    
    # Add to user PATH permanently
    $currentUserPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($currentUserPath -notlike "*$projectToolsDir*") {
        [Environment]::SetEnvironmentVariable("Path", "$projectToolsDir;$currentUserPath", "User")
        Write-Host "Added to user PATH" -ForegroundColor Green
    }
    
    # Verify
    Write-Host "`nTesting ADB..." -ForegroundColor Cyan
    & "$projectToolsDir\adb.exe" version
    
    Write-Host "`n=== ADB Ready! ===" -ForegroundColor Green
    Write-Host "Location: $projectToolsDir"
    Write-Host "`nRestart PowerShell or run: `$env:Path = `"$projectToolsDir;`$env:Path`""
    
} else {
    Write-Host "ADB not found at $adbSource" -ForegroundColor Red
    Write-Host "Please run the Android SDK setup first."
}
