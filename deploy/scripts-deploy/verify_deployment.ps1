# =============================================================================
# EgyptianAgent - Hybrid Architecture Deployment Verification Script (PowerShell)
# =============================================================================
#
# PURPOSE:
#   Comprehensive verification script to validate all components of the
#   EgyptianAgent Hybrid Architecture deployment on Android devices.
#   Windows PowerShell version for native Windows support.
#
# USAGE:
#   .\scripts\deploy\verify_deployment.ps1 [OPTIONS]
#
# OPTIONS:
#   -Device SERIAL     Target device serial (for multiple devices)
#   -Output FORMAT     Output format: Markdown, Json, Text (default: Markdown)
#   -OutputFile PATH   Write report to specified file
#   -AutoFix           Attempt automatic fixes for common issues
#   -Verbose           Enable verbose logging
#   -Ci                CI/CD mode (non-interactive, machine-readable)
#   -SkipTests         Skip functional tests
#   -Help              Show this help message
#
# CHECKS PERFORMED:
#   1. Build verification (APK exists, size correct)
#   2. Device connection (ADB working, device authorized)
#   3. App installation (package installed, version correct)
#   4. Permissions granted (all 10 required permissions)
#   5. Accessibility service enabled
#   6. Models deployed (FunctionGemma, Whisper)
#   7. Workflows deployed (10 YAML files)
#   8. Storage space available (>2GB free)
#   9. Battery optimization disabled
#   10. Quick functionality test
#
# OUTPUT:
#   - Markdown report with checkmarks/X marks
#   - Summary table with Pass/Fail
#   - Recommendations for any failures
#   - Exit code 0 if all pass, 1 if any fail
#
# RETURN CODES:
#   0   All checks passed
#   1   One or more checks failed
#   2   Device not connected
#   3   Critical error (script cannot continue)
#
# AUTHOR: EgyptianAgent Team
# VERSION: 1.0.0
# DATE: 2026-03-14
# =============================================================================

[CmdletBinding()]
param(
    [string]$Device,
    [ValidateSet('Markdown', 'Json', 'Text')]
    [string]$Output = 'Markdown',
    [string]$OutputFile,
    [switch]$AutoFix,
    [switch]$Verbose,
    [switch]$Ci,
    [switch]$SkipTests,
    [switch]$Help
)

# =============================================================================
# Configuration
# =============================================================================

$ScriptName = "verify_deployment.ps1"
$ScriptVersion = "1.0.0"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ReportDir = Join-Path $ProjectRoot "build\reports"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# App configuration
$AppPackage = "com.egyptian.agent"
$AppName = "EgyptianAgent"
$ExpectedApkSizeMb = 45
$MinStorageMb = 2048

$RequiredPermissions = @(
    "android.permission.RECORD_AUDIO",
    "android.permission.CALL_PHONE",
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.SEND_SMS",
    "android.permission.READ_SMS",
    "android.permission.BODY_SENSORS",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.CAMERA",
    "android.permission.READ_EXTERNAL_STORAGE"
)

# Model configuration
$FunctionGemmaModel = "functiongemma-270m-it-Q4_K_M.gguf"
$WhisperModel = "whisper-egyptian-v1.bin"
$ModelsPath = "/data/local/llm"

# Workflow configuration
$WorkflowsPath = "/sdcard/Android/data/$AppPackage/files/workflows"
$RequiredWorkflows = @(
    "morning_routine.yaml",
    "bedtime_routine.yaml",
    "check_social.yaml",
    "send_whatsapp_broadcast.yaml",
    "book_uber.yaml",
    "check_email.yaml",
    "youtube_search.yaml",
    "settings_toggle.yaml",
    "emergency_check.yaml",
    "grocery_list.yaml"
)

# State
$CheckResults = @{}
$Failures = @()
$Warnings = @()
$Recommendations = @()

# =============================================================================
# Helper Functions
# =============================================================================

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
    if ($Ci) {
        Write-Host "[PASS] $Message"
    } else {
        Write-ColorOutput $Message -Color Green -Prefix "✓"
    }
}

function Write-Failure {
    param([string]$Message)
    if ($Ci) {
        Write-Host "[FAIL] $Message" 2>&1
    } else {
        Write-ColorOutput $Message -Color Red -Prefix "✗"
    }
}

