#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Llama Model Conversion Script
# =============================================================================
#
# PURPOSE:
#   Converts Llama 3.2 3B model from HuggingFace to GGUF format with
#   specified quantization for mobile deployment.
#
# USAGE:
#   ./ml/finetune/scripts/convert_llama_model.sh [OPTIONS]
#
# OPTIONS:
#   --model NAME        Model name (default: Llama-3.2-3B-Instruct)
#   --output DIR        Output directory (default: models/)
#   --quantization TYPE Quantization: Q4_K_M, Q5_K_M, Q8_0 (default: Q4_K_M)
#   --hf-token TOKEN    HuggingFace token for gated models
#   --skip-download     Skip download if model exists locally
#   --clean             Remove intermediate files after conversion
#   --log-file PATH     Write conversion log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# REQUIREMENTS:
#   - Python 3.8+
#   - llama.cpp repository
#   - HuggingFace account (for gated models)
#   - Sufficient disk space (~20GB for conversion)
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Prerequisites missing
#   3   Conversion failed
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

MODEL_NAME="Llama-3.2-3B-Instruct"
OUTPUT_DIR="$PROJECT_DIR/models"
QUANTIZATION="Q4_K_M"
HF_TOKEN=""
SKIP_DOWNLOAD=false
CLEAN_AFTER=false
LOG_FILE=""
CI_MODE=false

declare -A COLORS=([red]='\033[0;31m' [green]='\033[0;32m' [yellow]='\033[1;33m' [blue]='\033[0;34m' [cyan]='\033[0;36m' [nc]='\033[0m')

