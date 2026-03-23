# Android SDK Installation Script for EgyptianAgent
# This script installs the Android SDK using command-line tools

$ErrorActionPreference = "Stop"

Write-Host "============================================"
Write-Host "Android SDK Installation Script"
Write-Host "============================================"

# Step 1: Create SDK directory
$sdkPath = "C:\Android\Sdk"
Write-Host "`n[Step 1] Creating SDK directory: $sdkPath"
New-Item -ItemType Directory -Force -Path $sdkPath | Out-Null
Write-Host "SDK directory created successfully"

# Step 2: Download command-line tools
$downloadUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$downloadPath = "$env:TEMP\android-cmdline.zip"
Write-Host "`n[Step 2] Downloading command-line tools from: $downloadUrl"
Write-Host "Download location: $downloadPath"

if (Test-Path $downloadPath) {
    Write-Host "Download already exists, skipping download"
} else {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $downloadPath -UseBasicParsing
    Write-Host "Download completed successfully"
}

# Step 3: Extract tools
$extractPath = "$env:TEMP\android-cmdline-extract"
Write-Host "`n[Step 3] Extracting tools to: $extractPath"
if (Test-Path $extractPath) {
    Remove-Item -Path $extractPath -Recurse -Force
}
Expand-Archive -Path $downloadPath -DestinationPath $extractPath -Force
Write-Host "Extraction completed successfully"

# Step 4: Install to SDK location
$cmdlinePath = "$sdkPath\cmdline-tools\latest"
Write-Host "`n[Step 4] Installing cmdline-tools to: $cmdlinePath"
if (Test-Path $cmdlinePath) {
    Remove-Item -Path $cmdlinePath -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $cmdlinePath | Out-Null
Move-Item -Path "$extractPath\cmdline-tools\*" -Destination $cmdlinePath -Force
Write-Host "cmdline-tools installed successfully"

# Step 5: Set ANDROID_HOME temporarily for this session
$env:ANDROID_HOME = $sdkPath
$env:PATH = "$sdkPath\cmdline-tools\latest\bin;$sdkPath\platform-tools;$env:PATH"

Write-Host "`n[Step 5] SDK Manager location verified"
$sdkManagerPath = "$sdkPath\cmdline-tools\latest\bin\sdkmanager.bat"
if (Test-Path $sdkManagerPath) {
    Write-Host "sdkmanager.bat found at: $sdkManagerPath"
} else {
    Write-Host "ERROR: sdkmanager.bat not found!"
    exit 1
}

Write-Host "`n============================================"
Write-Host "Base installation completed!"
Write-Host "============================================"
Write-Host ""
Write-Host "SDK Location: $sdkPath"
Write-Host ""
Write-Host "Next steps (run manually):"
Write-Host "  1. Accept licenses: $sdkManagerPath --licenses"
Write-Host "  2. Install components: $sdkManagerPath `\"platforms;android-34`\" `\"build-tools;34.0.0`\" `\"platform-tools`\""
Write-Host ""
Write-Host "Or run: .\install_android_components.ps1"