function Write-Warn {
    param([string]$Message)
    if ($Ci) {
        Write-Host "[WARN] $Message"
    } else {
        Write-ColorOutput $Message -Color Yellow -Prefix "⚠"
    }
}

function Write-Info {
    param([string]$Message)
    if ($Ci) {
        Write-Host "[INFO] $Message"
    } else {
        Write-ColorOutput $Message -Color Cyan -Prefix "ℹ"
    }
}

function Write-Verbose {
    param([string]$Message)
    if ($Verbose) {
        if ($Ci) {
            Write-Host "[DEBUG] $Message"
        } else {
            Write-ColorOutput $Message -Color Gray -Prefix "•"
        }
    }
}

function Write-Header {
    param([string]$Title)
    Write-Host ""
    if ($Ci) {
        Write-Host "=== $Title ==="
    } else {
        Write-Host "━" * 70 -ForegroundColor Blue
        Write-Host "  $Title" -ForegroundColor Blue
        Write-Host "━" * 70 -ForegroundColor Blue
    }
    Write-Host ""
}

function Write-SubHeader {
    param([string]$Title)
    if ($Ci) {
        Write-Host "--- $Title ---"
    } else {
        Write-Host "  $Title" -ForegroundColor Gray
        Write-Host "  $('─' * 50)" -ForegroundColor Gray
    }
}

function Show-Help {
    Write-Header "EgyptianAgent Deployment Verification - Help"
    
    Write-Host "USAGE:"
    Write-Host "  .\scripts\deploy\verify_deployment.ps1 [OPTIONS]"
    Write-Host ""
    Write-Host "OPTIONS:"
    Write-Host "  -Device SERIAL     Target device serial (for multiple devices)"
    Write-Host "  -Output FORMAT     Output format: Markdown, Json, Text (default: Markdown)"
    Write-Host "  -OutputFile PATH   Write report to specified file"
    Write-Host "  -AutoFix           Attempt automatic fixes for common issues"
    Write-Host "  -Verbose           Enable verbose logging"
    Write-Host "  -Ci                CI/CD mode (non-interactive, machine-readable)"
    Write-Host "  -SkipTests         Skip functional tests"
    Write-Host "  -Help              Show this help message"
    Write-Host ""
    Write-Host "EXAMPLES:"
    Write-Host "  # Run all checks"
    Write-Host "  .\scripts\deploy\verify_deployment.ps1"
    Write-Host ""
    Write-Host "  # Run with auto-fix"
    Write-Host "  .\scripts\deploy\verify_deployment.ps1 -AutoFix"
    Write-Host ""
    Write-Host "  # CI/CD mode with JSON output"
    Write-Host "  .\scripts\deploy\verify_deployment.ps1 -Ci -Output Json"
    Write-Host ""
    Write-Host "  # Target specific device"
    Write-Host "  .\scripts\deploy\verify_deployment.ps1 -Device ABC123XYZ"
    Write-Host ""
    
    exit 0
}

# =============================================================================
# ADB Helper Functions
# =============================================================================

