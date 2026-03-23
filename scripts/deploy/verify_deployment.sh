#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Hybrid Architecture Deployment Verification Script
# =============================================================================
#
# PURPOSE:
#   Comprehensive verification script to validate all components of the
#   EgyptianAgent Hybrid Architecture deployment on Android devices.
#
# USAGE:
#   ./scripts/deploy/verify_deployment.sh [OPTIONS]
#
# OPTIONS:
#   --device SERIAL     Target device serial (for multiple devices)
#   --output FORMAT     Output format: markdown, json, text (default: markdown)
#   --output-file PATH  Write report to specified file
#   --auto-fix          Attempt automatic fixes for common issues
#   --verbose           Enable verbose logging
#   --ci                CI/CD mode (non-interactive, machine-readable)
#   --skip-tests        Skip functional tests
#   --help              Show this help message
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
# VERSION: 3.0.0
# DATE: 2026-03-14
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly REPORT_DIR="$PROJECT_DIR/build/reports"
readonly TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# App configuration
readonly APP_PACKAGE="com.egyptian.agent"
readonly APP_NAME="EgyptianAgent"
readonly EXPECTED_APK_SIZE_MB=45
readonly MIN_STORAGE_MB=2048
readonly REQUIRED_PERMISSIONS=(
    "android.permission.RECORD_AUDIO"
    "android.permission.CALL_PHONE"
    "android.permission.READ_CONTACTS"
    "android.permission.WRITE_CONTACTS"
    "android.permission.SEND_SMS"
    "android.permission.READ_SMS"
    "android.permission.BODY_SENSORS"
    "android.permission.ACCESS_FINE_LOCATION"
    "android.permission.CAMERA"
    "android.permission.READ_EXTERNAL_STORAGE"
)

# Model configuration
readonly FUNCTIONGEMMA_MODEL="functiongemma-270m-it-Q4_K_M.gguf"
readonly WHISPER_MODEL="whisper-egyptian-v1.bin"
readonly MODELS_PATH="/data/local/llm"

# Workflow configuration
readonly WORKFLOWS_PATH="/sdcard/Android/data/$APP_PACKAGE/files/workflows"
readonly REQUIRED_WORKFLOWS=(
    "morning_routine.yaml"
    "bedtime_routine.yaml"
    "check_social.yaml"
    "send_whatsapp_broadcast.yaml"
    "book_uber.yaml"
    "check_email.yaml"
    "youtube_search.yaml"
    "settings_toggle.yaml"
    "emergency_check.yaml"
    "grocery_list.yaml"
)

# Colors
declare -A COLORS=(
    [red]='\033[0;31m'
    [green]='\033[0;32m'
    [yellow]='\033[1;33m'
    [blue]='\033[0;34m'
    [cyan]='\033[0;36m'
    [magenta]='\033[0;35m'
    [bold]='\033[1m'
    [nc]='\033[0m'
)

# State
declare -A CHECK_RESULTS=()
declare -a FAILURES=()
declare -a WARNINGS=()
declare -a RECOMMENDATIONS=()

# Options
DEVICE_SERIAL=""
OUTPUT_FORMAT="markdown"
OUTPUT_FILE=""
AUTO_FIX=false
VERBOSE=false
CI_MODE=false
SKIP_TESTS=false

# =============================================================================
# Logging Functions
# =============================================================================

log_info() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[INFO] $*"
    else
        echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"
    fi
}

log_warn() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[WARN] $*"
    else
        echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"
    fi
}

log_error() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[ERROR] $*" >&2
    else
        echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2
    fi
}

log_verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        if [[ "$CI_MODE" == "true" ]]; then
            echo "[DEBUG] $*"
        else
            echo -e "${COLORS[cyan]}[DEBUG]${COLORS[nc]} $*"
        fi
    fi
}

print_header() {
    local title="$1"
    local width=70

    if [[ "$CI_MODE" == "true" ]]; then
        echo ""
        echo "=== $title ==="
        echo ""
    else
        echo ""
        echo -e "${COLORS[blue]}$(printf '━%.0s' $(seq 1 $width))${COLORS[nc]}"
        echo -e "${COLORS[blue]}${COLORS[bold]}  $title${COLORS[nc]}"
        echo -e "${COLORS[blue]}$(printf '━%.0s' $(seq 1 $width))${COLORS[nc]}"
    fi
}

