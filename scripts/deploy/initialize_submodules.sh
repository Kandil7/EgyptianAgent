#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Git Submodules Initialization Script
# =============================================================================
#
# PURPOSE:
#   Initializes and updates git submodules for external dependencies including
#   llama.cpp for LLM inference and whisper.cpp for speech recognition.
#   Ensures all native library dependencies are properly configured.
#
# USAGE:
#   ./scripts/deploy/initialize_submodules.sh [OPTIONS]
#
# OPTIONS:
#   --force             Force re-initialization of existing submodules
#   --update            Update submodules to latest commits
#   --recursive         Initialize nested submodules recursively
#   --depth N           Clone depth for submodules (default: 1 for shallow)
#   --verbose           Enable verbose git output
#   --log-file PATH     Write initialization log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/deploy/initialize_submodules.sh
#   ./scripts/deploy/initialize_submodules.sh --force --update
#   ./scripts/deploy/initialize_submodules.sh --recursive --depth 10
#   ./scripts/deploy/initialize_submodules.sh --ci --log-file init.log
#
# SUBMODULES:
#   - llama.cpp    - LLM inference engine (https://github.com/ggerganov/llama.cpp)
#   - whisper.cpp  - Speech recognition engine (https://github.com/ggerganov/whisper.cpp)
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Git not available
#   3   Submodule initialization failed
#   4   Verification failed
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
readonly EXTERNAL_DIR="$PROJECT_DIR/external"
readonly INIT_TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# Default configuration
FORCE_INIT=false
UPDATE_SUBMODULES=false
RECURSIVE=false
CLONE_DEPTH=1
VERBOSE=false
LOG_FILE=""
CI_MODE=false

# Submodule definitions
declare -A SUBMODULES=(
    ["llama.cpp"]="https://github.com/ggerganov/llama.cpp.git"
    ["whisper.cpp"]="https://github.com/ggerganov/whisper.cpp.git"
)

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

