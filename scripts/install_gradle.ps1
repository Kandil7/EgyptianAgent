#!/usr/bin/env pwsh
# Install Gradle manually without admin rights

$gradleDir = "$env:LOCALAPPDATA\gradle"
$gradleVersion = "8.5"
$zipFile = "$env:TEMP\gradle-$gradleVersion-bin.zip"
$gradleHome = "$gradleDir\gradle-$gradleVersion"

Write-Host "Creating directory: $gradleDir" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $gradleDir | Out-Null

Write-Host "Downloading Gradle $gradleVersion..." -ForegroundColor Cyan
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zipFile

Write-Host "Extracting to $gradleDir..." -ForegroundColor Cyan
Expand-Archive -Path $zipFile -DestinationPath $gradleDir -Force

Write-Host "Cleaning up..." -ForegroundColor Cyan
Remove-Item $zipFile -Force

# Add to PATH for current session
$env:Path = "$gradleHome\bin;" + $env:Path

# Set GRADLE_HOME
[Environment]::SetEnvironmentVariable("GRADLE_HOME", $gradleHome, "User")

Write-Host "`n✅ Gradle $gradleVersion installed successfully to $gradleHome" -ForegroundColor Green
Write-Host "Note: Run 'refreshenv' or restart terminal for permanent PATH update" -ForegroundColor Yellow