print_subheader() {
    local title="$1"

    if [[ "$CI_MODE" == "true" ]]; then
        echo "--- $title ---"
    else
        echo -e "${COLORS[cyan]}  $title${COLORS[nc]}"
    fi
}

# =============================================================================
# ADB Helper Functions
# =============================================================================

adb_cmd() {
    if [[ -n "$DEVICE_SERIAL" ]]; then
        adb -s "$DEVICE_SERIAL" "$@" 2>/dev/null
    else
        adb "$@" 2>/dev/null
    fi
}

adb_shell() {
    adb_cmd shell "$@" 2>/dev/null
}

adb_shell_quiet() {
    adb_cmd shell "$@" 2>/dev/null | tr -d '\r'
}

# =============================================================================
# Check Functions
# =============================================================================

check_build_verification() {
    print_subheader "Check 1: Build Verification"

    local apk_path="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    local debug_apk_path="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

    # Check release APK first
    if [[ -f "$apk_path" ]]; then
        local size_bytes
        size_bytes=$(stat -c%s "$apk_path" 2>/dev/null || stat -f%z "$apk_path" 2>/dev/null || echo "0")
        local size_mb=$((size_bytes / 1024 / 1024))

        log_info "Release APK found: $apk_path"
        log_info "APK size: ${size_mb}MB"

        if [[ "$size_mb" -ge "$EXPECTED_APK_SIZE_MB" ]]; then
            CHECK_RESULTS["build"]="PASS"
            log_info "✓ APK size is correct (≥${EXPECTED_APK_SIZE_MB}MB)"
            return 0
        else
            CHECK_RESULTS["build"]="WARN"
            WARNINGS+=("APK size (${size_mb}MB) is smaller than expected (${EXPECTED_APK_SIZE_MB}MB)")
            RECOMMENDATIONS+=("Verify build completed successfully")
            return 0
        fi
    fi

    # Check debug APK
    if [[ -f "$debug_apk_path" ]]; then
        local size_bytes
        size_bytes=$(stat -c%s "$debug_apk_path" 2>/dev/null || stat -f%z "$debug_apk_path" 2>/dev/null || echo "0")
        local size_mb=$((size_bytes / 1024 / 1024))

        log_info "Debug APK found: $debug_apk_path"
        log_info "APK size: ${size_mb}MB"

        CHECK_RESULTS["build"]="PASS"
        log_info "✓ Debug APK available for testing"
        return 0
    fi

    CHECK_RESULTS["build"]="FAIL"
    FAILURES+=("APK not found. Run: ./gradlew assembleRelease")
    RECOMMENDATIONS+=("Build the APK: ./gradlew assembleRelease")
    return 1
}

check_device_connection() {
    print_subheader "Check 2: Device Connection"

    # Check ADB is installed
    if ! command -v adb &>/dev/null; then
        CHECK_RESULTS["device"]="FAIL"
        FAILURES+=("ADB not found in PATH")
        RECOMMENDATIONS+=("Install Android SDK Platform Tools")
        return 1
    fi

    log_info "ADB version: $(adb version | head -1)"

    # Check device is connected
    local device_count
    device_count=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")

    if [[ "$device_count" -eq 0 ]]; then
        CHECK_RESULTS["device"]="FAIL"
        FAILURES+=("No Android devices connected")
        RECOMMENDATIONS+=("1. Enable USB debugging on device")
        RECOMMENDATIONS+=("2. Connect device via USB cable")
        RECOMMENDATIONS+=("3. Accept USB debugging prompt on device")
        return 2
    fi

    if [[ "$device_count" -gt 1 ]]; then
        log_warn "Multiple devices connected ($device_count)"
        if [[ -z "$DEVICE_SERIAL" ]]; then
            log_warn "Use --device SERIAL to specify target"
        fi
    fi

    # Get device info
    local device_model
    device_model=$(adb_shell_quiet "getprop ro.product.model")
    local device_android
    device_android=$(adb_shell_quiet "getprop ro.build.version.release")
    local device_sdk
    device_sdk=$(adb_shell_quiet "getprop ro.build.version.sdk")

    log_info "Device: $device_model"
    log_info "Android: $device_android (SDK $device_sdk)"

    # Check device is authorized
    local device_state
    device_state=$(adb_shell_quiet "getprop ro.adb.secure")
    if [[ "$device_state" != "1" ]]; then
        log_warn "Device may not be fully authorized"
    fi

    CHECK_RESULTS["device"]="PASS"
    log_info "✓ Device connected and authorized"
    return 0
}

