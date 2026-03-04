#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Implementation Verification Script
# =============================================================================
#
# PURPOSE:
#   Verifies the complete Egyptian Agent implementation including source files,
#   model assets, build configuration, and deployment readiness.
#
# USAGE:
#   ./scripts/utils/verify_implementation.sh [OPTIONS]
#
# OPTIONS:
#   --quick             Quick verification (skip model checks)
#   --full              Full verification including models
#   --output FILE       Write report to file
#   --ci                CI/CD mode (machine-readable output)
#   -h, --help          Show this help message
#
# CHECKS PERFORMED:
#   - Project structure
#   - Source files (Java/Kotlin/C++)
#   - Build configuration
#   - Model assets
#   - Native libraries
#   - Permissions and manifest
#
# RETURN CODES:
#   0   All checks passed
#   1   General error
#   2   Critical files missing
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

QUICK_MODE=false
FULL_MODE=false
OUTPUT_FILE=""
CI_MODE=false

declare -A COLORS=([red]='\033[0;31m' [green]='\033[0;32m' [yellow]='\033[1;33m' [blue]='\033[0;34m' [cyan]='\033[0;36m' [nc]='\033[0m')

CHECKS_PASSED=0
CHECKS_FAILED=0
CHECKS_WARNING=0

log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { [[ "$CI_MODE" == "true" ]] && echo "[STEP] $*" || echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"; }
print_header() { local w=60; [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || { echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; echo -e "${COLORS[blue]}  $1${COLORS[nc]}"; echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; }; }

check_pass() { ((CHECKS_PASSED++)); [[ "$CI_MODE" == "true" ]] && echo "[PASS] $1" || echo -e "${COLORS[green]}✓${COLORS[nc]} $1"; }
check_fail() { ((CHECKS_FAILED++)); [[ "$CI_MODE" == "true" ]] && echo "[FAIL] $1" || echo -e "${COLORS[red]}✗${COLORS[nc]} $1"; }
check_warn() { ((CHECKS_WARNING++)); [[ "$CI_MODE" == "true" ]] && echo "[WARN] $1" || echo -e "${COLORS[yellow]}!${COLORS[nc]} $1"; }

check_file() {
    [[ -f "$1" ]] && { check_pass "$2"; return 0; } || { check_fail "$2"; return 1; }
}

check_dir() {
    [[ -d "$1" ]] && { check_pass "$2"; return 0; } || { check_fail "$2"; return 1; }
}

check_project_structure() {
    log_step "Checking project structure..."
    
    check_dir "$PROJECT_DIR/app" "app directory"
    check_dir "$PROJECT_DIR/app/src/main/java" "Java source directory"
    check_dir "$PROJECT_DIR/app/src/main/cpp" "Native source directory"
    check_dir "$PROJECT_DIR/app/src/main/assets" "Assets directory"
    check_dir "$PROJECT_DIR/scripts" "Scripts directory"
}

check_source_files() {
    log_step "Checking source files..."
    
    # Core Java files
    check_file "$PROJECT_DIR/app/src/main/java/com/egyptian/agent/core/VoiceService.java" "VoiceService.java"
    check_file "$PROJECT_DIR/app/src/main/java/com/egyptian/agent/ai/LlamaIntentEngine.java" "LlamaIntentEngine.java"
    check_file "$PROJECT_DIR/app/src/main/java/com/egyptian/agent/hybrid/HybridOrchestrator.java" "HybridOrchestrator.java"
    
    # Native files
    check_file "$PROJECT_DIR/app/src/main/cpp/llama_native.cpp" "llama_native.cpp"
    check_file "$PROJECT_DIR/app/CMakeLists.txt" "CMakeLists.txt"
}

check_build_config() {
    log_step "Checking build configuration..."
    
    check_file "$PROJECT_DIR/build.gradle" "Root build.gradle"
    check_file "$PROJECT_DIR/app/build.gradle" "App build.gradle"
    check_file "$PROJECT_DIR/settings.gradle" "settings.gradle"
    check_file "$PROJECT_DIR/gradlew" "Gradle wrapper"
}

check_manifest() {
    log_step "Checking AndroidManifest.xml..."
    
    local manifest="$PROJECT_DIR/app/src/main/AndroidManifest.xml"
    if [[ -f "$manifest" ]]; then
        check_pass "AndroidManifest.xml exists"
        
        # Check for required permissions
        grep -q "RECORD_AUDIO" "$manifest" && check_pass "RECORD_AUDIO permission" || check_warn "RECORD_AUDIO permission missing"
        grep -q "CALL_PHONE" "$manifest" && check_pass "CALL_PHONE permission" || check_warn "CALL_PHONE permission missing"
    else
        check_fail "AndroidManifest.xml"
    fi
}

check_models() {
    [[ "$QUICK_MODE" == "true" ]] && { log_info "Skipping model checks (quick mode)"; return 0; }
    
    log_step "Checking model assets..."
    
    # Check for model directories
    check_dir "$PROJECT_DIR/app/src/main/assets/models" "Models directory"
    
    # Check for model files (warn if missing, don't fail)
    if [[ -f "$PROJECT_DIR/app/src/main/assets/models/functiongemma-270m-it-Q4_K_M.gguf" ]]; then
        check_pass "FunctionGemma model"
    else
        check_warn "FunctionGemma model not found (download with download_functiongemma_model.sh)"
    fi
}

check_scripts() {
    log_step "Checking scripts..."
    
    check_file "$PROJECT_DIR/scripts/build/build.sh" "build.sh"
    check_file "$PROJECT_DIR/scripts/deploy/deploy_production.sh" "deploy_production.sh"
    check_file "$PROJECT_DIR/scripts/model/download_functiongemma_model.sh" "download_functiongemma_model.sh"
}

generate_report() {
    local total=$((CHECKS_PASSED + CHECKS_FAILED + CHECKS_WARNING))
    
    echo ""
    print_header "Verification Summary"
    echo "  Total Checks:  $total"
    echo "  Passed:        $CHECKS_PASSED"
    echo "  Failed:        $CHECKS_FAILED"
    echo "  Warnings:      $CHECKS_WARNING"
    echo ""
    
    if [[ $CHECKS_FAILED -eq 0 ]]; then
        log_success "All critical checks passed!"
        
        if [[ $CHECKS_WARNING -gt 0 ]]; then
            log_warn "$CHECKS_WARNING non-critical issue(s) found"
            return 3
        fi
        return 0
    else
        log_error "$CHECKS_FAILED critical check(s) failed"
        echo ""
        log_info "Run the following to fix issues:"
        echo "  1. ./scripts/deploy/initialize_submodules.sh"
        echo "  2. ./scripts/model/download_functiongemma_model.sh"
        echo "  3. ./scripts/build/build.sh --release"
        return 2
    fi
}

show_help() {
    cat << EOF
Egyptian Agent - Implementation Verification Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --quick             Quick verification (skip model checks)
    --full              Full verification including models
    --output FILE       Write report to file
    --ci                CI/CD mode (machine-readable)
    -h, --help          Show help

RETURN CODES:
    0   All checks passed
    1   General error
    2   Critical files missing
    3   Non-critical issues found
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --quick) QUICK_MODE=true; shift;;
            --full) FULL_MODE=true; shift;;
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
    
    print_header "Egyptian Agent Implementation Verification"
    echo "Project: $PROJECT_DIR"
    echo "Date: $(date)"
    echo ""
    
    # Verify project root
    if [[ ! -f "$PROJECT_DIR/build.gradle" ]]; then
        log_error "Not in project root directory"
        return 1
    fi
    
    check_project_structure
    echo ""
    check_source_files
    echo ""
    check_build_config
    echo ""
    check_manifest
    echo ""
    check_models
    echo ""
    check_scripts
    
    generate_report
}

main "$@"
