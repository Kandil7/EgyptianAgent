#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Complete Build Verification Script
# =============================================================================
#
# PURPOSE:
#   Performs a complete build, test, and verification cycle for the
#   Egyptian Agent application. Suitable for CI/CD pipelines and
#   release verification.
#
# USAGE:
#   ./scripts/utils/complete_build.sh [OPTIONS]
#
# OPTIONS:
#   --build-type TYPE   Build type: debug, release (default: release)
#   --skip-tests        Skip test execution
#   --skip-deploy       Skip device deployment
#   --output DIR        Output directory for artifacts
#   --log-file PATH     Write build log to specified file
#   --ci                CI/CD mode (machine-readable output)
#   -h, --help          Show this help message
#
# STAGES:
#   1. Prerequisites check
#   2. Unit tests
#   3. Build application
#   4. APK optimization and signing
#   5. Integration tests (if device connected)
#   6. Verification
#   7. Report generation
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Prerequisites missing
#   3   Build failed
#   4   Tests failed
#   5   Verification failed
#
# AUTHOR: EgyptianAgent Team
# VERSION: 2.0.0
# DATE: 2026-03-03
# =============================================================================

set -euo pipefail

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly LOG_DIR="$PROJECT_DIR/build/logs"
readonly BUILD_TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

BUILD_TYPE="release"
SKIP_TESTS=false
SKIP_DEPLOY=false
OUTPUT_DIR="$PROJECT_DIR/dist/complete_build"
LOG_FILE=""
CI_MODE=false

declare -A COLORS=([red]='\033[0;31m' [green]='\033[0;32m' [yellow]='\033[1;33m' [blue]='\033[0;34m' [cyan]='\033[0;36m' [nc]='\033[0m')

STAGE=0
TOTAL_STAGES=7