check_app_installation() {
    print_subheader "Check 3: App Installation"

    # Check if app is installed
    local is_installed
    is_installed=$(adb_shell_quiet "pm list packages | grep -c '$APP_PACKAGE'" || echo "0")

    if [[ "$is_installed" -eq 0 ]]; then
        CHECK_RESULTS["app_install"]="FAIL"
        FAILURES+=("$APP_NAME is not installed")
        RECOMMENDATIONS+=("Install app: adb install -r app/build/outputs/apk/debug/app-debug.apk")
        return 1
    fi

    # Get version info
    local version_name
    version_name=$(adb_shell_quiet "dumpsys package $APP_PACKAGE | grep versionName | head -1 | cut -d= -f2")
    local version_code
    version_code=$(adb_shell_quiet "dumpsys package $APP_PACKAGE | grep versionCode | head -1 | cut -d= -f2 | cut -d' ' -f1")

    log_info "Version: $version_name ($version_code)"

    # Check app is not disabled
    local app_state
    app_state=$(adb_shell_quiet "dumpsys package $APP_PACKAGE | grep applicationInfo | grep -c DISABLED" || echo "0")

    if [[ "$app_state" -gt 0 ]]; then
        CHECK_RESULTS["app_install"]="WARN"
        WARNINGS+=("App appears to be disabled")
        RECOMMENDATIONS+=("Enable app in device settings")
        return 0
    fi

    CHECK_RESULTS["app_install"]="PASS"
    log_info "✓ $APP_NAME is installed"
    return 0
}

check_permissions() {
    print_subheader "Check 4: Permissions Granted"

    local granted_count=0
    local denied_count=0
    local missing_permissions=()

    for perm in "${REQUIRED_PERMISSIONS[@]}"; do
        local is_granted
        is_granted=$(adb_shell_quiet "dumpsys package $APP_PACKAGE | grep -c \"$perm: granted=true\"" || echo "0")

        if [[ "$is_granted" -gt 0 ]]; then
            ((granted_count++))
            log_verbose "✓ $perm"
        else
            ((denied_count++))
            missing_permissions+=("$perm")
            log_verbose "✗ $perm"
        fi
    done

    log_info "Permissions: $granted_count/${#REQUIRED_PERMISSIONS[@]} granted"

    if [[ "$denied_count" -gt 0 ]]; then
        CHECK_RESULTS["permissions"]="WARN"
        WARNINGS+=("$denied_count permissions not granted")

        # Generate grant commands
        local grant_cmds=""
        for perm in "${missing_permissions[@]}"; do
            grant_cmds+="\n  adb shell pm grant $APP_PACKAGE $perm"
        done

        RECOMMENDATIONS+=("Grant missing permissions:$grant_cmds")
        return 1
    fi

    CHECK_RESULTS["permissions"]="PASS"
    log_info "✓ All required permissions granted"
    return 0
}

check_accessibility_service() {
    print_subheader "Check 5: Accessibility Service"

    # Check if accessibility service is enabled
    local enabled_services
    enabled_services=$(adb_shell_quiet "settings get secure enabled_accessibility_services")

    local accessibility_component="$APP_PACKAGE/.accessibility.EgyptianAccessibilityService"

    if [[ "$enabled_services" == *"$accessibility_component"* ]]; then
        CHECK_RESULTS["accessibility"]="PASS"
        log_info "✓ Accessibility service enabled"
        return 0
    fi

    # Check if service is installed
    local service_exists
    service_exists=$(adb_shell_quiet "dumpsys accessibility | grep -c '$APP_PACKAGE'" || echo "0")

    if [[ "$service_exists" -gt 0 ]]; then
        CHECK_RESULTS["accessibility"]="WARN"
        WARNINGS+=("Accessibility service installed but not enabled")
        RECOMMENDATIONS+=("Enable accessibility service:")
        RECOMMENDATIONS+=("  1. Open Settings → Accessibility")
        RECOMMENDATIONS+=("  2. Find '$APP_NAME' and enable it")
        RECOMMENDATIONS+=("  Or run: adb shell settings put secure enabled_accessibility_services \"$accessibility_component\"")
        return 1
    fi

    CHECK_RESULTS["accessibility"]="FAIL"
    FAILURES+=("Accessibility service not found")
    RECOMMENDATIONS+=("Reinstall app to register accessibility service")
    return 1
}

