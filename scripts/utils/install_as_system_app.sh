#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - System App Installer
# =============================================================================
#
# PURPOSE:
#   Installs the Egyptian Agent as a system app on rooted Android devices
#   for enhanced permissions and background operation.
#
# USAGE:
#   ./scripts/utils/install_as_system_app.sh [OPTIONS]
#
# OPTIONS:
#   --apk PATH          Path to APK file (required)
#   --device SERIAL     Target device serial
#   --no-reboot         Don't reboot after installation
#   --backup            Create backup before installation
#   --uninstall         Remove system app installation
#   --log-file PATH     Write installation log to specified file
#   --ci                CI/CD mode
#   -h, --help          Show this help message
#
# REQUIREMENTS:
#   - Rooted Android device with Magisk
#   - Unlocked bootloader
#   - ADB connection
#   - System partition write access
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Root access denied
#   3   Device not connected
#   4   Installation failed
#
# AUTHOR: EgyptianAgent Team
# VERSION: 2.0.0
# DATE: 2026-03-03
# =============================================================================

set -euo pipefail

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

APK_PATH=""
DEVICE_SERIAL=""
NO_REBOOT=false
CREATE_BACKUP=false
UNINSTALL_MODE=false
LOG_FILE=""
CI_MODE=false

readonly PACKAGE_NAME="com.egyptian.agent"
readonly SYSTEM_DIR="/system/priv-app/EgyptianAgent"
readonly BACKUP_DIR="/data/local/tmp/egyptian_agent_backup"
readonly TEMP_APK="/sdcard/Download/EgyptianAgent.apk"

declare -A COLORS=([red]='\033[0;31m' [green]='\033[0;32m' [yellow]='\033[1;33m' [blue]='\033[0;34m' [cyan]='\033[0;36m' [nc]='\033[0m')

