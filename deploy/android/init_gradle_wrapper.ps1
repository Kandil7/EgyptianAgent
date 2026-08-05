# =============================================================================
# EgyptianAgent - Initialize Gradle Wrapper Script
# =============================================================================
#
# PURPOSE:
#   Downloads and initializes the Gradle wrapper for the EgyptianAgent project.
#   This script fetches the gradle-wrapper.jar from the official Gradle repository.
#
# USAGE:
#   Open PowerShell and run:
#   .\scripts\utils\init_gradle_wrapper.ps1
#
# AUTHOR: EgyptianAgent Team
# VERSION: 1.0.0
# DATE: 2026-03-14
# =============================================================================

[CmdletBinding()]
param(
    [switch]$Help
)

$ScriptName = "init_gradle_wrapper.ps1"
$GradleVersion = "8.13"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$WrapperDir = Join-Path $ProjectRoot "gradle\wrapper"
$WrapperJarPath = Join-Path $WrapperDir "gradle-wrapper.jar"
$WrapperPropertiesPath = Join-Path $WrapperDir "gradle-wrapper.properties"

# Gradle wrapper JAR download URL
$GradleWrapperUrl = "https://raw.githubusercontent.com/gradle/gradle/v$GradleVersion/gradle/wrapper/gradle-wrapper.jar"
$GradleWrapperBackupUrl = "https://github.com/gradle/gradle/raw/v$GradleVersion/gradle/wrapper/gradle-wrapper.jar"