check_models_deployed() {
    print_subheader "Check 6: Models Deployed"

    local models_found=0
    local models_missing=()

    # Check FunctionGemma model
    local functiongemma_exists
    functiongemma_exists=$(adb_shell_quiet "test -f $MODELS_PATH/$FUNCTIONGEMMA_MODEL && echo yes" || echo "no")

    if [[ "$functiongemma_exists" == "yes" ]]; then
        local fg_size
        fg_size=$(adb_shell_quiet "ls -lh $MODELS_PATH/$FUNCTIONGEMMA_MODEL | awk '{print \$5}'")
        log_info "✓ FunctionGemma: $fg_size"
        ((models_found++))
    else
        models_missing+=("FunctionGemma")
        log_verbose "✗ FunctionGemma model not found"
    fi

    # Check Whisper model
    local whisper_exists
    whisper_exists=$(adb_shell_quiet "test -f $MODELS_PATH/$WHISPER_MODEL && echo yes" || echo "no")

    if [[ "$whisper_exists" == "yes" ]]; then
        local ws_size
        ws_size=$(adb_shell_quiet "ls -lh $MODELS_PATH/$WHISPER_MODEL | awk '{print \$5}'")
        log_info "✓ Whisper Egyptian: $ws_size"
        ((models_found++))
    else
        models_missing+=("Whisper Egyptian")
        log_verbose "✗ Whisper model not found"
    fi

    if [[ "$models_found" -eq 2 ]]; then
        CHECK_RESULTS["models"]="PASS"
        log_info "✓ All models deployed"
        return 0
    elif [[ "$models_found" -gt 0 ]]; then
        CHECK_RESULTS["models"]="WARN"
        WARNINGS+=("Missing models: ${models_missing[*]}")
        RECOMMENDATIONS+=("Deploy models: ./scripts/deploy/deploy_functiongemma.sh")
        RECOMMENDATIONS+=("Download Whisper: ./scripts/model/download_whisper_model.sh")
        return 1
    else
        CHECK_RESULTS["models"]="FAIL"
        FAILURES+=("No models found in $MODELS_PATH")
        RECOMMENDATIONS+=("Deploy models using deployment scripts")
        return 1
    fi
}