log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { [[ "$CI_MODE" == "true" ]] && echo "[STEP] $*" || echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"; }
log_success() { [[ "$CI_MODE" == "true" ]] && echo "[SUCCESS] $*" || echo -e "${COLORS[cyan]}[SUCCESS]${COLORS[nc]} $*"; }
print_header() { local w=60; [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || { echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; echo -e "${COLORS[blue]}  $1${COLORS[nc]}"; echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; }; }

adb_cmd() { [[ -n "$DEVICE_SERIAL" ]] && adb -s "$DEVICE_SERIAL" "$@" || adb "$@"; }
adb_shell() { adb_cmd shell "$@"; }
adb_shell_root() { adb_cmd shell su -c "$*"; }

check_adb() {
    command -v adb &>/dev/null || { log_error "ADB not found"; return 3; }
    
    local count=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
    [[ "$count" -eq 0 ]] && { log_error "No device connected"; return 3; }
    
    local model=$(adb_shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Unknown")
    log_info "Device: $model"
}

check_root() {
    local root_test=$(adb_shell_root "echo root_verified" 2>/dev/null | tr -d '\r' || echo "")
    [[ "$root_test" == "root_verified" ]] && { log_info "Root access confirmed"; return 0; }
    
    log_error "Root access not available"
    return 2
}

check_apk() {
    [[ -z "$APK_PATH" ]] && { log_error "APK path required (--apk)"; return 1; }
    [[ ! -f "$APK_PATH" ]] && { log_error "APK not found: $APK_PATH"; return 1; }
    
    log_info "APK: $APK_PATH ($(du -h "$APK_PATH" 2>/dev/null | cut -f1 || echo "unknown"))"
}

remount_system() {
    log_step "Remounting system partition..."
    
    if ! adb_shell_root "mount -o remount,rw /system" 2>/dev/null; then
        # Try alternative mount points
        adb_shell_root "mount -o remount,rw /vendor" 2>/dev/null && \
            log_info "Mounted /vendor" || \
            adb_shell_root "mount -o remount,rw /product" 2>/dev/null && \
            log_info "Mounted /product" || \
            { log_error "Failed to remount system partition"; return 4; }
    fi
    
    log_info "System partition: writable"
}

create_backup() {
    [[ "$CREATE_BACKUP" != "true" ]] && return 0
    
    log_step "Creating backup..."
    
    adb_shell_root "mkdir -p $BACKUP_DIR"
    
    # Backup existing installation
    local existing=$(adb_shell "pm path $PACKAGE_NAME" 2>/dev/null | head -1 | sed 's/package://')
    if [[ -n "$existing" ]]; then
        adb_shell_root "cp $existing $BACKUP_DIR/" 2>/dev/null || true
        log_info "APK backed up"
    fi
    
    # Backup system installation
    if adb_shell_root "test -d $SYSTEM_DIR" 2>/dev/null; then
        adb_shell_root "cp -r $SYSTEM_DIR $BACKUP_DIR/system_backup" 2>/dev/null || true
        log_info "System installation backed up"
    fi
    
    log_success "Backup created: $BACKUP_DIR"
}

install_system_app() {
    log_step "Installing as system app..."
    
    # Push APK to device
    log_info "Pushing APK..."
    adb_cmd push "$APK_PATH" "$TEMP_APK" || { log_error "Failed to push APK"; return 4; }
    
    # Create system directory
    log_info "Creating system directory..."
    adb_shell_root "mkdir -p $SYSTEM_DIR" || { log_error "Failed to create directory"; return 4; }
    
    # Copy APK to system
    log_info "Copying to system..."
    adb_shell_root "cp $TEMP_APK $SYSTEM_DIR/EgyptianAgent.apk" || { log_error "Failed to copy APK"; return 4; }
    
    # Set permissions
    log_info "Setting permissions..."
    adb_shell_root "chmod 644 $SYSTEM_DIR/EgyptianAgent.apk"
    adb_shell_root "chown root:root $SYSTEM_DIR/EgyptianAgent.apk" 2>/dev/null || true
    
    # Set SELinux context
    adb_shell_root "chcon u:object_r:system_file:s0 $SYSTEM_DIR/EgyptianAgent.apk" 2>/dev/null || true
    
    # Clean up temp
    adb_shell "rm -f $TEMP_APK" 2>/dev/null || true
    
    log_success "System app installed: $SYSTEM_DIR"
}

grant_permissions() {
    log_step "Granting permissions..."
    
    local perms=(
        "android.permission.RECORD_AUDIO"
        "android.permission.CALL_PHONE"
        "android.permission.READ_CONTACTS"
        "android.permission.READ_CALL_LOG"
        "android.permission.SEND_SMS"
        "android.permission.BODY_SENSORS"
        "android.permission.SYSTEM_ALERT_WINDOW"
        "android.permission.FOREGROUND_SERVICE"
        "android.permission.RECEIVE_BOOT_COMPLETED"
        "android.permission.WAKE_LOCK"
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    )
    
    for perm in "${perms[@]}"; do
        adb_shell "pm grant $PACKAGE_NAME $perm" 2>/dev/null || true
    done
    
    log_success "Permissions granted"
}

apply_battery_exemptions() {
    log_step "Applying battery exemptions..."
    
    adb_shell "dumpsys deviceidle whitelist +$PACKAGE_NAME" 2>/dev/null || true
    adb_shell "cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow" 2>/dev/null || true
    
    log_success "Battery exemptions applied"
}

uninstall_system_app() {
    log_step "Uninstalling system app..."
    
    # Remove from system
    adb_shell_root "rm -rf $SYSTEM_DIR" 2>/dev/null || true
    
    # Uninstall package
    adb_cmd uninstall "$PACKAGE_NAME" 2>/dev/null || true
    
    log_success "System app uninstalled"
}

reboot_device() {
    [[ "$NO_REBOOT" == "true" ]] && { log_info "Skipping reboot"; return 0; }
    
    log_info "Rebooting device in 5 seconds..."
    sleep 5
    adb_cmd reboot
    log_info "Device rebooting..."
}

show_help() {
    cat << EOF
Egyptian Agent - System App Installer

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --apk PATH          Path to APK file (required)
    --device SERIAL     Target device serial
    --no-reboot         Don't reboot after installation
    --backup            Create backup before installation
    --uninstall         Remove system app installation
    --log-file PATH     Write log to file
    --ci                CI/CD mode
    -h, --help          Show help

REQUIREMENTS:
    - Rooted Android device with Magisk
    - Unlocked bootloader
    - ADB connection

RETURN CODES:
    0   Success
    1   General error
    2   Root access denied
    3   Device not connected
    4   Installation failed
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --apk) APK_PATH="$2"; shift 2;;
            --device) DEVICE_SERIAL="$2"; shift 2;;
            --no-reboot) NO_REBOOT=true; shift;;
            --backup) CREATE_BACKUP=true; shift;;
            --uninstall) UNINSTALL_MODE=true; shift;;
            --log-file) LOG_FILE="$2"; shift 2;;
            --ci) CI_MODE=true; COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]=''); shift;;
            -h|--help) show_help; exit 0;;
            -*) log_error "Unknown option: $1"; exit 1;;
            *) log_error "Unexpected argument: $1"; exit 1;;
        esac
    done
}

main() {
    parse_arguments
    
    [[ -n "$LOG_FILE" ]] && exec > >(tee -a "$LOG_FILE") 2>&1
    
    print_header "Egyptian Agent System App Installer"
    
    check_adb || exit $?
    check_root || exit $?
    
    if [[ "$UNINSTALL_MODE" == "true" ]]; then
        uninstall_system_app
        reboot_device
        exit 0
    fi
    
    check_apk || exit $?
    create_backup
    remount_system || exit $?
    install_system_app || exit $?
    grant_permissions
    apply_battery_exemptions
    reboot_device
    
    print_header "Installation Complete"
    log_success "Egyptian Agent installed as system app"
    log_info "After reboot, say 'يا صاحبي' to activate"
}

main "$@"