function Write-ColorOutput {
    param(
        [string]$Message,
        [ConsoleColor]$Color = [ConsoleColor]::White,
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
    Write-ColorOutput $Message -Color Green -Prefix "✓"
}

function Write-Error {
    param([string]$Message)
    Write-ColorOutput $Message -Color Red -Prefix "✗"
}

function Write-Warn {
    param([string]$Message)
    Write-ColorOutput $Message -Color Yellow -Prefix "⚠"
}

function Write-Info {
    param([string]$Message)
    Write-ColorOutput $Message -Color Cyan -Prefix "ℹ"
}

function Show-Help {
    Write-Host ""
    Write-Host "EgyptianAgent - Initialize Gradle Wrapper"
    Write-Host "=" * 50
    Write-Host ""
    Write-Host "USAGE:"
    Write-Host "  .\scripts\utils\init_gradle_wrapper.ps1"
    Write-Host ""
    Write-Host "This script downloads and initializes the Gradle wrapper for Gradle $GradleVersion."
    Write-Host ""
    Write-Host "ALTERNATIVE METHODS:"
    Write-Host ""
    Write-Host "1. Using system Gradle (if installed):"
    Write-Host "   gradle wrapper --gradle-version $GradleVersion"
    Write-Host ""
    Write-Host "2. Manual download:"
    Write-Host "   Download from: $GradleWrapperUrl"
    Write-Host "   Save to: gradle\wrapper\gradle-wrapper.jar"
    Write-Host ""
    
    exit 0
}

function Initialize-GradleWrapper {
    Write-Host ""
    Write-Host "═" * 60 -ForegroundColor Cyan
    Write-Host "  EgyptianAgent - Gradle Wrapper Initialization" -ForegroundColor Cyan
    Write-Host "═" * 60 -ForegroundColor Cyan
    Write-Host ""
    
    Write-Info "Project root: $ProjectRoot"
    Write-Info "Gradle version: $GradleVersion"
    Write-Info "Wrapper directory: $WrapperDir"
    Write-Host ""
    
    # Check if wrapper already exists
    if (Test-Path $WrapperJarPath) {
        Write-Success "Gradle wrapper JAR already exists"
        Write-Host ""
        
        $overwrite = Read-Host "Overwrite existing wrapper? (Y/N)"
        if ($overwrite -ne "Y" -and $overwrite -ne "y") {
            Write-Info "Keeping existing wrapper. Exiting."
            return $true
        }
    }
    
    # Ensure wrapper directory exists
    if (!(Test-Path $WrapperDir)) {
        Write-Info "Creating wrapper directory: $WrapperDir"
        New-Item -ItemType Directory -Path $WrapperDir -Force | Out-Null
    }
    
    # Try to initialize using system Gradle first
    $gradleExe = Get-Command gradle -ErrorAction SilentlyContinue
    
    if ($gradleExe) {
        Write-Info "System Gradle found. Using it to initialize wrapper..."
        
        try {
            Push-Location $ProjectRoot
            gradle wrapper --gradle-version $GradleVersion
            Pop-Location
            
            if (Test-Path $WrapperJarPath) {
                Write-Success "Gradle wrapper initialized successfully using system Gradle"
                return $true
            }
        } catch {
            Write-Warn "System Gradle failed to initialize wrapper: $_"
        }
    } else {
        Write-Info "System Gradle not found. Will download wrapper JAR directly..."
    }
    
    # Download wrapper JAR
    Write-Host ""
    Write-Info "Downloading gradle-wrapper.jar..."
    Write-Info "Source: $GradleWrapperUrl"
    Write-Host ""
    
    try {
        # Try primary URL
        Invoke-WebRequest -Uri $GradleWrapperUrl -OutFile $WrapperJarPath -UseBasicParsing
        
        if (Test-Path $WrapperJarPath) {
            $fileSize = (Get-Item $WrapperJarPath).Length
            if ($fileSize -gt 10000) {  # Basic validation
                Write-Success "Downloaded gradle-wrapper.jar ($([math]::Round($fileSize/1KB, 2)) KB)"
                
                # Update properties file
                Update-WrapperProperties
                
                Write-Host ""
                Write-Host "═" * 60 -ForegroundColor Green
                Write-Host "  Gradle Wrapper Initialization Complete!" -ForegroundColor Green
                Write-Host "═" * 60 -ForegroundColor Green
                Write-Host ""
                Write-Host "You can now use the Gradle wrapper:"
                Write-Host "  .\gradlew.bat --version"
                Write-Host "  .\gradlew.bat assembleDebug"
                Write-Host ""
                return $true
            }
        }
        
        throw "Downloaded file appears to be invalid"
    } catch {
        Write-Warn "Primary download failed: $_"
        Write-Host ""
        
        # Try backup URL
        Write-Info "Trying backup URL..."
        
        try {
            Invoke-WebRequest -Uri $GradleWrapperBackupUrl -OutFile $WrapperJarPath -UseBasicParsing
            
            if (Test-Path $WrapperJarPath) {
                $fileSize = (Get-Item $WrapperJarPath).Length
                if ($fileSize -gt 10000) {
                    Write-Success "Downloaded gradle-wrapper.jar from backup source"
                    Update-WrapperProperties
                    return $true
                }
            }
        } catch {
            Write-Error "Backup download also failed: $_"
        }
    }
    
    # If we get here, downloads failed
    Write-Host ""
    Write-Warn "Automatic download failed. Please use one of these alternatives:"
    Write-Host ""
    Write-Host "Option 1: Install Gradle and run:"
    Write-Host "  choco install gradle -y"
    Write-Host "  gradle wrapper --gradle-version $GradleVersion"
    Write-Host ""
    Write-Host "Option 2: Manual download:"
    Write-Host "  1. Download from: $GradleWrapperUrl"
    Write-Host "  2. Save to: $WrapperJarPath"
    Write-Host ""
    Write-Host "Option 3: Copy from another Gradle project"
    Write-Host "  Copy gradle\wrapper\gradle-wrapper.jar from any Gradle 8.x project"
    Write-Host ""
    
    return $false
}

function Update-WrapperProperties {
    Write-Info "Updating gradle-wrapper.properties..."
    
    $propertiesContent = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
    
    $propertiesContent | Out-File -FilePath $WrapperPropertiesPath -Encoding ASCII -NoNewline
    Write-Success "Updated gradle-wrapper.properties"
}

# =============================================================================
# Main Entry Point
# =============================================================================

if ($Help) {
    Show-Help
}

try {
    $result = Initialize-GradleWrapper
    if ($result) {
        exit 0
    } else {
        exit 1
    }
} catch {
    Write-Error "Initialization failed: $_"
    exit 1
}