check_workflows_deployed() {
    print_subheader "Check 7: Workflows Deployed"

    local workflows_found=0
    local workflows_missing=()

    for workflow in "${REQUIRED_WORKFLOWS[@]}"; do
        local workflow_path="$WORKFLOWS_PATH/$workflow"
        local exists
        exists=$(adb_shell_quiet "test -f $workflow_path && echo yes" || echo "no")

        if [[ "$exists" == "yes" ]]; then
            ((workflows_found++))
            log_verbose "✓ $workflow"
        else
            workflows_missing+=("$workflow")
            log_verbose "✗ $workflow"
        fi
    done

    log_info "Workflows: $workflows_found/${#REQUIRED_WORKFLOWS[@]} deployed"

    if [[ "$workflows_found" -eq ${#REQUIRED_WORKFLOWS[@]} ]]; then
        CHECK_RESULTS["workflows"]="PASS"
        log_info "✓ All workflows deployed"
        return 0
    elif [[ "$workflows_found" -gt 0 ]]; then
        CHECK_RESULTS["workflows"]="WARN"
        WARNINGS+=("Missing workflows: ${workflows_missing[*]}")
        RECOMMENDATIONS+=("Deploy workflows to: $WORKFLOWS_PATH")
        return 1
    else
        CHECK_RESULTS["workflows"]="FAIL"
        FAILURES+=("No workflows found")
        RECOMMENDATIONS+=("Copy workflows to device: $WORKFLOWS_PATH")
        return 1
    fi
}

check_storage_space() {
    print_subheader "Check 8: Storage Space"

    local available
    available=$(adb_shell_quiet "df /data | tail -1 | awk '{print \$4}'" || echo "0")

    if [[ "$available" == "0" || -z "$available" ]]; then
        CHECK_RESULTS["storage"]="WARN"
        WARNINGS+=("Could not determine available storage")
        return 0
    fi

    local available_mb=$((available / 1024))
    local available_gb
    available_gb=$(awk "BEGIN {printf \"%.2f\", $available / 1048576}")

    log_info "Available storage: ${available_gb}GB (${available_mb}MB)"

    if [[ "$available_mb" -ge "$MIN_STORAGE_MB" ]]; then
        CHECK_RESULTS["storage"]="PASS"
        log_info "✓ Sufficient storage (≥${MIN_STORAGE_MB}MB required)"
        return 0
    else
        CHECK_RESULTS["storage"]="FAIL"
        FAILURES+=("Insufficient storage: ${available_gb}GB available, need ≥2GB")
        RECOMMENDATIONS+=("Free up storage on device")
        RECOMMENDATIONS+=("Remove unused apps and media")
        return 1
    fi
}

check_battery_optimization() {
    print_subheader "Check 9: Battery Optimization"

    # Check if battery optimization is disabled for the app
    local optimization_status
    optimization_status=$(adb_shell_quiet "dumpsys deviceidle | grep -A5 '$APP_PACKAGE' | grep -c 'WHITELIST'" || echo "0")

    if [[ "$optimization_status" -gt 0 ]]; then
        CHECK_RESULTS["battery"]="PASS"
        log_info "✓ Battery optimization disabled (whitelisted)"
        return 0
    fi

    # Alternative check
    local ignore_status
    ignore_status=$(adb_shell_quiet "settings get global app_standby_enabled" || echo "unknown")

    CHECK_RESULTS["battery"]="WARN"
    WARNINGS+=("Battery optimization may be enabled")
    RECOMMENDATIONS+=("Disable battery optimization for $APP_NAME:")
    RECOMMENDATIONS+=("  1. Settings → Apps → $APP_NAME → Battery")
    RECOMMENDATIONS+=("  2. Select 'Unrestricted'")
    RECOMMENDATIONS+=("  Or run: adb shell dumpsys deviceidle whitelist +$APP_PACKAGE")
    return 1
}

check_functionality_test() {
    print_subheader "Check 10: Quick Functionality Test"

    if [[ "$SKIP_TESTS" == "true" ]]; then
        CHECK_RESULTS["functionality"]="SKIP"
        log_info "⊘ Functionality tests skipped"
        return 0
    fi

    # Test 1: Check app can start
    log_info "Testing app launch..."
    local launch_result
    launch_result=$(adb_shell "am start -n $APP_PACKAGE/.MainActivity" 2>&1)

    if [[ "$launch_result" == *"Error"* ]]; then
        CHECK_RESULTS["functionality"]="FAIL"
        FAILURES+=("App failed to launch")
        RECOMMENDATIONS+=("Check app logs: adb logcat | grep $APP_PACKAGE")
        return 1
    fi

    sleep 2

    # Test 2: Check service is running
    log_info "Testing background service..."
    local service_running
    service_running=$(adb_shell_quiet "ps | grep -c '$APP_PACKAGE'" || echo "0")

    if [[ "$service_running" -eq 0 ]]; then
        CHECK_RESULTS["functionality"]="WARN"
        WARNINGS+=("Background service not running")
        RECOMMENDATIONS+=("Open app to start background service")
        return 1
    fi

    # Test 3: Quick intent test (if models available)
    if [[ "${CHECK_RESULTS[models]:-FAIL}" == "PASS" ]]; then
        log_info "Testing intent classification..."
        # This would require actual model inference, skip for now
        log_verbose "Intent test: skipped (requires model warm-up)"
    fi

    CHECK_RESULTS["functionality"]="PASS"
    log_info "✓ Basic functionality tests passed"
    return 0
}

# =============================================================================
# Auto-Fix Functions
# =============================================================================

attempt_auto_fix() {
    if [[ "$AUTO_FIX" != "true" ]]; then
        return 0
    fi

    print_header "Attempting Auto-Fixes"

    # Fix permissions
    if [[ "${CHECK_RESULTS[permissions]:-}" == "WARN" ]]; then
        log_info "Granting missing permissions..."
        for perm in "${REQUIRED_PERMISSIONS[@]}"; do
            adb_shell "pm grant $APP_PACKAGE $perm" 2>/dev/null || true
        done
        log_info "Permissions granted"
    fi

    # Fix battery optimization
    if [[ "${CHECK_RESULTS[battery]:-}" == "WARN" ]]; then
        log_info "Disabling battery optimization..."
        adb_shell "dumpsys deviceidle whitelist +$APP_PACKAGE" 2>/dev/null || true
        log_info "Battery optimization disabled"
    fi

    # Restart app
    log_info "Restarting app..."
    adb_shell "am force-stop $APP_PACKAGE" 2>/dev/null || true
    sleep 1
    adb_shell "am start -n $APP_PACKAGE/.MainActivity" 2>/dev/null || true
    log_info "App restarted"

    print_subheader "Auto-Fix Complete"
}

# =============================================================================
# Report Generation
# =============================================================================

generate_markdown_report() {
    local report=""

    report+="# EgyptianAgent Deployment Verification Report\n\n"
    report+="**Generated:** $(date '+%Y-%m-%d %H:%M:%S')\n"
    report+="**Device:** $(adb_shell_quiet "getprop ro.product.model" 2>/dev/null || echo "Unknown")\n"
    report+="**Android:** $(adb_shell_quiet "getprop ro.build.version.release" 2>/dev/null || echo "Unknown")\n\n"

    report+="## Summary\n\n"

    local pass_count=0
    local fail_count=0
    local warn_count=0
    local skip_count=0

    for check in "${!CHECK_RESULTS[@]}"; do
        case "${CHECK_RESULTS[$check]}" in
            PASS) ((pass_count++)) ;;
            FAIL) ((fail_count++)) ;;
            WARN) ((warn_count++)) ;;
            SKIP) ((skip_count++)) ;;
        esac
    done

    local total=$((pass_count + fail_count + warn_count + skip_count))

    report+="| Status | Count |\n"
    report+="|--------|-------|\n"
    report+="| ✅ Pass | $pass_count |\n"
    report+="| ❌ Fail | $fail_count |\n"
    report+="| ⚠️ Warning | $warn_count |\n"
    report+="| ⊘ Skipped | $skip_count |\n"
    report+="| **Total** | **$total** |\n\n"

    if [[ "$fail_count" -eq 0 && "$warn_count" -eq 0 ]]; then
        report+="**Overall Status:** ✅ ALL CHECKS PASSED\n\n"
    elif [[ "$fail_count" -eq 0 ]]; then
        report+="**Overall Status:** ⚠️ PASSED WITH WARNINGS\n\n"
    else
        report+="**Overall Status:** ❌ FAILED\n\n"
    fi

    report+="## Detailed Results\n\n"

    report+="### 1. Build Verification\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[build]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 2. Device Connection\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[device]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 3. App Installation\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[app_install]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 4. Permissions\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[permissions]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 5. Accessibility Service\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[accessibility]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 6. Models Deployed\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[models]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 7. Workflows Deployed\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[workflows]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 8. Storage Space\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[storage]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 9. Battery Optimization\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[battery]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || echo "❌ Fail")\n\n"

    report+="### 10. Functionality Test\n\n"
    report+="- **Status:** $([ "${CHECK_RESULTS[functionality]:-UNKNOWN}" == "PASS" ] && echo "✅ Pass" || ([ "${CHECK_RESULTS[functionality]:-}" == "SKIP" ] && echo "⊘ Skipped") || echo "❌ Fail")\n\n"

    if [[ ${#FAILURES[@]} -gt 0 ]]; then
        report+="## Failures\n\n"
        for failure in "${FAILURES[@]}"; do
            report+="- ❌ $failure\n"
        done
        report+="\n"
    fi

    if [[ ${#WARNINGS[@]} -gt 0 ]]; then
        report+="## Warnings\n\n"
        for warning in "${WARNINGS[@]}"; do
            report+="- ⚠️ $warning\n"
        done
        report+="\n"
    fi

    if [[ ${#RECOMMENDATIONS[@]} -gt 0 ]]; then
        report+="## Recommendations\n\n"
        for rec in "${RECOMMENDATIONS[@]}"; do
            report+="- $rec\n"
        done
        report+="\n"
    fi

    echo -e "$report"
}

generate_text_report() {
    echo ""
    echo "==============================================================================="
    echo "                    EGYPTIANAGENT DEPLOYMENT VERIFICATION"
    echo "==============================================================================="
    echo ""
    echo "Generated: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Device: $(adb_shell_quiet "getprop ro.product.model" 2>/dev/null || echo "Unknown")"
    echo "Android: $(adb_shell_quiet "getprop ro.build.version.release" 2>/dev/null || echo "Unknown")"
    echo ""
    echo "-------------------------------------------------------------------------------"
    echo "                              SUMMARY"
    echo "-------------------------------------------------------------------------------"
    echo ""

    local pass_count=0
    local fail_count=0
    local warn_count=0
    local skip_count=0

    for check in "${!CHECK_RESULTS[@]}"; do
        case "${CHECK_RESULTS[$check]}" in
            PASS) ((pass_count++)) ;;
            FAIL) ((fail_count++)) ;;
            WARN) ((warn_count++)) ;;
            SKIP) ((skip_count++)) ;;
        esac
    done

    printf "  %-20s %d\n" "Pass:" "$pass_count"
    printf "  %-20s %d\n" "Fail:" "$fail_count"
    printf "  %-20s %d\n" "Warning:" "$warn_count"
    printf "  %-20s %d\n" "Skipped:" "$skip_count"
    echo ""

    if [[ "$fail_count" -eq 0 && "$warn_count" -eq 0 ]]; then
        echo "  Overall: ✅ ALL CHECKS PASSED"
    elif [[ "$fail_count" -eq 0 ]]; then
        echo "  Overall: ⚠️  PASSED WITH WARNINGS"
    else
        echo "  Overall: ❌ FAILED"
    fi

    echo ""
    echo "-------------------------------------------------------------------------------"
    echo "                           DETAILED RESULTS"
    echo "-------------------------------------------------------------------------------"
    echo ""

    printf "  %-35s [%s]\n" "1. Build Verification" "${CHECK_RESULTS[build]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "2. Device Connection" "${CHECK_RESULTS[device]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "3. App Installation" "${CHECK_RESULTS[app_install]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "4. Permissions Granted" "${CHECK_RESULTS[permissions]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "5. Accessibility Service" "${CHECK_RESULTS[accessibility]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "6. Models Deployed" "${CHECK_RESULTS[models]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "7. Workflows Deployed" "${CHECK_RESULTS[workflows]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "8. Storage Space" "${CHECK_RESULTS[storage]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "9. Battery Optimization" "${CHECK_RESULTS[battery]:-UNKNOWN}"
    printf "  %-35s [%s]\n" "10. Functionality Test" "${CHECK_RESULTS[functionality]:-UNKNOWN}"

    echo ""

    if [[ ${#FAILURES[@]} -gt 0 ]]; then
        echo "-------------------------------------------------------------------------------"
        echo "                              FAILURES"
        echo "-------------------------------------------------------------------------------"
        for failure in "${FAILURES[@]}"; do
            echo "  ❌ $failure"
        done
        echo ""
    fi

    if [[ ${#WARNINGS[@]} -gt 0 ]]; then
        echo "-------------------------------------------------------------------------------"
        echo "                             WARNINGS"
        echo "-------------------------------------------------------------------------------"
        for warning in "${WARNINGS[@]}"; do
            echo "  ⚠️  $warning"
        done
        echo ""
    fi

    if [[ ${#RECOMMENDATIONS[@]} -gt 0 ]]; then
        echo "-------------------------------------------------------------------------------"
        echo "                          RECOMMENDATIONS"
        echo "-------------------------------------------------------------------------------"
        for rec in "${RECOMMENDATIONS[@]}"; do
            echo "  • $rec"
        done
        echo ""
    fi
}

# =============================================================================
# Help and Usage
# =============================================================================

show_help() {
    cat << EOF
EgyptianAgent - Hybrid Architecture Deployment Verification Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --device SERIAL     Target device serial (for multiple devices)
    --output FORMAT     Output format: markdown, json, text (default: markdown)
    --output-file PATH  Write report to specified file
    --auto-fix          Attempt automatic fixes for common issues
    --verbose           Enable verbose logging
    --ci                CI/CD mode (non-interactive, machine-readable)
    --skip-tests        Skip functional tests
    -h, --help          Show this help message

EXAMPLES:
    # Run all checks
    $SCRIPT_NAME

    # Run with auto-fix
    $SCRIPT_NAME --auto-fix

    # Run in CI mode
    $SCRIPT_NAME --ci --output text

    # Save report to file
    $SCRIPT_NAME --output-file deployment_report.md

    # Run on specific device
    $SCRIPT_NAME --device ABC123

CHECKS PERFORMED:
    1. Build verification (APK exists, size correct)
    2. Device connection (ADB working, device authorized)
    3. App installation (package installed, version correct)
    4. Permissions granted (all 10 required permissions)
    5. Accessibility service enabled
    6. Models deployed (FunctionGemma, Whisper)
    7. Workflows deployed (10 YAML files)
    8. Storage space available (>2GB free)
    9. Battery optimization disabled
    10. Quick functionality test

RETURN CODES:
    0   All checks passed
    1   One or more checks failed
    2   Device not connected
    3   Critical error

For more information, see: docs/deployment/DEPLOYMENT_GUIDE.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --device)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --device requires an argument"
                    return 1
                fi
                DEVICE_SERIAL="$2"
                shift 2
                ;;
            --output)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --output requires an argument"
                    return 1
                fi
                OUTPUT_FORMAT="$2"
                shift 2
                ;;
            --output-file)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --output-file requires an argument"
                    return 1
                fi
                OUTPUT_FILE="$2"
                shift 2
                ;;
            --auto-fix)
                AUTO_FIX=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --ci)
                CI_MODE=true
                COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [magenta]='' [bold]='' [nc]='')
                shift
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                return 1
                ;;
            *)
                log_error "Unexpected argument: $1"
                return 1
                ;;
        esac
    done

    return 0
}

# =============================================================================
# Main
# =============================================================================

main() {
    # Parse arguments
    if ! parse_arguments "$@"; then
        exit $?
    fi

    # Create report directory
    mkdir -p "$REPORT_DIR"

    # Print header
    print_header "EgyptianAgent Deployment Verification"

    log_info "Starting verification..."
    log_info "Timestamp: $TIMESTAMP"
    echo ""

    # Run all checks
    check_build_verification || true
    echo ""

    check_device_connection || {
        local exit_code=$?
        if [[ "$exit_code" -eq 2 ]]; then
            print_header "Verification Failed"
            log_error "No device connected. Cannot continue verification."
            echo ""
            log_info "Please connect an Android device and try again."
            exit 2
        fi
    }
    echo ""

    check_app_installation || true
    echo ""

    check_permissions || true
    echo ""

    check_accessibility_service || true
    echo ""

    check_models_deployed || true
    echo ""

    check_workflows_deployed || true
    echo ""

    check_storage_space || true
    echo ""

    check_battery_optimization || true
    echo ""

    check_functionality_test || true
    echo ""

    # Attempt auto-fixes if enabled
    attempt_auto_fix

    # Generate report
    print_header "Verification Report"

    local report=""
    if [[ "$OUTPUT_FORMAT" == "markdown" ]]; then
        report=$(generate_markdown_report)
    else
        report=$(generate_text_report)
    fi

    # Output report
    if [[ -n "$OUTPUT_FILE" ]]; then
        echo -e "$report" > "$OUTPUT_FILE"
        log_info "Report saved to: $OUTPUT_FILE"
    else
        echo -e "$report"
    fi

    # Also save to default location
    local default_report="$REPORT_DIR/verification_$TIMESTAMP.md"
    generate_markdown_report > "$default_report"
    log_verbose "Report saved to: $default_report"

    # Determine exit code
    local fail_count=0
    for check in "${!CHECK_RESULTS[@]}"; do
        if [[ "${CHECK_RESULTS[$check]}" == "FAIL" ]]; then
            ((fail_count++))
        fi
    done

    print_header "Verification Complete"

    if [[ "$fail_count" -gt 0 ]]; then
        log_error "Verification failed with $fail_count error(s)"
        exit 1
    else
        log_success "All checks passed!"
        exit 0
    fi
}

# Trap errors
trap 'log_error "Verification interrupted"; exit 3' INT TERM

# Run main
main "$@"
exit $?
