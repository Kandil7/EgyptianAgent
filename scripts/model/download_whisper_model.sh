#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Whisper Model Download Script
# =============================================================================
#
# PURPOSE:
#   Downloads Whisper ASR models for Egyptian Arabic speech recognition.
#   Supports multiple model sizes and sources with integrity verification.
#
# USAGE:
#   ./scripts/model/download_whisper_model.sh [OPTIONS]
#
# OPTIONS:
#   --size SIZE         Model size: tiny, base, small, medium, large (default: base)
#   --output DIR        Output directory (default: app/src/main/assets/models/)
#   --source SOURCE     Download source: huggingface, original (default: huggingface)
#   --resume            Resume interrupted download
#   --verify PATH       Verify an existing model file
#   --info              Show model information
#   --log-file PATH     Write download log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/model/download_whisper_model.sh
#   ./scripts/model/download_whisper_model.sh --size small
#   ./scripts/model/download_whisper_model.sh --verify ggml-base.bin
#   ./scripts/model/download_whisper_model.sh --info
#
# MODEL SIZES:
#   tiny   - ~75MB, fastest, lowest accuracy
#   base   - ~142MB, fast, good accuracy (recommended)
#   small  - ~466MB, slower, better accuracy
#   medium - ~1.5GB, slow, high accuracy
#   large  - ~3.1GB, slowest, highest accuracy
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Download failed
#   3   Verification failed
#   4   Model file not found
#   5   Invalid arguments
#
# AUTHOR: EgyptianAgent Team
# VERSION: 2.0.0
# DATE: 2026-03-03
# =============================================================================

set -euo pipefail

# Configuration
readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly LOG_DIR="$PROJECT_DIR/build/logs"

# Defaults
MODEL_SIZE="base"
OUTPUT_DIR="$PROJECT_DIR/app/src/main/assets/models"
DOWNLOAD_SOURCE="huggingface"
RESUME_DOWNLOAD=false
VERIFY_MODEL=""
SHOW_INFO=false
LOG_FILE=""
CI_MODE=false

# Model configurations
declare -A MODEL_FILES=(
    ["tiny"]="ggml-tiny.bin"
    ["base"]="ggml-base.bin"
    ["small"]="ggml-small.bin"
    ["medium"]="ggml-medium.bin"
    ["large"]="ggml-large-v3.bin"
)

declare -A MODEL_SIZES_MB=(
    ["tiny"]=75
    ["base"]=142
    ["small"]=466
    ["medium"]=1500
    ["large"]=3100
)

declare -A MODEL_URLS=(
    ["huggingface"]="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
    ["original"]="https://raw.githubusercontent.com/ggerganov/whisper.cpp/master/models/"
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

# Logging
init_logging() {
    mkdir -p "$LOG_DIR" "$OUTPUT_DIR"
    [[ -n "$LOG_FILE" ]] && exec > >(tee -a "$LOG_FILE") 2>&1
}

log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { [[ "$CI_MODE" == "true" ]] && echo "[STEP] $*" || echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"; }
log_success() { [[ "$CI_MODE" == "true" ]] && echo "[SUCCESS] $*" || echo -e "${COLORS[cyan]}[SUCCESS]${COLORS[nc]} $*"; }

print_header() {
    local width=60
    [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || {
        echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $width))${COLORS[nc]}"
        echo -e "${COLORS[blue]}  $1${COLORS[nc]}"
        echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $width))${COLORS[nc]}"
    }
}

get_file_size() {
    [[ -f "$1" ]] && (stat -c%s "$1" 2>/dev/null || stat -f%z "$1" 2>/dev/null || echo "0") || echo "0"
}

