# =============================================================================
# EgyptianAgent - Windows Development Environment Setup Script
# =============================================================================
#
# PURPOSE:
#   Automated setup script for Windows development environment to build
#   and deploy EgyptianAgent Hybrid Architecture on Android devices.
#
# USAGE:
#   Open PowerShell as Administrator and run:
#   .\scripts\setup\windows_setup.ps1
#
# OPTIONS:
#   -Auto           Run in automatic mode (no prompts)
#   -Verbose        Enable verbose output
#   -SkipChecks     Skip prerequisite checks
#   -Help           Show this help message
#
# EXAMPLES:
#   .\scripts\setup\windows_setup.ps1
#   .\scripts\setup\windows_setup.ps1 -Auto -Verbose
#
# REQUIREMENTS:
#   - Windows 10/11
#   - PowerShell 5.1+ or PowerShell 7+
#   - Administrator privileges (for PATH modifications)
#   - Internet connection
#
# AUTHOR: EgyptianAgent Team
# VERSION: 1.0.0
# DATE: 2026-03-14
# =============================================================================

[CmdletBinding()]
param(
    [switch]$Auto,
    [switch]$Verbose,
    [switch]$SkipChecks,
    [switch]$Help
)

# =============================================================================
# Configuration
# =============================================================================

$ScriptName = "windows_setup.ps1"
$ScriptVersion = "1.0.0"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

# Installation paths
$AndroidSDKPath = "$env:LOCALAPPDATA\Android\Sdk"
$JavaInstallPath = "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot"
$GradleUserHome = "$env:USERPROFILE\.gradle"

# Download URLs
$Urls = @{
    AndroidPlatformTools = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
    AndroidCommandLineTools = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    OpenJDK17 = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi"
    Gradle = "https://services.gradle.org/distributions/gradle-8.13-bin.zip"
    GitForWindows = "https://github.com/git-for-windows/git/releases/download/v2.44.0.windows.1/Git-2.44.0-64-bit.exe"
}

# Required versions
$RequiredVersions = @{
    Java = "17"
    MinAndroidApi = "28"
    TargetAndroidApi = "34"
    Gradle = "8.13"
    Adb = "1.0.41"
}

# Colors for output
$Colors = @{
    Red = [ConsoleColor]::Red
    Green = [ConsoleColor]::Green
    Yellow = [ConsoleColor]::Yellow
    Cyan = [ConsoleColor]::Cyan
    White = [ConsoleColor]::White
}

# =============================================================================
# Helper Functions
# =============================================================================

function Write-ColorOutput {
    param(
        [string]$Message,
        [ConsoleColor]$Color = $Colors.White,
        [string]$Prefix = ""
    )
    $originalColor = $Host.UI.RawUI.ForegroundColor
    $Host.UI.RawUI.ForegroundColor = $Color
    if ($Prefix) {
        Write-Host "$Prefix $Message"
    } else {
        Write-Host $Message
    }
    $Host.UI.RawUI.ForegroundColor = $originalColor
}

function Write-Success {
    param([string]$Message)
    Write-ColorOutput $Message -Color $Colors.Green -Prefix "✓"
}

function Write-Error {
    param([string]$Message)
    Write-ColorOutput $Message -Color $Colors.Red -Prefix "✗"
}

function Write-Warning {
    param([string]$Message)
    Write-ColorOutput $Message -Color $Colors.Yellow -Prefix "⚠"
}

function Write-Info {
    param([string]$Message)
    Write-ColorOutput $Message -Color $Colors.Cyan -Prefix "ℹ"
}

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "═" * 70 -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "═" * 70 -ForegroundColor Cyan
    Write-Host ""
}

function Write-SubHeader {
    param([string]$Message)
    Write-Host ""
    Write-Host "─" * 50 -ForegroundColor Gray
    Write-Host "  $Message" -ForegroundColor Gray
    Write-Host "─" * 50 -ForegroundColor Gray
    Write-Host ""
}

