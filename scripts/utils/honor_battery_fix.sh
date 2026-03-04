#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Honor Battery Optimization Fix
# =============================================================================
#
# PURPOSE:
#   Applies battery optimization exemptions for the Egyptian Agent on
#   Honor/Huawei devices to ensure background services work properly.
#
# USAGE:
#   ./scripts/utils/honor_battery_fix.sh [OPTIONS]
#
# OPTIONS:
#   --apply             Apply battery fixes
#   --verify            Verify current battery settings
#   --reset             Reset to default settings
#   --package NAME      Package name (default: com.egyptian.agent)
#   --log-file PATH     Write log to specified file
#   --ci                CI/CD mode
#   -h, --help          Show this help message
#
# REQUIREMENTS:
#   - Rooted device with Magisk
#   - ADB connection
#   - Honor/Huawei device (recommended)
#
# FIXES APPLIED:
#   - Doze mode whitelist
#   - Background activity allowance
#   - Wake lock permissions
#   - Protected apps configuration
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Root access denied
#   3   Device not connected
#
# AUTHOR: EgyptianAgent Team
# VERSION: 2.0.0
# DATE: 2026-03-03
# =============================================================================

set -euo pipefail

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ACTION="apply"
PACKAGE_NAME="com.egyptian.agent"
LOG_FILE=""
CI_MODE=false
DEVICE_SERIAL=""

declare -A COLORS=([red]='\033[0;31m' [green]='\033[0;32m' [yellow]='\033[1;33m' [blue]='\033[0;34m' [cyan]='\033[0;36m' [nc]='\033[0m')

log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { [[ "$CI_MODE" == "true" ]] && echo "[STEP] $*" || echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"; }
print_header() { local w=60; [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || { echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; echo -e "${COLORS[blue]}  $1${COLORS[nc]}"; echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; }; }

adb_cmd() { [[ -n "$DEVICE_SERIAL" ]] && adb -s "$DEVICE_SERIAL" "$@" || adb "$@"; }
adb_shell() { adb_cmd shell "$@"; }
adb_shell_root() { adb_cmd shell su -c "$*"; }

check_adb() {
    command -v adb &>/dev/null || { log_error "ADB not found"; return 3; }
    
    local count=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
    [[ "$count" -eq 0 ]] && { log_error "No device connected"; return 3; }
    
    log_info "Device connected"
}

check_root() {
    local root_test=$(adb_shell_root "echo root_verified" 2>/dev/null | tr -d '\r' || echo "")
    [[ "$root_test" == "root_verified" ]] && { log_info "Root access confirmed"; return 0; }
    
    log_error "Root access not available"
    return 2
}

apply_battery_fixes() {
    log_step "Applying battery optimization exemptions..."
    
    # Whitelist from doze mode
    log_info "Adding to doze whitelist..."
    adb_shell "dumpsys deviceidle whitelist +$PACKAGE_NAME" 2>/dev/null && \
        log_info "Doze whitelist: applied" || log_warn "Doze whitelist: failed"
    
    # Allow background activity
    log_info "Allowing background activity..."
    adb_shell "cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow" 2>/dev/null && \
        log_info "Background activity: allowed" || log_warn "Background activity: failed"
    
    # Allow wake without user present
    adb_shell "cmd appops set $PACKAGE_NAME ACTIVATE_DEVICE_WITHOUT_USER_PRESENT allow" 2>/dev/null || true
    
    # Allow wake lock
    adb_shell "cmd appops set $PACKAGE_NAME WAKE_LOCK allow" 2>/dev/null || true
    
    # Honor-specific: Protected apps
    log_info "Configuring protected apps..."
    adb_shell_root "settings put global device_idle_constants \"inactive_timeout=3600000,light_idle_timeout=3600000\"" 2>/dev/null || \
        log_warn "Protected apps config: failed (may not be needed)"
    
    # Disable battery optimization
    log_info "Disabling battery optimization..."
    adb_shell "pm grant $PACKAGE_NAME android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" 2>/dev/null || true
    
    log_success "Battery fixes applied"
}

verify_settings() {
    log_step "Verifying battery settings..."
    
    # Check doze whitelist
    local whitelist=$(adb_shell "dumpsys deviceidle | grep -A5 'Whitelist'" 2>/dev/null || echo "")
    if echo "$whitelist" | grep -q "$PACKAGE_NAME"; then
        log_info "Doze whitelist: enabled"
    else
        log_warn "Doze whitelist: not enabled"
    fi
    
    # Check app ops
    local bg_allowed=$(adb_shell "appops get $PACKAGE_NAME RUN_IN_BACKGROUND" 2>/dev/null || echo "")
    if echo "$bg_allowed" | grep -q "allow"; then
        log_info "Background activity: allowed"
    else
        log_warn "Background activity: restricted"
    fi
    
    # Check battery optimization
    local optimization=$(adb_shell "dumpsys deviceidle | grep -i \"$PACKAGE_NAME\"" 2>/dev/null || echo "")
    if [[ -n "$optimization" ]]; then
        log_info "Battery optimization: configured"
    else
        log_warn "Battery optimization: not configured"
    fi
}

reset_settings() {
    log_step "Resetting battery settings..."
    
    # Remove from doze whitelist
    adb_shell "dumpsys deviceidle whitelist -$PACKAGE_NAME" 2>/dev/null || true
    
    # Reset app ops to default
    adb_shell "cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND default" 2>/dev/null || true
    adb_shell "cmd appops set $PACKAGE_NAME WAKE_LOCK default" 2>/dev/null || true
    
    log_info "Settings reset to default"
}

show_help() {
    cat << EOF
Egyptian Agent - Honor Battery Optimization Fix

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --apply             Apply battery fixes (default)
    --verify            Verify current settings
    --reset             Reset to default settings
    --package NAME      Package name (default: com.egyptian.agent)
    --device SERIAL     Target device serial
    --log-file PATH     Write log to file
    --ci                CI/CD mode
    -h, --help          Show help

REQUIREMENTS:
    - Rooted device with Magisk
    - ADB connection

RETURN CODES:
    0   Success
    1   General error
    2   Root access denied
    3   Device not connected
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --apply) ACTION="apply"; shift;;
            --verify) ACTION="verify"; shift;;
            --reset) ACTION="reset"; shift;;
            --package) PACKAGE_NAME="$2"; shift 2;;
            --device) DEVICE_SERIAL="$2"; shift 2;;
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
    
    print_header "Honor Battery Optimization Fix"
    log_info "Package: $PACKAGE_NAME"
    log_info "Action: $ACTION"
    echo ""
    
    check_adb || exit $?
    
    case "$ACTION" in
        apply)
            check_root || exit $?
            apply_battery_fixes
            ;;
        verify)
            verify_settings
            ;;
        reset)
            check_root || exit $?
            reset_settings
            ;;
    esac
    
    echo ""
    log_info "Reboot recommended for changes to take effect"
    log_info "Run: adb reboot"
}

main "$@"
