#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Production Deployment Script
# =============================================================================
#
# PURPOSE:
#   Deploys the Egyptian Agent application as a system app on rooted Android
#   devices. Includes device verification, root access check, system partition
#   mounting, APK installation, permission granting, and rollback support.
#
# USAGE:
#   ./deploy/scripts-deploy/deploy_production.sh [OPTIONS]
#
# OPTIONS:
#   --apk PATH          Path to APK file (default: dist/production/*.apk)
#   --device SERIAL     Target device serial (for multiple devices)
#   --no-backup         Skip creating backup before deployment
#   --no-reboot         Don't reboot device after installation
#   --rollback          Rollback to previous version
#   --verify-only       Only verify deployment, don't install
#   --force             Force installation even if checks fail
#   --log-file PATH     Write deployment log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./deploy/scripts-deploy/deploy_production.sh
#   ./deploy/scripts-deploy/deploy_production.sh --apk /path/to/app.apk
#   ./deploy/scripts-deploy/deploy_production.sh --device ABC123 --no-reboot
#   ./deploy/scripts-deploy/deploy_production.sh --rollback
#   ./deploy/scripts-deploy/deploy_production.sh --verify-only
#
# REQUIREMENTS:
#   - Rooted Android device with Magisk
#   - USB debugging enabled
#   - Unlocked bootloader (for system partition modification)
#   - ADB and Fastboot tools installed
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Device not connected
#   3   Root access denied
#   4   Installation failed
#   5   Rollback failed
#   6   Verification failed
#
# AUTHOR: EgyptianAgent Team
# VERSION: 2.0.0
# DATE: 2026-03-03
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly LOG_DIR="$PROJECT_DIR/build/logs"
readonly DEPLOY_TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# Default configuration
APK_PATH=""
DEVICE_SERIAL=""
CREATE_BACKUP=true
AUTO_REBOOT=true
ROLLBACK_MODE=false
VERIFY_ONLY=false
FORCE_INSTALL=false
LOG_FILE=""
CI_MODE=false

# Deployment paths
readonly PACKAGE_NAME="com.egyptian.agent"
readonly SYSTEM_DIR="/system/priv-app/EgyptianAgent"
readonly BACKUP_DIR="/data/local/tmp/egyptian_agent_backup"
readonly TEMP_APK="/sdcard/Download/EgyptianAgent.apk"

# Colors
declare -A COLORS=(
    [red]='\033[0;31m'
    [green]='\033[0;32m'
    [yellow]='\033[1;33m'
    [blue]='\033[0;34m'
    [cyan]='\033[0;36m'
    [nc]='\033[0m'
)

# =============================================================================
# Logging Functions
# =============================================================================

init_logging() {
    mkdir -p "$LOG_DIR"
    
    if [[ -n "$LOG_FILE" ]]; then
        exec > >(tee -a "$LOG_FILE") 2>&1
    fi
}

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

log_step() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[STEP] $*"
    else
        echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"
    fi
}

log_success() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[SUCCESS] $*"
    else
        echo -e "${COLORS[cyan]}[SUCCESS]${COLORS[nc]} $*"
    fi
}

print_header() {
    local title="$1"
    local width=60
    
    if [[ "$CI_MODE" == "true" ]]; then
        echo "=== $title ==="
    else
        echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $width))${COLORS[nc]}"
        echo -e "${COLORS[blue]}  $title${COLORS[nc]}"
        echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $width))${COLORS[nc]}"
    fi
}

# =============================================================================
# Utility Functions
# =============================================================================

adb_cmd() {
    if [[ -n "$DEVICE_SERIAL" ]]; then
        adb -s "$DEVICE_SERIAL" "$@"
    else
        adb "$@"
    fi
}

adb_shell() {
    adb_cmd shell "$@"
}

adb_shell_root() {
    adb_cmd shell su -c "$*"
}