init_logging() { mkdir -p "$LOG_DIR"; [[ -n "$LOG_FILE" ]] && exec > >(tee -a "$LOG_FILE") 2>&1; }
log_info() { [[ "$CI_MODE" == "true" ]] && echo "[INFO] $*" || echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"; }
log_warn() { [[ "$CI_MODE" == "true" ]] && echo "[WARN] $*" || echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"; }
log_error() { [[ "$CI_MODE" == "true" ]] && echo "[ERROR] $*" >&2 || echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2; }
log_step() { [[ "$CI_MODE" == "true" ]] && echo "[STEP] $*" || echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"; }
log_success() { [[ "$CI_MODE" == "true" ]] && echo "[SUCCESS] $*" || echo -e "${COLORS[cyan]}[SUCCESS]${COLORS[nc]} $*"; }
print_header() { local w=60; [[ "$CI_MODE" == "true" ]] && echo "=== $1 ===" || { echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; echo -e "${COLORS[blue]}  $1${COLORS[nc]}"; echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $w))${COLORS[nc]}"; }; }

check_prerequisites() {
    log_step "Checking prerequisites..."
    local missing=()
    
    command -v python3 &>/dev/null || missing+=("Python 3.8+")
    command -v pip3 &>/dev/null || missing+=("pip3")
    command -v git &>/dev/null || missing+=("git")
    
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing: ${missing[*]}"
        return 2
    fi
    
    log_info "Prerequisites check passed"
}

install_dependencies() {
    log_step "Installing Python dependencies..."
    pip3 install --quiet huggingface_hub transformers accelerate sentencepiece
    log_success "Dependencies installed"
}

clone_llama_cpp() {
    local llama_dir="$PROJECT_DIR/external/llama.cpp"
    
    if [[ -d "$llama_dir/.git" ]]; then
        log_info "llama.cpp exists, updating..."
        (cd "$llama_dir" && git pull --quiet)
    else
        log_step "Cloning llama.cpp..."
        git clone --depth 1 https://github.com/ggerganov/llama.cpp.git "$llama_dir"
    fi
    
    log_info "llama.cpp ready"
}

download_model() {
    local model_dir="$OUTPUT_DIR/$(echo "$MODEL_NAME" | tr '[:upper:]' '[:lower:]')"
    
    if [[ -d "$model_dir" && "$SKIP_DOWNLOAD" == "true" ]]; then
        log_info "Model exists, skipping download"
        echo "$model_dir"
        return 0
    fi
    
    log_step "Downloading $MODEL_NAME from HuggingFace..."
    
    mkdir -p "$model_dir"
    
    python3 -c "
from huggingface_hub import snapshot_download
import sys
try:
    snapshot_download(
        repo_id='meta-llama/$MODEL_NAME',
        local_dir='$model_dir',
        local_dir_use_symlinks=False,
        token='${HF_TOKEN:-None}'
    )
    print('SUCCESS')
except Exception as e:
    print(f'ERROR: {e}', file=sys.stderr)
    sys.exit(1)
" || { log_error "Download failed"; return 3; }
    
    log_success "Model downloaded: $model_dir"
    echo "$model_dir"
}

convert_to_gguf() {
    local model_dir="$1"
    local llama_dir="$PROJECT_DIR/external/llama.cpp"
    local f16_output="$OUTPUT_DIR/$(echo "$MODEL_NAME" | tr '[:upper:]' '[:lower:]')-f16.gguf"
    
    log_step "Converting to GGUF (F16)..."
    
    python3 "$llama_dir/convert_hf_to_gguf.py" "$model_dir" --outfile "$f16_output" || {
        log_error "Conversion failed"
        return 3
    }
    
    log_success "F16 model created: $f16_output"
    echo "$f16_output"
}

quantize_model() {
    local f16_input="$1"
    local llama_dir="$PROJECT_DIR/external/llama.cpp"
    local output="$OUTPUT_DIR/$(echo "$MODEL_NAME" | tr '[:upper:]' '[:lower:]')-${QUANTIZATION}.gguf"
    
    log_step "Quantizing to $QUANTIZATION..."
    
    "$llama_dir/llama-quantize" "$f16_input" "$output" "$QUANTIZATION" || {
        log_error "Quantization failed"
        return 3
    }
    
    log_success "Quantized model: $output"
    echo "$output"
}

show_help() {
    cat << EOF
Egyptian Agent - Llama Model Conversion Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --model NAME        Model name (default: Llama-3.2-3B-Instruct)
    --output DIR        Output directory (default: models/)
    --quantization TYPE Quantization: Q4_K_M, Q5_K_M, Q8_0 (default: Q4_K_M)
    --hf-token TOKEN    HuggingFace token for gated models
    --skip-download     Skip download if model exists
    --clean             Remove intermediate files
    --log-file PATH     Write log to file
    --ci                CI/CD mode
    -h, --help          Show help

REQUIREMENTS:
    - Python 3.8+
    - ~20GB disk space
    - HuggingFace account (for gated models)

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Conversion failed
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --model) MODEL_NAME="$2"; shift 2;;
            --output) OUTPUT_DIR="$2"; shift 2;;
            --quantization) QUANTIZATION="$2"; shift 2;;
            --hf-token) HF_TOKEN="$2"; shift 2;;
            --skip-download) SKIP_DOWNLOAD=true; shift;;
            --clean) CLEAN_AFTER=true; shift;;
            --log-file) LOG_FILE="$2"; shift 2;;
            --ci) CI_MODE=true; COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]=''); shift;;
            -h|--help) show_help; exit 0;;
            -*) log_error "Unknown option: $1"; exit 5;;
            *) log_error "Unexpected argument: $1"; exit 5;;
        esac
    done
}

main() {
    parse_arguments
    init_logging
    print_header "Llama Model Conversion"
    
    check_prerequisites || exit $?
    install_dependencies
    clone_llama_cpp
    
    mkdir -p "$OUTPUT_DIR"
    
    local model_dir
    model_dir=$(download_model) || exit $?
    
    local f16_output
    f16_output=$(convert_to_gguf "$model_dir") || exit $?
    
    local quantized_output
    quantized_output=$(quantize_model "$f16_output") || exit $?
    
    if [[ "$CLEAN_AFTER" == "true" ]]; then
        log_step "Cleaning intermediate files..."
        rm -f "$f16_output"
    fi
    
    print_header "Conversion Complete"
    log_success "Model converted: $quantized_output"
}

trap 'log_error "Interrupted"; exit 1' INT TERM
main "$@"