format_size() {
    local bytes=$1
    if [[ $bytes -ge 1073741824 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1073741824}") GB"
    elif [[ $bytes -ge 1048576 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1048576}") MB"
    elif [[ $bytes -ge 1024 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1024}") KB"
    else echo "$bytes B"; fi
}

download_file() {
    local url="$1" output="$2" resume="$3"
    
    if command -v curl &>/dev/null; then
        local flags=(-L --progress-bar --connect-timeout 30 --max-time 7200 --retry 3 -o "$output")
        [[ "$resume" == "true" && -f "$output" ]] && flags+=(-C -)
        curl "${flags[@]}" "$url" && return 0
    fi
    
    if command -v wget &>/dev/null; then
        local flags=(--show-progress --progress=bar:force --timeout=30 --tries=3 -O "$output")
        [[ "$resume" == "true" && -f "$output" ]] && flags+=(--continue)
        wget "${flags[@]}" "$url" && return 0
    fi
    
    return 1
}

verify_model() {
    local model_file="$1"
    [[ ! -f "$model_file" ]] && { log_error "Model not found: $model_file"; return 3; }
    
    local size_bytes=$(get_file_size "$model_file")
    local size_mb=$((size_bytes / 1024 / 1024))
    local expected_mb="${MODEL_SIZES_MB[$MODEL_SIZE]}"
    
    log_info "Model: $(basename "$model_file")"
    log_info "Size: $(format_size $size_bytes) ($size_mb MB)"
    
    if [[ $size_mb -lt $((expected_mb * 80 / 100)) ]]; then
        log_error "Model too small! Expected ~${expected_mb}MB"
        return 3
    fi
    
    log_success "Verification passed"
    return 0
}

show_info() {
    print_header "Whisper Model Information"
    echo ""
    echo "Model Sizes:"
    echo "  tiny   - ~75MB   - Fastest, lowest accuracy (testing only)"
    echo "  base   - ~142MB  - Fast, good accuracy (recommended)"
    echo "  small  - ~466MB  - Slower, better accuracy"
    echo "  medium - ~1.5GB  - Slow, high accuracy"
    echo "  large  - ~3.1GB  - Slowest, highest accuracy"
    echo ""
    echo "For Egyptian Arabic, we recommend:"
    echo "  - base: Good balance for most devices"
    echo "  - small: Better accuracy if storage allows"
    echo ""
}

show_help() {
    cat << EOF
Egyptian Agent - Whisper Model Download Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --size SIZE         Model size: tiny, base, small, medium, large (default: base)
    --output DIR        Output directory (default: app/src/main/assets/models/)
    --source SOURCE     Download source: huggingface, original (default: huggingface)
    --resume            Resume interrupted download
    --verify PATH       Verify an existing model file
    --info              Show model information
    --log-file PATH     Write download log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    $SCRIPT_NAME                        # Download base model
    $SCRIPT_NAME --size small           # Download small model
    $SCRIPT_NAME --verify ggml-base.bin # Verify existing model
    $SCRIPT_NAME --info                 # Show model info

RETURN CODES:
    0   Success
    1   General error
    2   Download failed
    3   Verification failed
    5   Invalid arguments
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --size) MODEL_SIZE="${2,,}"; shift 2;;
            --output) OUTPUT_DIR="$2"; shift 2;;
            --source) DOWNLOAD_SOURCE="$2"; shift 2;;
            --resume) RESUME_DOWNLOAD=true; shift;;
            --verify) VERIFY_MODEL="$2"; shift 2;;
            --info) SHOW_INFO=true; shift;;
            --log-file) LOG_FILE="$2"; shift 2;;
            --ci) CI_MODE=true; COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]=''); shift;;
            -h|--help) show_help; exit 0;;
            -*) log_error "Unknown option: $1"; return 5;;
            *) log_error "Unexpected argument: $1"; return 5;;
        esac
    done
    
    if [[ ! "${MODEL_FILES[$MODEL_SIZE]+isset}" ]]; then
        log_error "Invalid model size: $MODEL_SIZE"
        log_error "Valid options: tiny, base, small, medium, large"
        return 5
    fi
}

main() {
    parse_arguments "$@" || exit $?
    init_logging
    print_header "Whisper Model Download"
    
    [[ "$SHOW_INFO" == "true" ]] && { show_info; exit 0; }
    
    if [[ -n "$VERIFY_MODEL" ]]; then
        MODEL_SIZE="base"
        verify_model "$VERIFY_MODEL"
        exit $?
    fi
    
    local model_file="${MODEL_FILES[$MODEL_SIZE]}"
    local output_file="$OUTPUT_DIR/$model_file"
    local download_url="${MODEL_URLS[$DOWNLOAD_SOURCE]}$model_file"
    
    log_info "Model: $MODEL_SIZE ($(format_size $((MODEL_SIZES_MB[$MODEL_SIZE] * 1024 * 1024))))"
    log_info "Output: $output_file"
    
    if [[ -f "$output_file" ]]; then
        log_info "Model already exists"
        verify_model "$output_file" && exit 0
        rm -f "$output_file"
    fi
    
    mkdir -p "$OUTPUT_DIR"
    
    log_step "Downloading..."
    if ! download_file "$download_url" "$output_file" "$RESUME_DOWNLOAD"; then
        log_error "Download failed"
        return 2
    fi
    
    verify_model "$output_file"
    log_success "Download complete: $output_file"
}

trap 'log_error "Interrupted"; exit 1' INT TERM
main "$@"
exit $?
