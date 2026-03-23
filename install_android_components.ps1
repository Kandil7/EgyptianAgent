# Android SDK Components Installation Script
# This script accepts licenses and installs required SDK components

$ErrorActionPreference = "Stop"

$sdkPath = "C:\Android\Sdk"
$sdkManagerPath = "$sdkPath\cmdline-tools\latest\bin\sdkmanager.bat"

Write-Host "============================================"
Write-Host "Android SDK Components Installation"
Write-Host "============================================"

# Set environment variables
$env:ANDROID_HOME = $sdkPath
$env:PATH = "$sdkPath\cmdline-tools\latest\bin;$sdkPath\platform-tools;$env:PATH"

Write-Host "`n[Step 1] Accepting Android SDK licenses..."
Write-Host "This may require pressing 'y' multiple times to accept all licenses"
Write-Host ""

# Accept licenses - use echo to automatically accept
& $sdkManagerPath --licenses <<EOF
y
y
y
y
y
y
y
y
y
y
EOF

Write-Host "`nLicenses accepted"

# Step 2: Install required components
Write-Host "`n[Step 2] Installing SDK components..."
Write-Host "  - Platform 34 (android-34)"
Write-Host "  - Build-Tools 34.0.0"
Write-Host "  - Platform-Tools (ADB)"
Write-Host ""

& $sdkManagerPath "platforms;android-34" "build-tools;34.0.0" "platform-tools"

Write-Host "`n============================================"
Write-Host "SDK Components Installation Completed!"
Write-Host "============================================"

# Verify installation
Write-Host "`n[Verification] Listing installed packages..."
& $sdkManagerPath --list_installed

Write-Host "`n============================================"
Write-Host "Installation Summary"
Write-Host "============================================"
Write-Host "SDK Location: $sdkPath"
Write-Host ""
Write-Host "Installed components:"
Write-Host "  - Android SDK Platform 34"
Write-Host "  - Android SDK Build-Tools 34.0.0"
Write-Host "  - Android SDK Platform-Tools"
Write-Host ""
Write-Host "Next step: Create local.properties file"
