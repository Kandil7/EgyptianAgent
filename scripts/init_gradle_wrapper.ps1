#!/usr/bin/env pwsh
# Initialize Gradle Wrapper

$gradleWrapperDir = "K:\business\projects_v2\EgyptianAgent\gradle\wrapper"

Write-Host "Creating gradle wrapper directory..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $gradleWrapperDir | Out-Null

Write-Host "Downloading gradle-wrapper.jar..." -ForegroundColor Cyan
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar" -OutFile "$gradleWrapperDir\gradle-wrapper.jar"

Write-Host "`n✅ Gradle wrapper initialized successfully" -ForegroundColor Green
