#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Model Fetch Script
# =============================================================================
#
# PURPOSE:
#   Fetches all required AI models for the Egyptian Agent including
#   FunctionGemma, Whisper, and optional Llama models.
#
# USAGE:
#   ./scripts/utils/fetch_models.sh [OPTIONS]
#
# OPTIONS:
#   --all               Fetch all models (default)
#   --functiongemma     Fetch FunctionGemma model only
#   --whisper           Fetch Whisper model only
#   --llama             Fetch Llama model only
#   --vosk              Fetch Vosk Arabic model only
#   --output DIR        Output directory (default: app/src/main/assets/models/)
#   --skip-large        Skip large models (>500MB)
#   --log-file PATH     Write fetch log to specified file
#   --ci                CI/CD mode
#   -h, --help          Show this help message
#
# MODELS:
#   FunctionGemma  - 270M parameter function calling model (~288MB)
#   Whisper base   - Speech recognition model (~142MB)
#   Llama 3.2 3B   - Large language model (~2GB quantized)
#   Vosk Arabic    - Offline STT model (~50MB)
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Download failed
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

FETCH_ALL=true
FETCH_FUNCTIONGEMMA=false
FETCH_WHISPER=false
FETCH_LLAMA=false
FETCH_VOSK=false
OUTPUT_DIR="$PROJECT_DIR/app/src/main/assets/models"
SKIP_LARGE=false
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

download_file() {
    local url="$1" output="$2" name="$3"
    
    if [[ -f "$output" ]]; then
        log_info "$name already exists"
        return 0
    fi
    
    log_step "Downloading $name..."
    
    if command -v curl &>/dev/null; then
        curl -L --progress-bar -o "$output" "$url" && return 0
    elif command -v wget &>/dev/null; then
        wget --show-progress -O "$output" "$url" && return 0
    fi
    
    log_error "No download tool available"
    return 2
}

fetch_functiongemma() {
    log_step "Fetching FunctionGemma model..."
    local url="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-Q4_K_M.gguf"
    local output="$OUTPUT_DIR/functiongemma-270m-it-Q4_K_M.gguf"
    download_file "$url" "$output" "FunctionGemma"
}

fetch_whisper() {
    log_step "Fetching Whisper model..."
    local url="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
    local output="$OUTPUT_DIR/ggml-base.bin"
    download_file "$url" "$output" "Whisper base"
}

fetch_vosk() {
    log_step "Fetching Vosk Arabic model..."
    local url="https://alphacephei.com/vosk/models/vosk-model-small-ar-0.22.tar.gz"
    local temp="/tmp/vosk-ar.tar.gz"
    
    download_file "$url" "$temp" "Vosk Arabic"
    
    log_info "Extracting Vosk model..."
    tar -xzf "$temp" -C "$OUTPUT_DIR" --strip-components=1 2>/dev/null || true
    rm -f "$temp"
}

fetch_llama() {
    if [[ "$SKIP_LARGE" == "true" ]]; then
        log_warn "Skipping Llama model (--skip-large)"
        return 0
    fi
    
    log_step "Fetching Llama model (large file, ~2GB)..."
    log_warn "This may take a while..."
    
    # Llama model would be downloaded via convert script
    log_info "Use ./scripts/model/setup_llama_model.sh for Llama model"
}

show_help() {
    cat << EOF
Egyptian Agent - Model Fetch Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --all               Fetch all models (default)
    --functiongemma     Fetch FunctionGemma only
    --whisper           Fetch Whisper only
    --llama             Fetch Llama only
    --vosk              Fetch Vosk Arabic only
    --output DIR        Output directory
    --skip-large        Skip large models (>500MB)
    --log-file PATH     Write log to file
    --ci                CI/CD mode
    -h, --help          Show help

MODELS:
    FunctionGemma  - ~288MB (function calling)
    Whisper base   - ~142MB (speech recognition)
    Llama 3.2 3B   - ~2GB (language model)
    Vosk Arabic    - ~50MB (offline STT)

RETURN CODES:
    0   Success
    1   General error
    2   Download failed
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --all) FETCH_ALL=true; shift;;
            --functiongemma) FETCH_ALL=false; FETCH_FUNCTIONGEMMA=true; shift;;
            --whisper) FETCH_ALL=false; FETCH_WHISPER=true; shift;;
            --llama) FETCH_ALL=false; FETCH_LLAMA=true; shift;;
            --vosk) FETCH_ALL=false; FETCH_VOSK=true; shift;;
            --output) OUTPUT_DIR="$2"; shift 2;;
            --skip-large) SKIP_LARGE=true; shift;;
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
    init_logging
    
    print_header "Egyptian Agent Model Fetch"
    mkdir -p "$OUTPUT_DIR"
    
    local exit_code=0
    
    if [[ "$FETCH_ALL" == "true" ]]; then
        fetch_functiongemma || exit_code=$?
        fetch_whisper || exit_code=$?
        fetch_vosk || exit_code=$?
        fetch_llama || exit_code=$?
    else
        [[ "$FETCH_FUNCTIONGEMMA" == "true" ]] && fetch_functiongemma || exit_code=$?
        [[ "$FETCH_WHISPER" == "true" ]] && fetch_whisper || exit_code=$?
        [[ "$FETCH_LLAMA" == "true" ]] && fetch_llama || exit_code=$?
        [[ "$FETCH_VOSK" == "true" ]] && fetch_vosk || exit_code=$?
    fi
    
    echo ""
    print_header "Fetch Complete"
    log_info "Models directory: $OUTPUT_DIR"
    ls -lh "$OUTPUT_DIR" 2>/dev/null || true
    
    exit $exit_code
}

main "$@"