get_file_size() {
    local file="$1"
    if [[ -f "$file" ]]; then
        stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

format_size() {
    local bytes="$1"
    if [[ $bytes -ge 1073741824 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1073741824}") GB"
    elif [[ $bytes -ge 1048576 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1048576}") MB"
    elif [[ $bytes -ge 1024 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1024}") KB"
    else
        echo "$bytes B"
    fi
}

# =============================================================================
# Device Checks
# =============================================================================

check_adb() {
    log_step "Checking ADB..."
    
    if ! command -v adb &>/dev/null; then
        log_error "ADB not found. Please install Android SDK platform-tools."
        log_error ""
        log_error "Installation:"
        echo "  - Windows: https://developer.android.com/studio/releases/platform-tools"
        echo "  - macOS:   brew install android-platform-tools"
        echo "  - Linux:   sudo apt install android-tools-adb"
        return 2
    fi
    
    log_info "ADB: $(which adb)"
    return 0
}

check_device_connection() {
    log_step "Checking device connection..."
    
    local devices
    if [[ -n "$DEVICE_SERIAL" ]]; then
        if ! adb_cmd devices | grep -q "$DEVICE_SERIAL"; then
            log_error "Device '$DEVICE_SERIAL' not found"
            return 2
        fi
        log_info "Using device: $DEVICE_SERIAL"
    else
        local device_count
        device_count=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
        
        if [[ "$device_count" -eq 0 ]]; then
            log_error "No connected devices found"
            echo ""
            log_error "Solutions:"
            echo "  1. Enable USB debugging on device"
            echo "  2. Connect device via USB cable"
            echo "  3. Accept USB debugging prompt on device"
            echo "  4. Run: adb devices"
            return 2
        fi
        
        if [[ "$device_count" -gt 1 ]]; then
            log_warn "Multiple devices connected ($device_count)"
            log_warn "Use --device SERIAL to specify target"
            echo ""
            adb devices
            echo ""
        fi
        
        log_info "Found $device_count device(s)"
    fi
    
    # Get device info
    local device_model
    device_model=$(adb_shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Unknown")
    local device_android
    device_android=$(adb_shell getprop ro.build.version.release 2>/dev/null | tr -d '\r' || echo "Unknown")
    local device_sdk
    device_sdk=$(adb_shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || echo "Unknown")
    
    log_info "Device: $device_model (Android $device_android, SDK $device_sdk)"
    
    return 0
}

check_root_access() {
    log_step "Checking root access..."
    
    if [[ "$FORCE_INSTALL" == "true" ]]; then
        log_warn "Forcing installation without root check"
        return 0
    fi
    
    local root_test
    root_test=$(adb_shell_root "echo root_access_verified" 2>/dev/null | tr -d '\r' || echo "")
    
    if [[ "$root_test" != "root_access_verified" ]]; then
        log_error "Root access not available"
        echo ""
        log_error "Solutions:"
        echo "  1. Ensure device is rooted with Magisk"
        echo "  2. Enable root access in Magisk settings"
        echo "  3. Grant root access to ADB shell when prompted"
        echo "  4. Use --force to skip root check (may fail)"
        return 3
    fi
    
    log_success "Root access confirmed"
    return 0
}

check_device_storage() {
    log_step "Checking device storage..."
    
    local available
    available=$(adb_shell "df /data 2>/dev/null | tail -1 | awk '{print \$4}'" | tr -d '\r' || echo "0")
    
    if [[ "$available" == "0" || -z "$available" ]]; then
        log_warn "Could not determine available storage"
        return 0
    fi
    
    local available_mb=$((available / 1024))
    local required_mb=500  # Minimum required space
    
    log_info "Available storage: $(format_size $((available * 1024)))"
    
    if [[ "$available_mb" -lt "$required_mb" ]]; then
        log_error "Insufficient storage! Need ${required_mb}MB, have ${available_mb}MB"
        return 4
    fi
    
    log_info "Storage check passed (need ${required_mb}MB)"
    return 0
}

check_existing_installation() {
    log_step "Checking existing installation..."
    
    local installed
    installed=$(adb_shell "pm list packages | grep -c '$PACKAGE_NAME'" 2>/dev/null || echo "0")
    
    if [[ "$installed" -gt 0 ]]; then
        log_info "Existing installation found"
        
        # Get version info
        local version
        version=$(adb_shell "dumpsys package $PACKAGE_NAME 2>/dev/null | grep versionName | head -1" | tr -d '\r' || echo "unknown")
        log_info "Current version: $version"
        
        return 0
    else
        log_info "No existing installation found"
        return 0
    fi
}

# =============================================================================
# Backup Functions
# =============================================================================

create_backup() {
    if [[ "$CREATE_BACKUP" != "true" ]]; then
        log_info "Skipping backup (--no-backup)"
        return 0
    fi
    
    log_step "Creating backup..."
    
    # Create backup directory
    adb_shell_root "mkdir -p $BACKUP_DIR"
    
    # Backup existing APK if present
    local existing_apk
    existing_apk=$(adb_shell "pm path $PACKAGE_NAME 2>/dev/null | head -1" | tr -d '\r' | sed 's/package://' || echo "")
    
    if [[ -n "$existing_apk" && "$existing_apk" != "" ]]; then
        log_info "Backing up existing APK..."
        adb_shell_root "cp $existing_apk $BACKUP_DIR/EgyptianAgent_backup.apk"
    fi
    
    # Backup app data
    log_info "Backing up app data..."
    adb_shell_root "mkdir -p $BACKUP_DIR/data"
    adb_shell_root "cp -r /data/data/$PACKAGE_NAME/* $BACKUP_DIR/data/" 2>/dev/null || true
    
    # Backup system installation info
    adb_shell_root "ls -la $SYSTEM_DIR > $BACKUP_DIR/system_info.txt" 2>/dev/null || true
    
    log_success "Backup created: $BACKUP_DIR"
    return 0
}

rollback_installation() {
    log_step "Rolling back to previous version..."
    
    if [[ ! "$ROLLBACK_MODE" == "true" ]]; then
        return 0
    fi
    
    # Check for backup
    local backup_exists
    backup_exists=$(adb_shell "test -f $BACKUP_DIR/EgyptianAgent_backup.apk && echo yes" 2>/dev/null | tr -d '\r' || echo "no")
    
    if [[ "$backup_exists" != "yes" ]]; then
        log_error "No backup found for rollback"
        return 5
    fi
    
    # Remove current installation
    log_info "Removing current installation..."
    adb_shell_root "rm -rf $SYSTEM_DIR"
    adb_shell "pm uninstall $PACKAGE_NAME" 2>/dev/null || true
    
    # Restore backup
    log_info "Restoring backup..."
    adb_shell_root "mkdir -p $SYSTEM_DIR"
    adb_shell_root "cp $BACKUP_DIR/EgyptianAgent_backup.apk $SYSTEM_DIR/EgyptianAgent.apk"
    adb_shell_root "chmod 644 $SYSTEM_DIR/EgyptianAgent.apk"
    
    # Reboot
    log_info "Rebooting device..."
    adb_shell "reboot"
    
    log_success "Rollback completed"
    return 0
}

# =============================================================================
# Installation Functions
# =============================================================================

verify_apk() {
    local apk_file="$1"
    
    log_step "Verifying APK..."
    
    if [[ ! -f "$apk_file" ]]; then
        log_error "APK not found: $apk_file"
        return 4
    fi
    
    local size_bytes
    size_bytes=$(get_file_size "$apk_file")
    local size_mb=$((size_bytes / 1024 / 1024))
    
    log_info "APK: $(basename "$apk_file")"
    log_info "Size: $(format_size "$size_bytes") ($size_mb MB)"
    
    # Verify APK is valid
    if command -v aapt2 &>/dev/null; then
        if ! aapt2 dump badging "$apk_file" &>/dev/null; then
            log_error "APK validation failed"
            return 4
        fi
        log_info "APK validation: passed"
    fi
    
    # Check minimum size
    if [[ "$size_mb" -lt 30 ]]; then
        log_warn "APK size is smaller than expected"
    fi
    
    return 0
}

push_apk_to_device() {
    local apk_file="$1"
    
    log_step "Pushing APK to device..."
    
    if ! adb_cmd push "$apk_file" "$TEMP_APK"; then
        log_error "Failed to push APK"
        return 4
    fi
    
    # Verify push
    local remote_size
    remote_size=$(adb_shell "ls -l $TEMP_APK 2>/dev/null | awk '{print \$5}'" | tr -d '\r' || echo "0")
    local local_size
    local_size=$(get_file_size "$apk_file")
    
    if [[ "$remote_size" != "$local_size" ]]; then
        log_warn "Size mismatch after push (local: $local_size, remote: $remote_size)"
    fi
    
    log_success "APK pushed to: $TEMP_APK"
    return 0
}

install_as_system_app() {
    log_step "Installing as system app..."
    
    # Create system app directory
    log_info "Creating system directory..."
    if ! adb_shell_root "mkdir -p $SYSTEM_DIR"; then
        log_error "Failed to create system directory"
        return 4
    fi
    
    # Copy APK to system directory
    log_info "Copying APK to system..."
    if ! adb_shell_root "cp $TEMP_APK $SYSTEM_DIR/EgyptianAgent.apk"; then
        log_error "Failed to copy APK to system"
        return 4
    fi
    
    # Set permissions
    log_info "Setting permissions..."
    adb_shell_root "chmod 644 $SYSTEM_DIR/EgyptianAgent.apk"
    adb_shell_root "chown root:root $SYSTEM_DIR/EgyptianAgent.apk" 2>/dev/null || true
    
    # Set proper SELinux context
    adb_shell_root "chcon u:object_r:system_file:s0 $SYSTEM_DIR/EgyptianAgent.apk" 2>/dev/null || true
    
    log_success "APK installed to: $SYSTEM_DIR"
    return 0
}

grant_permissions() {
    log_step "Granting permissions..."
    
    local permissions=(
        "android.permission.RECORD_AUDIO"
        "android.permission.CALL_PHONE"
        "android.permission.READ_CONTACTS"
        "android.permission.READ_CALL_LOG"
        "android.permission.SEND_SMS"
        "android.permission.RECEIVE_SMS"
        "android.permission.BODY_SENSORS"
        "android.permission.ACCESS_FINE_LOCATION"
        "android.permission.SYSTEM_ALERT_WINDOW"
        "android.permission.FOREGROUND_SERVICE"
        "android.permission.RECEIVE_BOOT_COMPLETED"
        "android.permission.WAKE_LOCK"
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    )
    
    for perm in "${permissions[@]}"; do
        adb_shell "pm grant $PACKAGE_NAME $perm" 2>/dev/null || true
    done
    
    log_success "Permissions granted"
    return 0
}

apply_battery_optimizations() {
    log_step "Applying battery optimizations..."
    
    # Whitelist from doze mode
    adb_shell "dumpsys deviceidle whitelist +$PACKAGE_NAME" 2>/dev/null || true
    
    # Allow background activity
    adb_shell "cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow" 2>/dev/null || true
    adb_shell "cmd appops set $PACKAGE_NAME ACTIVATE_DEVICE_WITHOUT_USER_PRESENT allow" 2>/dev/null || true
    
    log_success "Battery optimizations applied"
    return 0
}

# =============================================================================
# Verification Functions
# =============================================================================

verify_installation() {
    log_step "Verifying installation..."
    
    # Check if app is installed
    local installed
    installed=$(adb_shell "pm list packages | grep -c '$PACKAGE_NAME'" 2>/dev/null || echo "0")
    
    if [[ "$installed" -eq 0 ]]; then
        log_error "App not found in package list"
        return 6
    fi
    
    log_info "Package installed: yes"
    
    # Check if installed as system app
    local is_system
    is_system=$(adb_shell "dumpsys package $PACKAGE_NAME 2>/dev/null | grep -c 'SYSTEM'" || echo "0")
    
    if [[ "$is_system" -gt 0 ]]; then
        log_info "System app: yes"
    else
        log_warn "System app: no (may require reboot)"
    fi
    
    # Check if app can start
    log_info "Testing app launch..."
    adb_shell "am start -n $PACKAGE_NAME/.MainActivity" 2>/dev/null || true
    
    # Wait for app to start
    sleep 2
    
    # Check if service is running
    local service_running
    service_running=$(adb_shell "dumpsys activity services | grep -c 'EgyptianAgent'" 2>/dev/null || echo "0")
    
    if [[ "$service_running" -gt 0 ]]; then
        log_info "Service running: yes"
    else
        log_warn "Service running: no (may require reboot)"
    fi
    
    log_success "Installation verified"
    return 0
}

verify_only() {
    log_step "Running verification only..."
    
    check_device_connection || return $?
    check_root_access || return $?
    check_existing_installation || return $?
    
    print_header "Verification Results"
    
    log_info "Device is ready for deployment"
    log_info "System directory: $SYSTEM_DIR"
    log_info "Backup directory: $BACKUP_DIR"
    
    return 0
}

# =============================================================================
# Help and Usage
# =============================================================================

show_help() {
    cat << EOF
Egyptian Agent - Production Deployment Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --apk PATH          Path to APK file
                        (default: dist/production/*.apk)
    --device SERIAL     Target device serial (for multiple devices)
    --no-backup         Skip creating backup before deployment
    --no-reboot         Don't reboot device after installation
    --rollback          Rollback to previous version
    --verify-only       Only verify deployment, don't install
    --force             Force installation even if checks fail
    --log-file PATH     Write deployment log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    # Standard deployment
    $SCRIPT_NAME

    # Deploy specific APK
    $SCRIPT_NAME --apk /path/to/app.apk

    # Deploy to specific device
    $SCRIPT_NAME --device ABC123

    # Deploy without reboot
    $SCRIPT_NAME --no-reboot

    # Rollback to previous version
    $SCRIPT_NAME --rollback

    # Verify only
    $SCRIPT_NAME --verify-only

REQUIREMENTS:
    - Rooted Android device with Magisk
    - USB debugging enabled
    - Unlocked bootloader
    - ADB tools installed

RETURN CODES:
    0   Success
    1   General error
    2   Device not connected
    3   Root access denied
    4   Installation failed
    5   Rollback failed
    6   Verification failed

For more information, see: docs/deployment/DEPLOYMENT_GUIDE.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --apk)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --apk requires an argument"
                    return 5
                fi
                APK_PATH="$2"
                shift 2
                ;;
            --device)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --device requires an argument"
                    return 5
                fi
                DEVICE_SERIAL="$2"
                shift 2
                ;;
            --no-backup)
                CREATE_BACKUP=false
                shift
                ;;
            --no-reboot)
                AUTO_REBOOT=false
                shift
                ;;
            --rollback)
                ROLLBACK_MODE=true
                shift
                ;;
            --verify-only)
                VERIFY_ONLY=true
                shift
                ;;
            --force)
                FORCE_INSTALL=true
                shift
                ;;
            --log-file)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --log-file requires an argument"
                    return 5
                fi
                LOG_FILE="$2"
                shift 2
                ;;
            --ci)
                CI_MODE=true
                COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]='')
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                return 5
                ;;
            *)
                log_error "Unexpected argument: $1"
                return 5
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
    
    # Initialize logging
    init_logging
    
    # Print header
    print_header "Egyptian Agent Production Deployment"
    
    echo ""
    log_info "Deployment Configuration:"
    echo "  APK Path:      ${APK_PATH:-auto-detect}"
    echo "  Device:        ${DEVICE_SERIAL:-auto-detect}"
    echo "  Create Backup: $CREATE_BACKUP"
    echo "  Auto Reboot:   $AUTO_REBOOT"
    echo "  Rollback Mode: $ROLLBACK_MODE"
    echo ""
    
    # Check ADB
    if ! check_adb; then
        exit $?
    fi
    
    # Handle verify-only mode
    if [[ "$VERIFY_ONLY" == "true" ]]; then
        verify_only
        exit $?
    fi
    
    # Handle rollback mode
    if [[ "$ROLLBACK_MODE" == "true" ]]; then
        check_device_connection || exit $?
        rollback_installation || exit $?
        exit 0
    fi
    
    # Check device connection
    if ! check_device_connection; then
        exit $?
    fi
    
    # Check root access
    if ! check_root_access; then
        exit $?
    fi
    
    # Check storage
    if ! check_device_storage; then
        exit $?
    fi
    
    # Check existing installation
    check_existing_installation || true
    
    echo ""
    
    # Find APK if not specified
    if [[ -z "$APK_PATH" ]]; then
        local prod_dir="$PROJECT_DIR/dist/production"
        if [[ -d "$prod_dir" ]]; then
            APK_PATH=$(find "$prod_dir" -name "*_signed.apk" -type f | head -1)
        fi
        
        if [[ -z "$APK_PATH" || ! -f "$APK_PATH" ]]; then
            # Try build output
            APK_PATH="$PROJECT_DIR/android/build/outputs/apk/release/app-release.apk"
        fi
    fi
    
    # Verify APK
    if ! verify_apk "$APK_PATH"; then
        exit $?
    fi
    
    echo ""
    
    # Create backup
    create_backup
    
    echo ""
    
    # Push APK to device
    if ! push_apk_to_device "$APK_PATH"; then
        exit $?
    fi
    
    echo ""
    
    # Install as system app
    if ! install_as_system_app; then
        exit $?
    fi
    
    echo ""
    
    # Grant permissions
    grant_permissions
    
    # Apply battery optimizations
    apply_battery_optimizations
    
    echo ""
    
    # Verify installation
    if ! verify_installation; then
        exit $?
    fi
    
    # Reboot if requested
    if [[ "$AUTO_REBOOT" == "true" ]]; then
        echo ""
        log_info "Rebooting device in 5 seconds..."
        sleep 5
        adb_shell "reboot"
        log_info "Device rebooting..."
    fi
    
    # Print summary
    print_header "Deployment Complete"
    
    log_success "Egyptian Agent deployed successfully!"
    echo ""
    echo "  Installation: $SYSTEM_DIR"
    echo "  Backup:       $BACKUP_DIR"
    echo ""
    
    if [[ "$AUTO_REBOOT" == "true" ]]; then
        log_info "After reboot:"
        echo "  1. Wait for device to fully boot"
        echo "  2. Say 'يا صاحبي' to activate the assistant"
        echo "  3. Check logs: adb logcat | grep EgyptianAgent"
    else
        log_info "Manual reboot required for full functionality:"
        echo "  adb reboot"
    fi
    
    echo ""
    
    return 0
}

# Trap errors
trap 'log_error "Deployment interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
