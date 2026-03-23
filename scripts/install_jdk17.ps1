#!/usr/bin/env pwsh
# Install JDK 17 manually without admin rights

$jdkDir = "$env:LOCALAPPDATA\jdk"
$jdkVersion = "17.0.14"
$jdkBuild = "7"
$zipFile = "$env:TEMP\openjdk-$jdkVersion-windows-x64.zip"
$jdkHome = "$jdkDir\jdk-17"

Write-Host "Creating directory: $jdkDir" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $jdkDir | Out-Null

Write-Host "Downloading JDK $jdkVersion..." -ForegroundColor Cyan
# Using Eclipse Temurin download - direct release URL
$downloadUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.14_7.zip"
Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFile

Write-Host "Extracting to $jdkDir..." -ForegroundColor Cyan
Expand-Archive -Path $zipFile -DestinationPath $jdkDir -Force

Write-Host "Cleaning up..." -ForegroundColor Cyan
Remove-Item $zipFile -Force

# Set JAVA_HOME
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkHome, "User")

# Add to PATH for current session
$env:Path = "$jdkHome\bin;" + $env:Path

Write-Host "`n✅ JDK $jdkVersion installed successfully to $jdkHome" -ForegroundColor Green
Write-Host "Note: Run 'refreshenv' or restart terminal for permanent PATH update" -ForegroundColor Yellow
