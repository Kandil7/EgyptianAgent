#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Test Execution Script
# =============================================================================
#
# PURPOSE:
#   Runs comprehensive tests for the FunctionGemma Egyptian Agent including
#   unit tests, integration tests, and generates coverage reports.
#
# USAGE:
#   ./scripts/test/run_functiongemma_tests.sh [OPTIONS]
#
# OPTIONS:
#   --all               Run all tests (unit + integration)
#   --unit              Run unit tests only
#   --integration       Run integration tests only (requires device)
#   --coverage          Generate code coverage report
#   --class NAME        Run specific test class
#   --category CAT      Run tests by category
#   --output DIR        Output directory for reports (default: build/reports)
#   --log-file PATH     Write test log to specified file
#   --ci                CI/CD mode (JUnit XML output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/test/run_functiongemma_tests.sh --all
#   ./scripts/test/run_functiongemma_tests.sh --unit --coverage
#   ./scripts/test/run_functiongemma_tests.sh --class FunctionGemmaEngineTest
#   ./scripts/test/run_functiongemma_tests.sh --ci --output test-results
#
# TEST CATEGORIES:
#   CALL_CONTACT        Contact calling tests
#   SEND_WHATSAPP       WhatsApp messaging tests
#   SET_ALARM           Alarm setting tests
#   EMERGENCY           Emergency response tests
#   DEVICE_CONTROL      Device control tests
#   EGYPTIAN_DIALECT    Egyptian dialect understanding tests
#
# RETURN CODES:
#   0   All tests passed
#   1   General error
#   2   Prerequisites missing
#   3   Tests failed
#   4   Device not connected (for integration tests)
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

RUN_ALL=false
RUN_UNIT=false
RUN_INTEGRATION=false
RUN_COVERAGE=false
TEST_CLASS=""
TEST_CATEGORY=""
OUTPUT_DIR="$PROJECT_DIR/build/reports"
LOG_FILE=""
CI_MODE=false

declare -A COLORS=([red]='\033[0;31m' [green]='\033[0;32m' [yellow]='\033[1;33m' [blue]='\033[0;34m' [cyan]='\033[0;36m' [nc]='\033[0m')

