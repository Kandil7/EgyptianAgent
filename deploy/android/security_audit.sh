#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Security Audit Script
# =============================================================================
#
# PURPOSE:
#   Performs automated security checks on the Egyptian Agent codebase
#   including network access, permissions, and security configurations.
#
# USAGE:
#   ./deploy/android/security_audit.sh [OPTIONS]
#
# OPTIONS:
#   --full              Full audit including dependency scanning
#   --quick             Quick audit (critical checks only)
#   --output FILE       Write report to file
#   --ci                CI/CD mode (machine-readable output)
#   -h, --help          Show this help message
#
# CHECKS PERFORMED:
#   - Network access verification
#   - Cleartext traffic configuration
#   - Dangerous permissions audit
#   - Security component presence
#   - Emergency rate limiting
#   - Command sanitization
#   - Memory leak detection
#
# RETURN CODES:
#   0   All security checks passed
#   1   General error
#   2   Critical security issues found
#   3   Non-critical issues found
#
# AUTHOR: EgyptianAgent Team
# VERSION: 2.0.0
# DATE: 2026-03-03
# =============================================================================

set -euo pipefail

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly APP_SRC="$PROJECT_DIR/android/src/main"

FULL_AUDIT=false
QUICK_AUDIT=false
OUTPUT_FILE=""
CI_MODE=false

declare -A COLORS=([red]='\033[0;31m' [green]='\033[0;32m' [yellow]='\033[1;33m' [blue]='\033[0;34m' [cyan]='\033[0;36m' [nc]='\033[0m')

ISSUES_CRITICAL=0
ISSUES_WARNING=0