function Test-AdbInstalled {
    try {
        $null = Get-Command adb -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

function Invoke-Adb {
    param([string[]]$Arguments)
    
    $cmd = @("adb")
    
    if ($Device) {
        $cmd += @("-s", $Device)
    }
    
    $cmd += $Arguments
    
    try {
        $result = & $cmd 2>&1
        return $result
    } catch {
        Write-Verbose "ADB command failed: $_"
        return $null
    }
}

function Invoke-AdbShell {
    param([string]$Command)
    
    $result = Invoke-Adb -Arguments @("shell", $Command)
    return ($result -join "`n").Trim()
}

function Invoke-AdbShellQuiet {
    param([string]$Command)
    
    $result = Invoke-AdbShell $Command
    return $result -replace "`r", ""
}

# =============================================================================
# Check Functions
# =============================================================================

function Check-Prerequisites {
    Write-SubHeader "Checking Prerequisites"
    
    $allGood = $true
    
    # Check ADB
    if (!(Test-AdbInstalled)) {
        Write-Failure "ADB is not installed or not in PATH"
        Write-Host ""
        Write-Host "To install ADB, run the setup script:"
        Write-Host "  .\scripts\setup\windows_setup.ps1"
        Write-Host ""
        Write-Host "Or see the prerequisites guide:"
        Write-Host "  docs\deployment\DEPLOYMENT_PREREQUISITES.md"
        Write-Host ""
        $Recommendations += "Install Android SDK Platform Tools"
        $allGood = $false
    } else {
        $adbVersion = Invoke-Adb @("version") | Select-Object -First 1
        Write-Info "ADB version: $adbVersion"
        Write-Success "ADB is installed"
    }
    
    # Check device connection
    if ($allGood) {
        $devices = Invoke-Adb @("devices") | Select-String "device$"
        
        if (!$devices) {
            Write-Failure "No Android devices connected"
            Write-Host ""
            Write-Host "To connect a device:"
            Write-Host "  1. Enable USB debugging on your Android device"
            Write-Host "  2. Connect device via USB cable"
            Write-Host "  3. Accept the USB debugging prompt on your device"
            Write-Host ""
            $Recommendations += "Connect an Android device with USB debugging enabled"
            $allGood = $false
        } else {
            $deviceCount = ($devices | Measure-Object).Count
            if ($deviceCount -gt 1 -and !$Device) {
                Write-Warn "Multiple devices connected ($deviceCount). Use -Device to specify target."
            }
            Write-Success "Device connected and authorized"
        }
    }
    
    return $allGood
}

function Check-BuildVerification {
    Write-SubHeader "Check 1: Build Verification"
    
    $releaseApkPath = Join-Path $ProjectRoot "android\build\outputs\apk\release\app-release.apk"
    $debugApkPath = Join-Path $ProjectRoot "android\build\outputs\apk\debug\app-debug.apk"
    
    # Check release APK first
    if (Test-Path $releaseApkPath) {
        $sizeBytes = (Get-Item $releaseApkPath).Length
        $sizeMb = [math]::Round($sizeBytes / 1MB, 2)
        
        Write-Info "Release APK found: $releaseApkPath"
        Write-Info "APK size: ${sizeMb}MB"
        
        if ($sizeMb -ge $ExpectedApkSizeMb) {
            $CheckResults["build"] = "PASS"
            Write-Success "APK size is correct (≥${ExpectedApkSizeMb}MB)"
            return $true
        } else {
            $CheckResults["build"] = "WARN"
            $Warnings += "APK size (${sizeMb}MB) is smaller than expected (${ExpectedApkSizeMb}MB)"
            $Recommendations += "Verify build completed successfully"
            return $true
        }
    }
    
    # Check debug APK
    if (Test-Path $debugApkPath) {
        $sizeBytes = (Get-Item $debugApkPath).Length
        $sizeMb = [math]::Round($sizeBytes / 1MB, 2)
        
        Write-Info "Debug APK found: $debugApkPath"
        Write-Info "APK size: ${sizeMb}MB"
        
        $CheckResults["build"] = "PASS"
        Write-Success "Debug APK available for testing"
        return $true
    }
    
    $CheckResults["build"] = "FAIL"
    $Failures += "APK not found. Run: .\gradlew.bat assembleDebug"
    $Recommendations += "Build the APK: .\gradlew.bat assembleDebug"
    return $false
}

function Check-DeviceConnection {
    Write-SubHeader "Check 2: Device Connection"
    
    $devices = Invoke-Adb @("devices") | Select-String "device$"
    
    if (!$devices) {
        $CheckResults["device"] = "FAIL"
        $Failures += "No Android devices connected"
        $Recommendations += "1. Enable USB debugging on device"
        $Recommendations += "2. Connect device via USB cable"
        $Recommendations += "3. Accept USB debugging prompt on device"
        return $false
    }
    
    # Get device info
    $deviceModel = Invoke-AdbShellQuiet "getprop ro.product.model"
    $deviceAndroid = Invoke-AdbShellQuiet "getprop ro.build.version.release"
    $deviceSdk = Invoke-AdbShellQuiet "getprop ro.build.version.sdk"
    
    Write-Info "Device: $deviceModel"
    Write-Info "Android: $deviceAndroid (SDK $deviceSdk)"
    
    $CheckResults["device"] = "PASS"
    Write-Success "Device connected and authorized"
    return $true
}

function Check-AppInstallation {
    Write-SubHeader "Check 3: App Installation"
    
    $isInstalled = Invoke-AdbShell "pm list packages | Select-String '$AppPackage'"
    
    if (!$isInstalled) {
        $CheckResults["app_install"] = "FAIL"
        $Failures += "$AppName is not installed"
        $Recommendations += "Install app: adb install -r android\build\outputs\apk\debug\app-debug.apk"
        return $false
    }
    
    # Get version info
    $versionInfo = Invoke-AdbShell "dumpsys package $AppPackage | Select-String 'versionName'"
    $versionName = ($versionInfo -split "=")[1]
    
    Write-Info "Version: $versionName"
    
    $CheckResults["app_install"] = "PASS"
    Write-Success "$AppName is installed"
    return $true
}

function Check-Permissions {
    Write-SubHeader "Check 4: Permissions Granted"
    
    $grantedCount = 0
    $deniedCount = 0
    $missingPermissions = @()
    
    foreach ($perm in $RequiredPermissions) {
        $isGranted = Invoke-AdbShell "dumpsys package $AppPackage | Select-String '$perm: granted=true'"
        
        if ($isGranted) {
            $grantedCount++
            Write-Verbose "✓ $perm"
        } else {
            $deniedCount++
            $missingPermissions += $perm
            Write-Verbose "✗ $perm"
        }
    }
    
    Write-Info "Permissions: $grantedCount/$($RequiredPermissions.Count) granted"
    
    if ($deniedCount -gt 0) {
        $CheckResults["permissions"] = "WARN"
        $Warnings += "$deniedCount permissions not granted"
        
        $grantCmds = ""
        foreach ($perm in $missingPermissions) {
            $grantCmds += "`n  adb shell pm grant $AppPackage $perm"
        }
        
        $Recommendations += "Grant missing permissions:$grantCmds"
        return $false
    }
    
    $CheckResults["permissions"] = "PASS"
    Write-Success "All required permissions granted"
    return $true
}

function Check-AccessibilityService {
    Write-SubHeader "Check 5: Accessibility Service"
    
    $enabledServices = Invoke-AdbShellQuiet "settings get secure enabled_accessibility_services"
    $accessibilityComponent = "$AppPackage/.accessibility.EgyptianAccessibilityService"
    
    if ($enabledServices -like "*$accessibilityComponent*") {
        $CheckResults["accessibility"] = "PASS"
        Write-Success "Accessibility service enabled"
        return $true
    }
    
    # Check if service is installed
    $serviceExists = Invoke-AdbShell "dumpsys accessibility | Select-String '$AppPackage'"
    
    if ($serviceExists) {
        $CheckResults["accessibility"] = "WARN"
        $Warnings += "Accessibility service installed but not enabled"
        $Recommendations += "Enable accessibility service:"
        $Recommendations += "  1. Open Settings → Accessibility"
        $Recommendations += "  2. Find '$AppName' and enable it"
        return $false
    }
    
    $CheckResults["accessibility"] = "FAIL"
    $Failures += "Accessibility service not found"
    $Recommendations += "Reinstall app to register accessibility service"
    return $false
}

function Check-ModelsDeployed {
    Write-SubHeader "Check 6: Models Deployed"
    
    $modelsFound = 0
    $modelsMissing = @()
    
    # Check FunctionGemma model
    $functionGemmaExists = Invoke-AdbShell "test -f $ModelsPath/$FunctionGemmaModel && echo yes || echo no"
    
    if ($functionGemmaExists -eq "yes") {
        $fgSize = Invoke-AdbShell "ls -lh $ModelsPath/$FunctionGemmaModel | awk '{print `$5}'"
        Write-Info "✓ FunctionGemma: $fgSize"
        $modelsFound++
    } else {
        $modelsMissing += "FunctionGemma"
        Write-Verbose "✗ FunctionGemma model not found"
    }
    
    # Check Whisper model
    $whisperExists = Invoke-AdbShell "test -f $ModelsPath/$WhisperModel && echo yes || echo no"
    
    if ($whisperExists -eq "yes") {
        $wsSize = Invoke-AdbShell "ls -lh $ModelsPath/$WhisperModel | awk '{print `$5}'"
        Write-Info "✓ Whisper Egyptian: $wsSize"
        $modelsFound++
    } else {
        $modelsMissing += "Whisper Egyptian"
        Write-Verbose "✗ Whisper model not found"
    }
    
    if ($modelsFound -eq 2) {
        $CheckResults["models"] = "PASS"
        Write-Success "All models deployed"
        return $true
    } elseif ($modelsFound -gt 0) {
        $CheckResults["models"] = "WARN"
        $Warnings += "Missing models: $($modelsMissing -join ', ')"
        $Recommendations += "Deploy models: .\scripts\deploy\deploy_functiongemma.sh"
        return $false
    } else {
        $CheckResults["models"] = "FAIL"
        $Failures += "No models found in $ModelsPath"
        $Recommendations += "Deploy models using deployment scripts"
        return $false
    }
}

function Check-WorkflowsDeployed {
    Write-SubHeader "Check 7: Workflows Deployed"
    
    $workflowsFound = 0
    $workflowsMissing = @()
    
    foreach ($workflow in $RequiredWorkflows) {
        $workflowPath = "$WorkflowsPath/$workflow"
        $exists = Invoke-AdbShell "test -f $workflowPath && echo yes || echo no"
        
        if ($exists -eq "yes") {
            $workflowsFound++
            Write-Verbose "✓ $workflow"
        } else {
            $workflowsMissing += $workflow
            Write-Verbose "✗ $workflow"
        }
    }
    
    Write-Info "Workflows: $workflowsFound/$($RequiredWorkflows.Count) deployed"
    
    if ($workflowsFound -eq $RequiredWorkflows.Count) {
        $CheckResults["workflows"] = "PASS"
        Write-Success "All workflows deployed"
        return $true
    } elseif ($workflowsFound -gt 0) {
        $CheckResults["workflows"] = "WARN"
        $Warnings += "Missing workflows: $($workflowsMissing -join ', ')"
        $Recommendations += "Deploy workflows to: $WorkflowsPath"
        return $false
    } else {
        $CheckResults["workflows"] = "FAIL"
        $Failures += "No workflows found"
        $Recommendations += "Copy workflows to device: $WorkflowsPath"
        return $false
    }
}

function Check-StorageSpace {
    Write-SubHeader "Check 8: Storage Space"
    
    $storageInfo = Invoke-AdbShell "df /data | tail -1"
    
    if (!$storageInfo) {
        $CheckResults["storage"] = "WARN"
        $Warnings += "Could not determine available storage"
        return $false
    }
    
    # Parse storage info (format varies by device)
    $availableKb = 0
    if ($storageInfo -match "(\d+)\s+\d+\s+\d+\s+(\d+)") {
        $availableKb = [int]$matches[2]
    } elseif ($storageInfo -match "(\d+)K\s+") {
        $availableKb = [int]$matches[1]
    }
    
    $availableMb = [math]::Round($availableKb / 1024, 2)
    $availableGb = [math]::Round($availableKb / 1048576, 2)
    
    Write-Info "Available storage: ${availableGb}GB (${availableMb}MB)"
    
    if ($availableMb -ge $MinStorageMb) {
        $CheckResults["storage"] = "PASS"
        Write-Success "Sufficient storage (≥${MinStorageMb}MB required)"
        return $true
    } else {
        $CheckResults["storage"] = "FAIL"
        $Failures += "Insufficient storage: ${availableGb}GB available, need ≥2GB"
        $Recommendations += "Free up storage on device"
        $Recommendations += "Remove unused apps and media"
        return $false
    }
}

function Check-BatteryOptimization {
    Write-SubHeader "Check 9: Battery Optimization"
    
    # Check if battery optimization is disabled for the app
    $optimizationStatus = Invoke-AdbShell "dumpsys deviceidle | Select-String '$AppPackage' | Select-String 'WHITELIST'"
    
    if ($optimizationStatus) {
        $CheckResults["battery"] = "PASS"
        Write-Success "Battery optimization disabled (whitelisted)"
        return $true
    }
    
    $CheckResults["battery"] = "WARN"
    $Warnings += "Battery optimization may be enabled"
    $Recommendations += "Disable battery optimization for $AppName:"
    $Recommendations += "  1. Settings → Apps → $AppName → Battery"
    $Recommendations += "  2. Select 'Unrestricted'"
    $Recommendations += "  Or run: adb shell dumpsys deviceidle whitelist +$AppPackage"
    return $false
}

function Check-FunctionalityTest {
    Write-SubHeader "Check 10: Quick Functionality Test"
    
    if ($SkipTests) {
        $CheckResults["functionality"] = "SKIP"
        Write-Info "⊘ Functionality tests skipped"
        return $true
    }
    
    # Test 1: Check app can start
    Write-Info "Testing app launch..."
    $launchResult = Invoke-AdbShell "am start -n $AppPackage/.MainActivity"
    
    if ($launchResult -like "*Error*") {
        $CheckResults["functionality"] = "FAIL"
        $Failures += "App failed to launch"
        $Recommendations += "Check app logs: adb logcat | grep $AppPackage"
        return $false
    }
    
    Start-Sleep -Seconds 2
    
    # Test 2: Check service is running
    Write-Info "Testing background service..."
    $serviceRunning = Invoke-AdbShell "ps | Select-String '$AppPackage'"
    
    if (!$serviceRunning) {
        $CheckResults["functionality"] = "WARN"
        $Warnings += "Background service not running"
        $Recommendations += "Open app to start background service"
        return $false
    }
    
    $CheckResults["functionality"] = "PASS"
    Write-Success "Basic functionality tests passed"
    return $true
}

# =============================================================================
# Auto-Fix Functions
# =============================================================================

function Invoke-AutoFix {
    if (!$AutoFix) {
        return
    }
    
    Write-Header "Attempting Auto-Fixes"
    
    # Fix permissions
    if ($CheckResults["permissions"] -eq "WARN") {
        Write-Info "Granting missing permissions..."
        foreach ($perm in $RequiredPermissions) {
            Invoke-AdbShell "pm grant $AppPackage $perm" 2>$null
        }
        Write-Success "Permissions granted"
    }
    
    # Fix battery optimization
    if ($CheckResults["battery"] -eq "WARN") {
        Write-Info "Disabling battery optimization..."
        Invoke-AdbShell "dumpsys deviceidle whitelist +$AppPackage" 2>$null
        Write-Success "Battery optimization disabled"
    }
    
    # Restart app
    Write-Info "Restarting app..."
    Invoke-AdbShell "am force-stop $AppPackage" 2>$null
    Start-Sleep -Seconds 1
    Invoke-AdbShell "am start -n $AppPackage/.MainActivity" 2>$null
    Write-Success "App restarted"
    
    Write-SubHeader "Auto-Fix Complete"
}

# =============================================================================
# Report Generation
# =============================================================================

function Get-StatusIcon {
    param([string]$Status)
    
    switch ($Status) {
        "PASS" { return "✅" }
        "FAIL" { return "❌" }
        "WARN" { return "⚠️" }
        "SKIP" { return "⊘" }
        default { return "❓" }
    }
}

function Generate-MarkdownReport {
    $report = @()
    
    $report += "# EgyptianAgent Deployment Verification Report`n"
    $report += "**Generated:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n"
    $report += "**Device:** $(Invoke-AdbShellQuiet 'getprop ro.product.model')`n"
    $report += "**Android:** $(Invoke-AdbShellQuiet 'getprop ro.build.version.release')`n"
    $report += ""
    
    $report += "## Summary`n"
    
    $passCount = ($CheckResults.Values | Where-Object { $_ -eq "PASS" }).Count
    $failCount = ($CheckResults.Values | Where-Object { $_ -eq "FAIL" }).Count
    $warnCount = ($CheckResults.Values | Where-Object { $_ -eq "WARN" }).Count
    $skipCount = ($CheckResults.Values | Where-Object { $_ -eq "SKIP" }).Count
    $totalCount = $CheckResults.Count
    
    $report += "| Status | Count |"
    $report += "|--------|-------|"
    $report += "| ✅ Pass | $passCount |"
    $report += "| ❌ Fail | $failCount |"
    $report += "| ⚠️ Warning | $warnCount |"
    $report += "| ⊘ Skipped | $skipCount |"
    $report += "| **Total** | **$totalCount** |"
    $report += ""
    
    if ($failCount -eq 0 -and $warnCount -eq 0) {
        $report += "**Overall Status:** ✅ ALL CHECKS PASSED`n"
    } elseif ($failCount -eq 0) {
        $report += "**Overall Status:** ⚠️ PASSED WITH WARNINGS`n"
    } else {
        $report += "**Overall Status:** ❌ FAILED`n"
    }
    $report += ""
    
    $report += "## Detailed Results`n"
    
    $checkNames = @{
        "build" = "Build Verification"
        "device" = "Device Connection"
        "app_install" = "App Installation"
        "permissions" = "Permissions Granted"
        "accessibility" = "Accessibility Service"
        "models" = "Models Deployed"
        "workflows" = "Workflows Deployed"
        "storage" = "Storage Space"
        "battery" = "Battery Optimization"
        "functionality" = "Functionality Test"
    }
    
    $checkNum = 1
    foreach ($check in $checkNames.GetEnumerator()) {
        $status = $CheckResults[$check.Key]
        if ($status) {
            $icon = Get-StatusIcon $status
            $report += "### $checkNum. $($check.Value)`n"
            $report += "- **Status:** $icon $status`n"
            $report += ""
            $checkNum++
        }
    }
    
    if ($Failures.Count -gt 0) {
        $report += "## Failures`n"
        foreach ($failure in $Failures) {
            $report += "- ❌ $failure"
        }
        $report += ""
    }
    
    if ($Warnings.Count -gt 0) {
        $report += "## Warnings`n"
        foreach ($warning in $Warnings) {
            $report += "- ⚠️ $warning"
        }
        $report += ""
    }
    
    if ($Recommendations.Count -gt 0) {
        $report += "## Recommendations`n"
        foreach ($rec in $Recommendations) {
            $report += "- $rec"
        }
        $report += ""
    }
    
    return $report -join "`n"
}

function Generate-JsonReport {
    $report = @{
        timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        device = @{
            model = Invoke-AdbShellQuiet "getprop ro.product.model"
            android = Invoke-AdbShellQuiet "getprop ro.build.version.release"
            sdk = Invoke-AdbShellQuiet "getprop ro.build.version.sdk"
        }
        summary = @{
            total = $CheckResults.Count
            pass = ($CheckResults.Values | Where-Object { $_ -eq "PASS" }).Count
            fail = ($CheckResults.Values | Where-Object { $_ -eq "FAIL" }).Count
            warn = ($CheckResults.Values | Where-Object { $_ -eq "WARN" }).Count
            skip = ($CheckResults.Values | Where-Object { $_ -eq "SKIP" }).Count
        }
        checks = $CheckResults
        failures = $Failures
        warnings = $Warnings
        recommendations = $Recommendations
    }
    
    return $report | ConvertTo-Json -Depth 5
}

function Generate-TextReport {
    $report = @()
    
    $report += "EgyptianAgent Deployment Verification Report"
    $report += "=" * 50
    $report += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    $report += "Device: $(Invoke-AdbShellQuiet 'getprop ro.product.model')"
    $report += "Android: $(Invoke-AdbShellQuiet 'getprop ro.build.version.release')"
    $report += ""
    
    $report += "SUMMARY"
    $report += "-" * 50
    
    $passCount = ($CheckResults.Values | Where-Object { $_ -eq "PASS" }).Count
    $failCount = ($CheckResults.Values | Where-Object { $_ -eq "FAIL" }).Count
    $warnCount = ($CheckResults.Values | Where-Object { $_ -eq "WARN" }).Count
    
    $report += "Pass: $passCount"
    $report += "Fail: $failCount"
    $report += "Warn: $warnCount"
    $report += ""
    
    if ($failCount -eq 0 -and $warnCount -eq 0) {
        $report += "Overall Status: ALL CHECKS PASSED"
    } elseif ($failCount -eq 0) {
        $report += "Overall Status: PASSED WITH WARNINGS"
    } else {
        $report += "Overall Status: FAILED"
    }
    $report += ""
    
    $report += "DETAILED RESULTS"
    $report += "-" * 50
    
    foreach ($check in $CheckResults.GetEnumerator()) {
        $icon = Get-StatusIcon $check.Value
        $report += "$icon $($check.Key): $($check.Value)"
    }
    $report += ""
    
    if ($Failures.Count -gt 0) {
        $report += "FAILURES:"
        foreach ($failure in $Failures) {
            $report += "  - $failure"
        }
        $report += ""
    }
    
    if ($Recommendations.Count -gt 0) {
        $report += "RECOMMENDATIONS:"
        foreach ($rec in $Recommendations) {
            $report += "  - $rec"
        }
    }
    
    return $report -join "`n"
}

function Save-Report {
    param([string]$Content)
    
    if ($OutputFile) {
        $reportPath = $OutputFile
    } else {
        if (!(Test-Path $ReportDir)) {
            New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
        }
        $extension = switch ($Output) {
            "Markdown" { ".md" }
            "Json" { ".json" }
            "Text" { ".txt" }
        }
        $reportPath = Join-Path $ReportDir "verification_report_$Timestamp$extension"
    }
    
    $Content | Out-File -FilePath $reportPath -Encoding UTF8
    Write-Info "Report saved to: $reportPath"
}

# =============================================================================
# Main Execution
# =============================================================================

function Invoke-Verification {
    Write-Header "EgyptianAgent Deployment Verification"
    
    Write-Host "Version: $ScriptVersion"
    Write-Host "Project: $ProjectRoot"
    Write-Host "Timestamp: $Timestamp"
    Write-Host ""
    
    # Check prerequisites first
    if (!(Check-Prerequisites)) {
        Write-Host ""
        Write-Failure "Prerequisites check failed. Cannot continue verification."
        Write-Host ""
        Write-Host "Please run the setup script first:"
        Write-Host "  .\scripts\setup\windows_setup.ps1"
        Write-Host ""
        Write-Host "Or see the prerequisites guide:"
        Write-Host "  docs\deployment\DEPLOYMENT_PREREQUISITES.md"
        Write-Host ""
        exit 3
    }
    
    Write-Host ""
    
    # Run all checks
    Write-Header "Running Verification Checks"
    
    Check-BuildVerification | Out-Null
    Check-DeviceConnection | Out-Null
    Check-AppInstallation | Out-Null
    Check-Permissions | Out-Null
    Check-AccessibilityService | Out-Null
    Check-ModelsDeployed | Out-Null
    Check-WorkflowsDeployed | Out-Null
    Check-StorageSpace | Out-Null
    Check-BatteryOptimization | Out-Null
    Check-FunctionalityTest | Out-Null
    
    # Attempt auto-fixes
    Invoke-AutoFix
    
    # Generate report
    Write-Header "Generating Report"
    
    $report = switch ($Output) {
        "Markdown" { Generate-MarkdownReport }
        "Json" { Generate-JsonReport }
        "Text" { Generate-TextReport }
    }
    
    # Output report
    if ($Ci -or $Verbose) {
        Write-Host ""
        Write-Host $report
    }
    
    # Save report
    if ($OutputFile -or $Ci) {
        Save-Report $report
    }
    
    # Summary
    Write-Host ""
    Write-Header "Verification Summary"
    
    $passCount = ($CheckResults.Values | Where-Object { $_ -eq "PASS" }).Count
    $failCount = ($CheckResults.Values | Where-Object { $_ -eq "FAIL" }).Count
    $warnCount = ($CheckResults.Values | Where-Object { $_ -eq "WARN" }).Count
    $totalCount = $CheckResults.Count
    
    Write-Host "Checks: $passCount/$totalCount passed"
    
    if ($failCount -gt 0) {
        Write-Failure "$failCount check(s) failed"
    }
    
    if ($warnCount -gt 0) {
        Write-Warn "$warnCount warning(s)"
    }
    
    Write-Host ""
    
    # Determine exit code
    if ($failCount -gt 0) {
        Write-Failure "Verification FAILED"
        exit 1
    } elseif ($warnCount -gt 0) {
        Write-Warn "Verification PASSED with warnings"
        exit 0
    } else {
        Write-Success "Verification PASSED - All checks successful!"
        exit 0
    }
}

# =============================================================================
# Entry Point
# =============================================================================

if ($Help) {
    Show-Help
}

try {
    Invoke-Verification
} catch {
    Write-Failure "Verification failed with error: $_"
    exit 3
}