get_commit_hash() {
    local dir="$1"
    if [[ -d "$dir/.git" ]]; then
        (cd "$dir" && git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    else
        echo "not-a-git-repo"
    fi
}

get_branch_name() {
    local dir="$1"
    if [[ -d "$dir/.git" ]]; then
        (cd "$dir" && git branch --show-current 2>/dev/null || echo "unknown")
    else
        echo "unknown"
    fi
}

# =============================================================================
# Prerequisite Checks
# =============================================================================

check_git() {
    log_step "Checking Git..."
    
    if ! command -v git &>/dev/null; then
        log_error "Git not found. Please install Git."
        log_error ""
        log_error "Installation:"
        echo "  - Windows: https://git-scm.com/download/win"
        echo "  - macOS:   brew install git"
        echo "  - Linux:   sudo apt install git"
        return 2
    fi
    
    log_info "Git: $(git --version)"
    return 0
}

check_project_git() {
    log_step "Checking project Git repository..."
    
    if [[ ! -d "$PROJECT_DIR/.git" ]]; then
        log_warn "Project is not a Git repository"
        log_warn "Will initialize submodules manually"
        return 0
    fi
    
    log_info "Project Git repository: found"
    return 0
}

# =============================================================================
# Submodule Functions
# =============================================================================

init_git_submodules() {
    if [[ ! -f "$PROJECT_DIR/.gitmodules" ]]; then
        log_warn ".gitmodules not found - using manual initialization"
        return 1
    fi
    
    log_step "Initializing Git submodules..."
    
    local git_flags=""
    if [[ "$RECURSIVE" == "true" ]]; then
        git_flags="--recursive"
    fi
    
    if [[ "$VERBOSE" == "true" ]]; then
        git submodule init $git_flags
        git submodule update --init $git_flags
    else
        git submodule init $git_flags 2>&1 | head -10
        git submodule update --init $git_flags 2>&1 | head -10
    fi
    
    log_success "Git submodules initialized"
    return 0
}

clone_submodule() {
    local name="$1"
    local url="$2"
    local dest_dir="$3"
    
    log_step "Cloning $name..."
    
    local clone_flags="--depth $CLONE_DEPTH"
    if [[ "$VERBOSE" == "true" ]]; then
        clone_flags=""
    fi
    
    if [[ -d "$dest_dir" ]]; then
        if [[ "$FORCE_INIT" == "true" ]]; then
            log_warn "Removing existing $name directory"
            rm -rf "$dest_dir"
        else
            log_info "$name already exists, updating..."
            (cd "$dest_dir" && git pull --rebase 2>/dev/null) || true
            return 0
        fi
    fi
    
    # Clone repository
    if ! git clone $clone_flags "$url" "$dest_dir"; then
        log_error "Failed to clone $name"
        return 3
    fi
    
    # Initialize nested submodules if recursive
    if [[ "$RECURSIVE" == "true" ]]; then
        log_info "Initializing nested submodules for $name..."
        (cd "$dest_dir" && git submodule update --init --recursive 2>/dev/null) || \
            log_warn "Some nested submodules failed to initialize"
    fi
    
    log_success "$name cloned successfully"
    return 0
}

update_submodule() {
    local name="$1"
    local dest_dir="$2"
    
    log_step "Updating $name..."
    
    if [[ ! -d "$dest_dir" ]]; then
        log_warn "$name directory not found, skipping update"
        return 0
    fi
    
    (cd "$dest_dir" && git pull --rebase 2>/dev/null) || {
        log_warn "Failed to update $name"
        return 0
    }
    
    local commit
    commit=$(get_commit_hash "$dest_dir")
    local branch
    branch=$(get_branch_name "$dest_dir")
    
    log_info "$name updated to $branch@$commit"
    return 0
}

verify_submodule() {
    local name="$1"
    local dest_dir="$2"
    local required_files="$3"
    
    log_step "Verifying $name..."
    
    if [[ ! -d "$dest_dir" ]]; then
        log_error "$name directory not found"
        return 4
    fi
    
    local errors=0
    
    # Check for CMakeLists.txt
    if [[ -f "$dest_dir/CMakeLists.txt" ]]; then
        log_info "  ✓ CMakeLists.txt found"
    else
        log_error "  ✗ CMakeLists.txt missing"
        ((errors++)) || true
    fi
    
    # Check required files
    for file in $required_files; do
        if [[ -f "$dest_dir/$file" ]]; then
            log_info "  ✓ $file found"
        else
            log_warn "  ✗ $file missing (optional)"
        fi
    done
    
    # Get commit info
    local commit
    commit=$(get_commit_hash "$dest_dir")
    local branch
    branch=$(get_branch_name "$dest_dir")
    log_info "  Branch: $branch, Commit: $commit"
    
    if [[ $errors -gt 0 ]]; then
        return 4
    fi
    
    log_success "$name verified"
    return 0
}

# =============================================================================
# Main Initialization
# =============================================================================

initialize_llama_cpp() {
    local llama_dir="$EXTERNAL_DIR/llama.cpp"
    
    if [[ "$UPDATE_SUBMODULES" == "true" ]]; then
        update_submodule "llama.cpp" "$llama_dir"
    else
        clone_submodule "llama.cpp" "${SUBMODULES[llama.cpp]}" "$llama_dir"
    fi
    
    verify_submodule "llama.cpp" "$llama_dir" "ggml.h llama.h"
}

initialize_whisper_cpp() {
    local whisper_dir="$EXTERNAL_DIR/whisper.cpp"
    
    if [[ "$UPDATE_SUBMODULES" == "true" ]]; then
        update_submodule "whisper.cpp" "${SUBMODULES[whisper.cpp]}" "$whisper_dir"
    else
        clone_submodule "whisper.cpp" "${SUBMODULES[whisper.cpp]}" "$whisper_dir"
    fi
    
    verify_submodule "whisper.cpp" "$whisper_dir" "whisper.h"
}

# =============================================================================
# Help and Usage
# =============================================================================

show_help() {
    cat << EOF
Egyptian Agent - Git Submodules Initialization Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --force             Force re-initialization of existing submodules
    --update            Update submodules to latest commits
    --recursive         Initialize nested submodules recursively
    --depth N           Clone depth for submodules (default: 1)
    --verbose           Enable verbose git output
    --log-file PATH     Write initialization log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    # Standard initialization
    $SCRIPT_NAME

    # Force re-initialization
    $SCRIPT_NAME --force

    # Update to latest commits
    $SCRIPT_NAME --update

    # Full recursive initialization
    $SCRIPT_NAME --recursive --depth 10

    # CI/CD initialization
    $SCRIPT_NAME --ci --log-file init.log

SUBMODULES:
    llama.cpp    - LLM inference engine
                   https://github.com/ggerganov/llama.cpp

    whisper.cpp  - Speech recognition engine
                   https://github.com/ggerganov/whisper.cpp

RETURN CODES:
    0   Success
    1   General error
    2   Git not available
    3   Submodule initialization failed
    4   Verification failed

For more information, see: docs/architecture/NATIVE_LIBRARIES.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --force)
                FORCE_INIT=true
                shift
                ;;
            --update)
                UPDATE_SUBMODULES=true
                shift
                ;;
            --recursive)
                RECURSIVE=true
                shift
                ;;
            --depth)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --depth requires an argument"
                    return 5
                fi
                CLONE_DEPTH="$2"
                shift 2
                ;;
            --verbose)
                VERBOSE=true
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
    print_header "Submodule Initialization"
    
    echo ""
    log_info "Initialization Configuration:"
    echo "  External Dir:  $EXTERNAL_DIR"
    echo "  Force Init:    $FORCE_INIT"
    echo "  Update:        $UPDATE_SUBMODULES"
    echo "  Recursive:     $RECURSIVE"
    echo "  Clone Depth:   $CLONE_DEPTH"
    echo ""
    
    # Check prerequisites
    if ! check_git; then
        exit $?
    fi
    
    check_project_git || true
    
    echo ""
    
    # Create external directory
    mkdir -p "$EXTERNAL_DIR"
    
    # Try Git submodules first
    init_git_submodules || true
    
    echo ""
    
    # Initialize llama.cpp
    local llama_errors=0
    if ! initialize_llama_cpp; then
        ((llama_errors++)) || true
    fi
    
    echo ""
    
    # Initialize whisper.cpp
    local whisper_errors=0
    if ! initialize_whisper_cpp; then
        ((whisper_errors++)) || true
    fi
    
    echo ""
    
    # Print summary
    print_header "Initialization Summary"
    
    local total_errors=$((llama_errors + whisper_errors))
    
    if [[ $total_errors -eq 0 ]]; then
        log_success "All submodules initialized successfully!"
        echo ""
        echo "Submodules:"
        echo "  ✓ llama.cpp   - $EXTERNAL_DIR/llama.cpp"
        echo "  ✓ whisper.cpp - $EXTERNAL_DIR/whisper.cpp"
        echo ""
        log_info "Next steps:"
        echo "  1. Build native libraries: ./scripts/build/build_native_libs.sh"
        echo "  2. Download AI models:     ./scripts/model/download_functiongemma_model.sh"
        echo "  3. Build application:      ./scripts/build/build.sh --native"
        echo ""
        return 0
    else
        log_error "Initialization completed with $total_errors error(s)"
        echo ""
        log_error "Troubleshooting:"
        echo "  - Check your internet connection"
        echo "  - Run: git submodule sync"
        echo "  - Run: git submodule update --init --recursive --force"
        echo "  - Check disk space availability"
        echo ""
        return 4
    fi
}

# Trap errors
trap 'log_error "Initialization interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