init_logging() { mkdir -p "$LOG_DIR" "$OUTPUT_DIR"; [[ -n "$LOG_FILE" ]] && exec > >(tee -a "$LOG_FILE") 2>&1; }
log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { [[ "$CI_MODE" == "true" ]] && echo "[STEP] $*" || echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"; }
log_success() { [[ "$CI_MODE" == "true" ]] && echo "[SUCCESS] $*" || echo -e "${COLORS[cyan]}[SUCCESS]${COLORS[nc]} $*"; }
print_header() { local w=60; [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || { echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; echo -e "${COLORS[blue]}  $1${COLORS[nc]}"; echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; }; }

get_gradlew() { [[ -f "$PROJECT_DIR/gradlew" ]] && echo "./gradlew" || { [[ -f "$PROJECT_DIR/gradlew.bat" ]] && echo "./gradlew.bat" || return 1; }; }

check_prerequisites() {
    log_step "Checking prerequisites..."
    
    command -v java &>/dev/null || { log_error "Java required"; return 2; }
    get_gradlew || { log_error "Gradle wrapper not found"; return 2; }
    
    log_info "Prerequisites check passed"
}

check_device() {
    if ! command -v adb &>/dev/null; then
        log_error "ADB required for integration tests"
        return 4
    fi
    
    local count=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
    [[ "$count" -eq 0 ]] && { log_error "No device connected"; return 4; }
    
    log_info "Device connected: $count device(s)"
}

run_unit_tests() {
    log_step "Running unit tests..."
    
    local gradlew=$(get_gradlew)
    local args="testDebugUnitTest"
    
    [[ -n "$TEST_CLASS" ]] && args="$args --tests $TEST_CLASS"
    [[ -n "$TEST_CATEGORY" ]] && args="$args --tests *${TEST_CATEGORY}*"
    
    if ! $gradlew $args --console=plain; then
        log_error "Unit tests failed"
        return 3
    fi
    
    log_success "Unit tests passed"
}

run_integration_tests() {
    log_step "Running integration tests..."
    
    check_device || return $?
    
    local gradlew=$(get_gradlew)
    
    if ! $gradlew connectedAndroidTest --console=plain; then
        log_error "Integration tests failed"
        return 3
    fi
    
    log_success "Integration tests passed"
}

generate_coverage() {
    log_step "Generating coverage report..."
    
    local gradlew=$(get_gradlew)
    $gradlew testDebugUnitTest jacocoTestReport --console=plain || true
    
    local report="$OUTPUT_DIR/coverage/html/index.html"
    if [[ -f "$report" ]]; then
        log_success "Coverage report: $report"
    else
        log_warn "Coverage report not generated"
    fi
}

generate_summary() {
    log_step "Generating test summary..."
    
    local results_dir="$PROJECT_DIR/build/test-results"
    local passed=0
    local failed=0
    local total=0
    
    if [[ -d "$results_dir" ]]; then
        for xml in "$results_dir"/*.xml; do
            [[ -f "$xml" ]] || continue
            local f=$(grep -o 'failures="[0-9]*"' "$xml" 2>/dev/null | grep -o '[0-9]*' || echo "0")
            local t=$(grep -o 'tests="[0-9]*"' "$xml" 2>/dev/null | grep -o '[0-9]*' || echo "0")
            failed=$((failed + f))
            total=$((total + t))
        done
        passed=$((total - failed))
    fi
    
    echo ""
    print_header "Test Summary"
    echo "  Total Tests:  $total"
    echo "  Passed:       $passed"
    echo "  Failed:       $failed"
    echo ""
    
    if [[ $failed -eq 0 && $total -gt 0 ]]; then
        log_success "All tests passed!"
        return 0
    elif [[ $total -eq 0 ]]; then
        log_warn "No test results found"
        return 0
    else
        log_error "$failed test(s) failed"
        return 3
    fi
}

show_help() {
    cat << EOF
Egyptian Agent - Test Execution Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --all               Run all tests (unit + integration)
    --unit              Run unit tests only
    --integration       Run integration tests only
    --coverage          Generate coverage report
    --class NAME        Run specific test class
    --category CAT      Run tests by category
    --output DIR        Output directory (default: build/reports)
    --log-file PATH     Write log to file
    --ci                CI/CD mode (JUnit XML)
    -h, --help          Show help

TEST CATEGORIES:
    CALL_CONTACT, SEND_WHATSAPP, SET_ALARM, EMERGENCY,
    DEVICE_CONTROL, EGYPTIAN_DIALECT

RETURN CODES:
    0   All tests passed
    1   General error
    2   Prerequisites missing
    3   Tests failed
    4   Device not connected
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --all) RUN_ALL=true; RUN_UNIT=true; RUN_INTEGRATION=true; shift;;
            --unit) RUN_UNIT=true; shift;;
            --integration) RUN_INTEGRATION=true; shift;;
            --coverage) RUN_COVERAGE=true; shift;;
            --class) TEST_CLASS="$2"; shift 2;;
            --category) TEST_CATEGORY="$2"; shift 2;;
            --output) OUTPUT_DIR="$2"; shift 2;;
            --log-file) LOG_FILE="$2"; shift 2;;
            --ci) CI_MODE=true; COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]=''); shift;;
            -h|--help) show_help; exit 0;;
            -*) log_error "Unknown option: $1"; exit 5;;
            *) log_error "Unexpected argument: $1"; exit 5;;
        esac
    done
    
    [[ "$RUN_ALL" == "false" && "$RUN_UNIT" == "false" && "$RUN_INTEGRATION" == "false" && "$RUN_COVERAGE" == "false" ]] && {
        show_help
        exit 0
    }
}

main() {
    parse_arguments
    init_logging
    print_header "FunctionGemma Test Suite"
    
    log_info "Date: $(date)"
    log_info "Output: $OUTPUT_DIR"
    
    check_prerequisites || exit $?
    
    local exit_code=0
    
    if [[ "$RUN_UNIT" == "true" ]]; then
        run_unit_tests || exit_code=$?
    fi
    
    if [[ "$RUN_INTEGRATION" == "true" ]]; then
        run_integration_tests || exit_code=$?
    fi
    
    if [[ "$RUN_COVERAGE" == "true" ]]; then
        generate_coverage
    fi
    
    generate_summary || exit_code=$?
    
    exit $exit_code
}

trap 'log_error "Interrupted"; exit 1' INT TERM
main "$@"