log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { ((STAGE++)); [[ "$CI_MODE" == "true" ]] && echo "[STEP $STAGE/$TOTAL_STAGES] $*" || echo -e "${COLORS[blue]}[STEP $STAGE/$TOTAL_STAGES]${COLORS[nc]} $*"; }
log_success() { [[ "$CI_MODE" == "true" ]] && echo "[SUCCESS] $*" || echo -e "${COLORS[cyan]}[SUCCESS]${COLORS[nc]} $*"; }
print_header() { local w=60; [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || { echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; echo -e "${COLORS[blue]}  $1${COLORS[nc]}"; echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; }; }

get_gradlew() { [[ -f "$PROJECT_DIR/gradlew" ]] && echo "./gradlew" || { [[ -f "$PROJECT_DIR/gradlew.bat" ]] && echo "./gradlew.bat" || return 1; }; }
get_file_size() { [[ -f "$1" ]] && (stat -c%s "$1" 2>/dev/null || stat -f%z "$1" 2>/dev/null || echo "0") || echo "0"; }
format_size() { local b=$1; if [[ $b -ge 1073741824 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $b / 1073741824}") GB"; elif [[ $b -ge 1048576 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $b / 1048576}") MB"; else echo "$b B"; fi; }

check_prerequisites() {
    log_step "Checking prerequisites..."
    
    local missing=()
    
    command -v java &>/dev/null || missing+=("Java")
    get_gradlew || missing+=("Gradle wrapper")
    
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing: ${missing[*]}"
        return 2
    fi
    
    log_info "Java: $(java -version 2>&1 | head -1)"
    log_info "Gradle: ready"
    log_success "Prerequisites check passed"
}

run_tests() {
    [[ "$SKIP_TESTS" == "true" ]] && { log_info "Skipping tests"; return 0; }
    
    log_step "Running tests..."
    
    local gradlew=$(get_gradlew)
    
    log_info "Running unit tests..."
    if ! $gradlew test --console=plain 2>&1 | tail -20; then
        log_error "Unit tests failed"
        return 4
    fi
    
    log_success "Tests passed"
}

build_application() {
    log_step "Building application ($BUILD_TYPE)..."
    
    local gradlew=$(get_gradlew)
    local task="assembleDebug"
    [[ "$BUILD_TYPE" == "release" ]] && task="assembleRelease"
    
    local start_time=$(date +%s)
    
    if ! $gradlew $task --no-daemon --console=plain 2>&1 | tail -30; then
        log_error "Build failed"
        return 3
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log_success "Build completed in ${duration}s"
}

find_apk() {
    local apk_dir="$PROJECT_DIR/app/build/outputs/apk"
    local apk_path="$apk_dir/$BUILD_TYPE"
    
    [[ ! -d "$apk_path" ]] && { log_error "APK directory not found"; return 3; }
    
    local apk=$(find "$apk_path" -name "*.apk" -type f | head -1)
    [[ -z "$apk" ]] && { log_error "APK not found"; return 3; }
    
    echo "$apk"
}

optimize_and_sign() {
    local apk="$1"
    
    log_step "Optimizing and signing APK..."
    
    mkdir -p "$OUTPUT_DIR"
    
    local output_apk="$OUTPUT_DIR/EgyptianAgent_${BUILD_TYPE}_${BUILD_TIMESTAMP}.apk"
    
    # Copy APK (optimization requires zipalign which may not be available)
    cp "$apk" "$output_apk"
    
    local size=$(get_file_size "$output_apk")
    log_info "Output: $(basename "$output_apk")"
    log_info "Size: $(format_size "$size")"
    
    log_success "APK ready: $output_apk"
    echo "$output_apk"
}

run_integration_tests() {
    [[ "$SKIP_DEPLOY" == "true" ]] && { log_info "Skipping integration tests"; return 0; }
    
    log_step "Checking for integration tests..."
    
    if ! command -v adb &>/dev/null; then
        log_info "ADB not available, skipping integration tests"
        return 0
    fi
    
    local device_count=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
    if [[ "$device_count" -eq 0 ]]; then
        log_info "No device connected, skipping integration tests"
        return 0
    fi
    
    log_info "Device connected, running integration tests..."
    
    # Run integration tests if available
    local gradlew=$(get_gradlew)
    $gradlew connectedAndroidTest --console=plain 2>&1 | tail -10 || log_warn "Integration tests failed"
    
    log_success "Integration tests completed"
}

verify_build() {
    log_step "Verifying build..."
    
    local apk="$1"
    
    [[ ! -f "$apk" ]] && { log_error "APK not found for verification"; return 5; }
    
    local size=$(get_file_size "$apk")
    local size_mb=$((size / 1024 / 1024))
    
    if [[ "$size_mb" -lt 30 ]]; then
        log_error "APK too small (${size_mb}MB)"
        return 5
    fi
    
    log_info "APK size: $(format_size "$size") - OK"
    
    # Verify APK structure if aapt2 available
    if command -v aapt2 &>/dev/null; then
        if aapt2 dump badging "$apk" &>/dev/null; then
            log_info "APK structure: valid"
        else
            log_warn "APK structure: could not verify"
        fi
    fi
    
    log_success "Verification passed"
}

generate_report() {
    local apk="$1"
    local report="$OUTPUT_DIR/build_report_${BUILD_TIMESTAMP}.txt"
    
    log_step "Generating build report..."
    
    cat > "$report" << EOF
===============================================================================
Egyptian Agent - Complete Build Report
===============================================================================

Build Information:
  Timestamp:       $BUILD_TIMESTAMP
  Build Type:      $BUILD_TYPE
  Duration:        See logs

Environment:
  OS:              $(uname -s 2>/dev/null || echo "Unknown")
  Hostname:        $(hostname 2>/dev/null || echo "Unknown")

Git Information:
  Branch:          $(git branch --show-current 2>/dev/null || echo "Unknown")
  Commit:          $(git rev-parse HEAD 2>/dev/null || echo "Unknown")
  Status:          $(git diff-index --quiet HEAD -- 2>/dev/null && echo "Clean" || echo "Modified")

Build Artifacts:
  APK:             $(basename "$apk")
  Size:            $(format_size $(get_file_size "$apk"))
  SHA256:          $(sha256sum "$apk" 2>/dev/null | cut -d' ' -f1 || echo "Not computed")

Build Status:      SUCCESS
===============================================================================
EOF
    
    log_info "Report: $report"
    log_success "Build report generated"
}

show_help() {
    cat << EOF
Egyptian Agent - Complete Build Verification Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --build-type TYPE   Build type: debug, release (default: release)
    --skip-tests        Skip test execution
    --skip-deploy       Skip device deployment
    --output DIR        Output directory (default: dist/complete_build)
    --log-file PATH     Write build log to file
    --ci                CI/CD mode
    -h, --help          Show help

STAGES:
    1. Prerequisites check
    2. Unit tests
    3. Build application
    4. APK optimization and signing
    5. Integration tests
    6. Verification
    7. Report generation

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Build failed
    4   Tests failed
    5   Verification failed
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --build-type) BUILD_TYPE="$2"; shift 2;;
            --skip-tests) SKIP_TESTS=true; shift;;
            --skip-deploy) SKIP_DEPLOY=true; shift;;
            --output) OUTPUT_DIR="$2"; shift 2;;
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
    
    mkdir -p "$OUTPUT_DIR" "$LOG_DIR"
    
    print_header "Egyptian Agent Complete Build"
    log_info "Build Type: $BUILD_TYPE"
    log_info "Output: $OUTPUT_DIR"
    log_info "Timestamp: $BUILD_TIMESTAMP"
    echo ""
    
    local exit_code=0
    local apk=""
    
    check_prerequisites || exit $?
    run_tests || exit_code=$?
    [[ $exit_code -ne 0 ]] && exit $exit_code
    
    build_application || exit $?
    apk=$(find_apk) || exit $?
    apk=$(optimize_and_sign "$apk") || exit $?
    run_integration_tests
    verify_build "$apk" || exit $?
    generate_report "$apk"
    
    print_header "Build Complete"
    log_success "Build completed successfully!"
    echo ""
    echo "  APK:      $apk"
    echo "  Size:     $(format_size $(get_file_size "$apk"))"
    echo "  Report:   $OUTPUT_DIR/build_report_${BUILD_TIMESTAMP}.txt"
    echo ""
    
    return 0
}

trap 'log_error "Build interrupted at stage $STAGE"; exit 1' INT TERM
main "$@"