log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { [[ "$CI_MODE" == "true" ]] && echo "[STEP] $*" || echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"; }
print_header() { local w=60; [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || { echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; echo -e "${COLORS[blue]}  $1${COLORS[nc]}"; echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; }; }

pass() { echo -e "${COLORS[green]}✓${COLORS[nc]} $1"; }
fail() { echo -e "${COLORS[red]}✗${COLORS[nc]} $1"; ((ISSUES_CRITICAL++)); }
warn() { echo -e "${COLORS[yellow]}!${COLORS[nc]} $1"; ((ISSUES_WARNING++)); }

check_network_calls() {
    log_step "Checking for network calls..."
    
    local network_files=$(grep -r "okhttp3\|HttpURLConnection\|HttpClient" "$APP_SRC/java/" \
        --include="*.java" --include="*.kt" 2>/dev/null | \
        grep -v "/test/" | grep -v "CommandSanitizer" | grep -v "DataEncryptionManager" || true)
    
    if [[ -n "$network_files" ]]; then
        fail "Network calls found in production code"
        echo "  $network_files" | head -5
    else
        pass "No unauthorized network calls"
    fi
}

check_cleartext_traffic() {
    log_step "Checking cleartext traffic configuration..."
    
    local manifest="$APP_SRC/AndroidManifest.xml"
    
    if grep -q 'usesCleartextTraffic="true"' "$manifest" 2>/dev/null; then
        fail "Cleartext traffic is enabled"
    else
        pass "Cleartext traffic is disabled"
    fi
}

check_network_security_config() {
    log_step "Checking network security config..."
    
    local config="$APP_SRC/res/xml/network_security_config.xml"
    
    if [[ -f "$config" ]]; then
        if grep -q 'cleartextTrafficPermitted="false"' "$config" 2>/dev/null; then
            pass "Network security config properly configured"
        else
            warn "Network security config may allow cleartext traffic"
        fi
    else
        warn "Network security config file missing"
    fi
}

check_dangerous_permissions() {
    log_step "Checking for dangerous permissions..."
    
    local manifest="$APP_SRC/AndroidManifest.xml"
    local dangerous_perms=("QUERY_ALL_PACKAGES" "PACKAGE_USAGE_STATS" "DEVICE_POWER")
    
    for perm in "${dangerous_perms[@]}"; do
        if grep -q "$perm" "$manifest" 2>/dev/null; then
            fail "Dangerous permission found: $perm"
        else
            pass "Dangerous permission removed: $perm"
        fi
    done
    
    # Check INTERNET permission
    if grep -q 'android.permission.INTERNET' "$manifest" 2>/dev/null; then
        warn "INTERNET permission present (verify if needed)"
    else
        pass "INTERNET permission removed"
    fi
}

check_security_components() {
    log_step "Checking security components..."
    
    local components=(
        "java/com/egyptian/agent/security/CommandSanitizer.java:CommandSanitizer"
        "java/com/egyptian/agent/security/DataEncryptionManager.java:DataEncryptionManager"
        "xml/voice_interaction_service.xml:Voice interaction service"
    )
    
    for component in "${components[@]}"; do
        local file="${component%%:*}"
        local name="${component##*:}"
        
        if [[ -f "$APP_SRC/$file" ]]; then
            pass "$name present"
        else
            warn "$name missing"
        fi
    done
}

check_emergency_rate_limiting() {
    log_step "Checking emergency rate limiting..."
    
    local handler="$APP_SRC/java/com/egyptian/agent/executors/EmergencyHandler.java"
    
    if [[ -f "$handler" ]] && grep -q "EMERGENCY_COOLDOWN_MS\|rateLimit\|cooldown" "$handler" 2>/dev/null; then
        pass "Emergency rate limiting implemented"
    else
        warn "Emergency rate limiting not found"
    fi
}

check_command_sanitization() {
    log_step "Checking command sanitization..."
    
    local sanitizer="$APP_SRC/java/com/egyptian/agent/security/CommandSanitizer.java"
    
    if [[ -f "$sanitizer" ]] && grep -q "ALLOWED_COMMANDS\|sanitize\|whitelist" "$sanitizer" 2>/dev/null; then
        pass "Command sanitization implemented"
    else
        warn "Command sanitization not found"
    fi
}

check_memory_leaks() {
    log_step "Checking for memory leak fixes..."
    
    local voice_service="$APP_SRC/java/com/egyptian/agent/core/VoiceService.java"
    
    if [[ -f "$voice_service" ]]; then
        if grep -q "removeCallbacksAndMessages\|unregisterReceiver" "$voice_service" 2>/dev/null; then
            pass "Handler memory leak prevention found"
        else
            warn "Handler cleanup may be missing"
        fi
        
        if grep -q "wakeLock.*=.*null\|release()" "$voice_service" 2>/dev/null; then
            pass "WakeLock cleanup found"
        else
            warn "WakeLock cleanup may be missing"
        fi
    else
        warn "VoiceService.java not found"
    fi
}

run_dependency_scan() {
    if [[ "$FULL_AUDIT" != "true" ]]; then
        return 0
    fi
    
    log_step "Scanning dependencies..."
    
    # Check for known vulnerable dependencies
    local vulnerabilities=0
    
    # This would integrate with dependency-check or similar tools
    log_info "Dependency scan requires external tools"
    log_info "Run: ./gradlew dependencyCheckAnalyze"
}

generate_report() {
    local total=$((ISSUES_CRITICAL + ISSUES_WARNING))
    
    echo ""
    print_header "Security Audit Summary"
    echo "  Critical Issues: $ISSUES_CRITICAL"
    echo "  Warnings:        $ISSUES_WARNING"
    echo ""
    
    if [[ $ISSUES_CRITICAL -eq 0 ]]; then
        if [[ $ISSUES_WARNING -gt 0 ]]; then
            log_warn "Audit passed with $ISSUES_WARNING warning(s)"
            return 3
        else
            log_success "All security checks passed!"
            return 0
        fi
    else
        log_error "Audit failed: $ISSUES_CRITICAL critical issue(s)"
        echo ""
        log_info "Remediation:"
        echo "  1. Review and fix critical issues above"
        echo "  2. Run: ./gradlew lint for additional checks"
        echo "  3. Consult security team for guidance"
        return 2
    fi
}

show_help() {
    cat << EOF
Egyptian Agent - Security Audit Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --full              Full audit including dependency scan
    --quick             Quick audit (critical checks only)
    --output FILE       Write report to file
    --ci                CI/CD mode (machine-readable)
    -h, --help          Show help

CHECKS:
    - Network access verification
    - Cleartext traffic configuration
    - Dangerous permissions audit
    - Security component presence
    - Emergency rate limiting
    - Command sanitization
    - Memory leak detection

RETURN CODES:
    0   All checks passed
    1   General error
    2   Critical issues found
    3   Non-critical issues found
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --full) FULL_AUDIT=true; shift;;
            --quick) QUICK_AUDIT=true; shift;;
            --output) OUTPUT_FILE="$2"; shift 2;;
            --ci) CI_MODE=true; COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]=''); shift;;
            -h|--help) show_help; exit 0;;
            -*) log_error "Unknown option: $1"; exit 1;;
            *) log_error "Unexpected argument: $1"; exit 1;;
        esac
    done
}

main() {
    parse_arguments
    
    [[ -n "$OUTPUT_FILE" ]] && exec > >(tee -a "$OUTPUT_FILE") 2>&1
    
    print_header "Egyptian Agent Security Audit"
    echo "Project: $PROJECT_DIR"
    echo "Date: $(date)"
    echo ""
    
    # Verify project
    if [[ ! -d "$APP_SRC" ]]; then
        log_error "Android source directory not found"
        return 1
    fi
    
    check_network_calls
    echo ""
    
    if [[ "$QUICK_AUDIT" != "true" ]]; then
        check_cleartext_traffic
        check_network_security_config
        echo ""
        check_dangerous_permissions
        echo ""
        check_security_components
        echo ""
        check_emergency_rate_limiting
        check_command_sanitization
        check_memory_leaks
        echo ""
        run_dependency_scan
    else
        check_cleartext_traffic
        check_dangerous_permissions
    fi
    
    generate_report
}

main "$@"