function Test-Administrator {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Test-InternetConnection {
    try {
        $connection = Test-Connection -ComputerName www.google.com -Count 1 -Quiet
        return $connection
    } catch {
        return $false
    }
}

function Get-FileVersion {
    param([string]$FilePath)
    if (Test-Path $FilePath) {
        return (Get-Item $FilePath).VersionInfo.FileVersion
    }
    return $null
}

function Add-ToPath {
    param([string]$PathToAdd)
    
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    
    if ($currentPath -notlike "*$PathToAdd*") {
        $newPath = "$currentPath;$PathToAdd"
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
        Write-Success "Added to PATH: $PathToAdd"
        return $true
    } else {
        Write-Info "Already in PATH: $PathToAdd"
        return $false
    }
}

function Download-File {
    param(
        [string]$Url,
        [string]$OutputPath
    )
    
    Write-Info "Downloading: $Url"
    
    try {
        Invoke-WebRequest -Uri $Url -OutFile $OutputPath -UseBasicParsing
        Write-Success "Downloaded: $OutputPath"
        return $true
    } catch {
        Write-Error "Failed to download: $_"
        return $false
    }
}

function Expand-Zip {
    param(
        [string]$ZipPath,
        [string]$DestinationPath
    )
    
    Write-Info "Extracting: $ZipPath"
    
    try {
        if (!(Test-Path $DestinationPath)) {
            New-Item -ItemType Directory -Path $DestinationPath -Force | Out-Null
        }
        Expand-Archive -Path $ZipPath -DestinationPath $DestinationPath -Force
        Write-Success "Extracted to: $DestinationPath"
        return $true
    } catch {
        Write-Error "Failed to extract: $_"
        return $false
    }
}

# =============================================================================
# Check Functions
# =============================================================================

function Check-Java {
    Write-SubHeader "Checking Java Installation"
    
    $javaExe = Get-Command java -ErrorAction SilentlyContinue
    
    if ($javaExe) {
        $javaVersion = java -version 2>&1
        Write-Host $javaVersion[0]
        
        if ($javaVersion -match "version \"?(\d+)") {
            $version = $matches[1]
            if ($version -eq $RequiredVersions.Java) {
                Write-Success "Java $($RequiredVersions.Java) is installed"
                return $true
            } else {
                Write-Warning "Java version $version found, but $($RequiredVersions.Java) is required"
            }
        }
    } else {
        Write-Error "Java is not installed or not in PATH"
    }
    
    return $false
}

function Check-AndroidSDK {
    Write-SubHeader "Checking Android SDK Installation"
    
    if (Test-Path $AndroidSDKPath) {
        Write-Success "Android SDK found at: $AndroidSDKPath"
        
        $platformToolsPath = "$AndroidSDKPath\platform-tools"
        if (Test-Path $platformToolsPath) {
            Write-Success "Platform-tools found"
            return $true
        } else {
            Write-Warning "Platform-tools not found in Android SDK"
        }
    } else {
        Write-Error "Android SDK not found at: $AndroidSDKPath"
    }
    
    return $false
}

function Check-ADB {
    Write-SubHeader "Checking ADB Installation"
    
    $adbExe = Get-Command adb -ErrorAction SilentlyContinue
    
    if ($adbExe) {
        $adbVersion = adb version 2>&1 | Select-Object -First 1
        Write-Host $adbVersion
        Write-Success "ADB is installed and accessible"
        return $true
    } else {
        Write-Error "ADB is not installed or not in PATH"
    }
    
    return $false
}

function Check-Gradle {
    Write-SubHeader "Checking Gradle Installation"
    
    # Check for Gradle wrapper first
    $gradleWrapper = Join-Path $ProjectRoot "gradle\wrapper\gradle-wrapper.jar"
    if (Test-Path $gradleWrapper) {
        Write-Success "Gradle wrapper found"
        return $true
    }
    
    # Check for system Gradle
    $gradleExe = Get-Command gradle -ErrorAction SilentlyContinue
    if ($gradleExe) {
        $gradleVersion = gradle --version 2>&1 | Select-Object -First 1
        Write-Host $gradleVersion
        Write-Success "Gradle is installed"
        return $true
    }
    
    Write-Warning "Gradle not found (wrapper or system installation)"
    return $false
}

function Check-Git {
    Write-SubHeader "Checking Git Installation"
    
    $gitExe = Get-Command git -ErrorAction SilentlyContinue
    
    if ($gitExe) {
        $gitVersion = git --version 2>&1
        Write-Host $gitVersion
        Write-Success "Git is installed"
        return $true
    } else {
        Write-Error "Git is not installed or not in PATH"
    }
    
    return $false
}

function Check-DeviceConnection {
    Write-SubHeader "Checking Android Device Connection"
    
    $adbExe = Get-Command adb -ErrorAction SilentlyContinue
    
    if (!$adbExe) {
        Write-Warning "Cannot check device: ADB not installed"
        return $false
    }
    
    $devices = adb devices 2>&1 | Select-String "device$"
    
    if ($devices) {
        Write-Success "Android device connected"
        foreach ($device in $devices) {
            $deviceInfo = adb -s $($device.Line.Split()[0]) shell "getprop ro.product.model" 2>&1
            $androidVersion = adb -s $($device.Line.Split()[0]) shell "getprop ro.build.version.release" 2>&1
            Write-Host "  Device: $deviceInfo (Android $androidVersion)"
        }
        return $true
    } else {
        Write-Warning "No Android devices connected"
        Write-Host ""
        Write-Host "To connect a device:"
        Write-Host "  1. Enable USB debugging on your Android device"
        Write-Host "  2. Connect via USB cable"
        Write-Host "  3. Accept the USB debugging prompt on your device"
        return $false
    }
}

function Check-Chocolatey {
    Write-SubHeader "Checking Chocolatey Package Manager"
    
    $chocoExe = Get-Command choco -ErrorAction SilentlyContinue
    
    if ($chocoExe) {
        $chocoVersion = choco --version 2>&1
        Write-Host "Chocolatey version: $chocoVersion"
        Write-Success "Chocolatey is installed"
        return $true
    } else {
        Write-Warning "Chocolatey is not installed"
        return $false
    }
}

# =============================================================================
# Installation Functions
# =============================================================================

function Install-Chocolatey {
    Write-Header "Installing Chocolatey Package Manager"
    
    Write-Info "Downloading and installing Chocolatey..."
    
    try {
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
        Write-Success "Chocolatey installed successfully"
        return $true
    } catch {
        Write-Error "Failed to install Chocolatey: $_"
        return $false
    }
}

function Install-JavaViaChocolatey {
    Write-Header "Installing Java JDK 17 via Chocolatey"
    
    Write-Info "Installing OpenJDK 17..."
    
    try {
        choco install openjdk17 -y --force
        Write-Success "Java JDK 17 installed successfully"
        
        # Refresh environment variables
        Refresh-Environment
        return $true
    } catch {
        Write-Error "Failed to install Java: $_"
        return $false
    }
}

function Install-AndroidSDKViaChocolatey {
    Write-Header "Installing Android SDK Platform Tools via Chocolatey"
    
    Write-Info "Installing Android SDK Platform Tools..."
    
    try {
        choco install android-sdk-platform-tools -y --force
        Write-Success "Android SDK Platform Tools installed successfully"
        
        # Refresh environment variables
        Refresh-Environment
        return $true
    } catch {
        Write-Error "Failed to install Android SDK: $_"
        return $false
    }
}

function Install-GradleViaChocolatey {
    Write-Header "Installing Gradle via Chocolatey"
    
    Write-Info "Installing Gradle..."
    
    try {
        choco install gradle -y --force
        Write-Success "Gradle installed successfully"
        
        # Refresh environment variables
        Refresh-Environment
        return $true
    } catch {
        Write-Error "Failed to install Gradle: $_"
        return $false
    }
}

function Install-JavaManual {
    Write-Header "Installing Java JDK 17 (Manual Download)"
    
    Write-Info "Please download and install Java JDK 17 from:"
    Write-Host ""
    Write-Host "  https://adoptium.net/temurin/releases/?version=17"
    Write-Host ""
    Write-Info "Or use this direct download link:"
    Write-Host "  $($Urls.OpenJDK17)"
    Write-Host ""
    Write-Info "After installation, restart this script."
    
    if ($Auto) {
        Write-Warning "Auto mode: Skipping manual installation"
        return $false
    }
    
    $openBrowser = Read-Host "Open download page in browser? (Y/N)"
    if ($openBrowser -eq "Y" -or $openBrowser -eq "y") {
        Start-Process "https://adoptium.net/temurin/releases/?version=17"
    }
    
    return $false
}

function Install-ADBManual {
    Write-Header "Installing ADB (Manual Download)"
    
    $platformToolsPath = "$AndroidSDKPath\platform-tools"
    
    Write-Info "Downloading Android Platform Tools..."
    
    if (!(Test-Path $AndroidSDKPath)) {
        New-Item -ItemType Directory -Path $AndroidSDKPath -Force | Out-Null
    }
    
    $zipPath = "$env:TEMP\platform-tools.zip"
    
    if (Download-File -Url $Urls.AndroidPlatformTools -OutputPath $zipPath) {
        if (Expand-Zip -ZipPath $zipPath -DestinationPath $AndroidSDKPath) {
            # Move platform-tools to correct location
            $extractedPath = "$AndroidSDKPath\platform-tools"
            if (Test-Path $extractedPath) {
                Add-ToPath $extractedPath
                Write-Success "ADB installed to: $extractedPath"
                
                # Clean up
                Remove-Item $zipPath -Force
                
                return $true
            }
        }
    }
    
    Write-Info "Alternative: Download from https://developer.android.com/studio/releases/platform-tools"
    return $false
}

function Initialize-GradleWrapper {
    Write-Header "Initializing Gradle Wrapper"
    
    Write-Info "Checking if Gradle wrapper needs initialization..."
    
    $gradleWrapperJar = Join-Path $ProjectRoot "gradle\wrapper\gradle-wrapper.jar"
    
    if (Test-Path $gradleWrapperJar) {
        Write-Success "Gradle wrapper already initialized"
        return $true
    }
    
    Write-Info "Gradle wrapper JAR not found. Initializing..."
    
    # Check if we have a system gradle
    $gradleExe = Get-Command gradle -ErrorAction SilentlyContinue
    
    if ($gradleExe) {
        Write-Info "Using system Gradle to initialize wrapper..."
        
        try {
            Push-Location $ProjectRoot
            gradle wrapper --gradle-version $($RequiredVersions.Gradle)
            Pop-Location
            Write-Success "Gradle wrapper initialized successfully"
            return $true
        } catch {
            Write-Error "Failed to initialize wrapper: $_"
        }
    } else {
        Write-Warning "System Gradle not found. Manual initialization required."
        Write-Host ""
        Write-Host "To initialize the Gradle wrapper manually:"
        Write-Host "  1. Install Gradle: choco install gradle -y"
        Write-Host "  2. Run: gradle wrapper --gradle-version $($RequiredVersions.Gradle)"
        Write-Host ""
        Write-Host "Or download the wrapper JAR from:"
        Write-Host "  https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar"
        Write-Host ""
    }
    
    return $false
}

function Refresh-Environment {
    Write-Info "Refreshing environment variables..."
    
    # Refresh PATH from registry
    $registryPath = "Registry::HKEY_CURRENT_USER\Environment"
    $userPath = (Get-ItemProperty -Path $registryPath -Name Path).Path
    
    [Environment]::SetEnvironmentVariable("Path", $userPath, "User")
    
    # Refresh current session PATH
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + 
                [System.Environment]::GetEnvironmentVariable("Path", "User")
    
    Write-Success "Environment variables refreshed"
}

function Enable-USBDebugging {
    Write-Header "USB Debugging Setup Guide"
    
    Write-Host "To enable USB debugging on your Android device:"
    Write-Host ""
    Write-Host "Step 1: Enable Developer Options"
    Write-Host "  1. Open Settings on your device"
    Write-Host "  2. Go to 'About phone'"
    Write-Host "  3. Tap 'Build number' 7 times"
    Write-Host "  4. You'll see 'You are now a developer!'"
    Write-Host ""
    Write-Host "Step 2: Enable USB Debugging"
    Write-Host "  1. Go to Settings → System → Developer options"
    Write-Host "  2. Find and enable 'USB debugging'"
    Write-Host "  3. Accept the warning prompt"
    Write-Host ""
    Write-Host "Step 3: Connect and Authorize"
    Write-Host "  1. Connect your device via USB cable"
    Write-Host "  2. On your device, accept the 'Allow USB debugging' prompt"
    Write-Host "  3. Check 'Always allow from this computer' for convenience"
    Write-Host ""
    Write-Host "For Honor X6c (Android 12):"
    Write-Host "  - You may also need to enable 'Disable Permission Monitoring'"
    Write-Host "  - For system app installation, root access (Magisk) is required"
    Write-Host ""
    
    if ($Auto) {
        return
    }
    
    $checkDevice = Read-Host "Check device connection now? (Y/N)"
    if ($checkDevice -eq "Y" -or $checkDevice -eq "y") {
        Check-DeviceConnection
    }
}

# =============================================================================
# Main Setup Function
# =============================================================================

function Run-Setup {
    Write-Header "EgyptianAgent Windows Development Environment Setup"
    
    Write-Host "Version: $ScriptVersion"
    Write-Host "Project: $ProjectRoot"
    Write-Host ""
    
    # Check for administrator privileges
    if (!(Test-Administrator)) {
        Write-Warning "This script requires administrator privileges for PATH modifications."
        Write-Host ""
        
        if ($Auto) {
            Write-Error "Cannot continue in auto mode without administrator privileges."
            return $false
        }
        
        $runAsAdmin = Read-Host "Restart script as Administrator? (Y/N)"
        if ($runAsAdmin -eq "Y" -or $runAsAdmin -eq "y") {
            Start-Process powershell -Verb RunAs -ArgumentList "-ExecutionPolicy Bypass -File `"$PSCommandPath`" -Auto"
            return $true
        }
        
        Write-Warning "Continuing without administrator privileges. Some features may not work."
    }
    
    # Check internet connection
    if (!(Test-InternetConnection)) {
        Write-Error "No internet connection detected. Please connect to the internet and try again."
        return $false
    }
    
    Write-Success "Internet connection verified"
    
    # Run checks
    if (!$SkipChecks) {
        Write-Header "Running Prerequisite Checks"
        
        $checks = @{
            Java = Check-Java
            AndroidSDK = Check-AndroidSDK
            ADB = Check-ADB
            Gradle = Check-Gradle
            Git = Check-Git
            Chocolatey = Check-Chocolatey
        }
        
        Write-SubHeader "Check Summary"
        
        foreach ($check in $checks.GetEnumerator()) {
            $status = if ($check.Value) { "✓ Pass" } else { "✗ Fail" }
            $color = if ($check.Value) { $Colors.Green } else { $Colors.Red }
            Write-Host "  $("{0,-15}" -f $check.Key) : $status" -ForegroundColor $color
        }
        
        # Check if all passed
        $allPassed = ($checks.Values | Where-Object { $_ -eq $false }).Count -eq 0
        
        if ($allPassed) {
            Write-Host ""
            Write-Success "All prerequisites are satisfied!"
            Write-Host ""
            
            if (!$Auto) {
                $continue = Read-Host "Continue with additional setup? (Y/N)"
                if ($continue -ne "Y" -and $continue -ne "y") {
                    return $true
                }
            }
        } else {
            Write-Host ""
            Write-Warning "Some prerequisites are missing. Setup will install them."
        }
    }
    
    # Installation menu
    Write-Header "Installation Options"
    
    if (!$Auto) {
        Write-Host "Select what to install:"
        Write-Host "  1. Install all missing prerequisites (Recommended)"
        Write-Host "  2. Install Java JDK 17"
        Write-Host "  3. Install Android SDK Platform Tools (ADB)"
        Write-Host "  4. Install Gradle"
        Write-Host "  5. Initialize Gradle Wrapper"
        Write-Host "  6. USB Debugging Setup Guide"
        Write-Host "  7. Exit"
        Write-Host ""
        
        $choice = Read-Host "Enter choice (1-7)"
        
        switch ($choice) {
            "1" {
                Install-All
            }
            "2" {
                if (Check-Chocolatey) {
                    Install-JavaViaChocolatey
                } else {
                    Install-JavaManual
                }
            }
            "3" {
                if (Check-Chocolatey) {
                    Install-AndroidSDKViaChocolatey
                } else {
                    Install-ADBManual
                }
            }
            "4" {
                if (Check-Chocolatey) {
                    Install-GradleViaChocolatey
                } else {
                    Write-Warning "Chocolatey required for automated Gradle installation"
                    Write-Info "Download from: https://gradle.org/install/"
                }
            }
            "5" {
                Initialize-GradleWrapper
            }
            "6" {
                Enable-USBDebugging
            }
            "7" {
                Write-Info "Setup cancelled."
                return $false
            }
            default {
                Write-Warning "Invalid choice. Please run the script again."
                return $false
            }
        }
    } else {
        # Auto mode - install everything
        Install-All
    }
    
    # Final verification
    Write-Header "Final Verification"
    
    Check-Java | Out-Null
    Check-ADB | Out-Null
    Check-Gradle | Out-Null
    Check-DeviceConnection | Out-Null
    
    Write-Host ""
    Write-Header "Setup Complete!"
    
    Write-Host ""
    Write-Host "Next steps:"
    Write-Host "  1. If you installed new software, close and reopen PowerShell"
    Write-Host "  2. Connect your Android device with USB debugging enabled"
    Write-Host "  3. Run: .\gradlew.bat assembleDebug"
    Write-Host "  4. Run: .\scripts\deploy\verify_deployment.sh"
    Write-Host ""
    Write-Host "Documentation:"
    Write-Host "  - Quick Start: docs\deployment\DEPLOYMENT_PREREQUISITES.md"
    Write-Host "  - Deployment Guide: docs\deployment\DEPLOYMENT_GUIDE.md"
    Write-Host ""
    
    return $true
}

function Install-All {
    Write-Header "Installing All Prerequisites"
    
    # Install Chocolatey if needed
    if (!(Check-Chocolatey)) {
        if (!(Install-Chocolatey)) {
            Write-Warning "Chocolatey installation failed. Using manual installation methods."
        }
    }
    
    # Install Java if needed
    if (!(Check-Java)) {
        if (Check-Chocolatey) {
            Install-JavaViaChocolatey
        } else {
            Install-JavaManual
        }
    }
    
    # Install Android SDK if needed
    if (!(Check-AndroidSDK) -or !(Check-ADB)) {
        if (Check-Chocolatey) {
            Install-AndroidSDKViaChocolatey
        } else {
            Install-ADBManual
        }
    }
    
    # Install Gradle if needed
    if (!(Check-Gradle)) {
        if (Check-Chocolatey) {
            Install-GradleViaChocolatey
        } else {
            Write-Warning "Chocolatey required for automated Gradle installation"
            Write-Info "Manual download: https://gradle.org/install/"
        }
    }
    
    # Initialize Gradle wrapper
    Initialize-GradleWrapper
    
    Write-Success "All installations completed!"
}

# =============================================================================
# Help Function
# =============================================================================

function Show-Help {
    Write-Header "EgyptianAgent Windows Setup - Help"
    
    Write-Host "USAGE:"
    Write-Host "  .\scripts\setup\windows_setup.ps1 [OPTIONS]"
    Write-Host ""
    Write-Host "OPTIONS:"
    Write-Host "  -Auto       Run in automatic mode (no prompts, install everything)"
    Write-Host "  -Verbose    Enable verbose output"
    Write-Host "  -SkipChecks Skip prerequisite checks"
    Write-Host "  -Help       Show this help message"
    Write-Host ""
    Write-Host "EXAMPLES:"
    Write-Host "  # Interactive mode"
    Write-Host "  .\scripts\setup\windows_setup.ps1"
    Write-Host ""
    Write-Host "  # Automatic installation (requires Admin)"
    Write-Host "  .\scripts\setup\windows_setup.ps1 -Auto"
    Write-Host ""
    Write-Host "QUICK SETUP COMMANDS:"
    Write-Host ""
    Write-Host "  # Using Chocolatey (recommended)"
    Write-Host "  choco install openjdk17 android-sdk-platform-tools gradle -y"
    Write-Host ""
    Write-Host "  # Initialize Gradle wrapper"
    Write-Host "  gradle wrapper --gradle-version 8.13"
    Write-Host ""
    Write-Host "  # Build the project"
    Write-Host "  .\gradlew.bat assembleDebug"
    Write-Host ""
    
    exit 0
}

# =============================================================================
# Main Entry Point
# =============================================================================

if ($Help) {
    Show-Help
}

try {
    Run-Setup
} catch {
    Write-Error "Setup failed: $_"
    Write-Host ""
    Write-Host "Please check the error message above and try again."
    Write-Host "For manual installation instructions, see:"
    Write-Host "  docs\deployment\DEPLOYMENT_PREREQUISITES.md"
    exit 1
}
