#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Llama Model Setup Script
# =============================================================================
#
# PURPOSE:
#   Downloads, converts, and sets up the Llama 3.2 3B model for the
#   Egyptian Agent application. Automates the complete model pipeline.
#
# USAGE:
#   ./scripts/model/setup_llama_model.sh [OPTIONS]
#
# OPTIONS:
#   --model NAME        Model name (default: Llama-3.2-3B-Instruct)
#   --quantization TYPE Quantization type (default: Q4_K_M)
#   --output DIR        Output directory (default: app/src/main/assets/model/)
#   --hf-token TOKEN    HuggingFace token for gated models
#   --skip-download     Skip download if model exists
#   --clean             Remove intermediate files
#   --log-file PATH     Write setup log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# REQUIREMENTS:
#   - Python 3.8+
#   - HuggingFace account (for gated models)
#   - ~20GB disk space for conversion
#   - llama.cpp will be cloned if not present
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Prerequisites missing
#   3   Setup failed
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
QUANTIZATION="Q4_K_M"
OUTPUT_DIR="$PROJECT_DIR/app/src/main/assets/model"
HF_TOKEN=""
SKIP_DOWNLOAD=false
CLEAN_AFTER=false
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

get_file_size() { [[ -f "$1" ]] && (stat -c%s "$1" 2>/dev/null || stat -f%z "$1" 2>/dev/null || echo "0") || echo "0"; }
format_size() { local b=$1; if [[ $b -ge 1073741824 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $b / 1073741824}") GB"; elif [[ $b -ge 1048576 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $b / 1048576}") MB"; else echo "$b B"; fi; }

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

setup_llama_cpp() {
    local llama_dir="$PROJECT_DIR/external/llama.cpp"
    
    if [[ -d "$llama_dir/.git" ]]; then
        log_info "llama.cpp exists"
        (cd "$llama_dir" && git pull --quiet 2>/dev/null) || true
    else
        log_step "Cloning llama.cpp..."
        git clone --depth 1 https://github.com/ggerganov/llama.cpp.git "$llama_dir"
    fi
    
    # Install Python dependencies
    pip3 install --quiet -r "$llama_dir/requirements.txt" 2>/dev/null || true
    
    log_success "llama.cpp ready"
    echo "$llama_dir"
}

download_model() {
    local model_dir="$PROJECT_DIR/models/$(echo "$MODEL_NAME" | tr '[:upper:]' '[:lower:]')"
    
    if [[ -d "$model_dir" && "$SKIP_DOWNLOAD" == "true" ]]; then
        log_info "Model exists, skipping download"
        echo "$model_dir"
        return 0
    fi
    
    log_step "Downloading $MODEL_NAME..."
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
    
    log_success "Model downloaded"
    echo "$model_dir"
}

convert_model() {
    local model_dir="$1"
    local llama_dir="$2"
    local f16_output="$PROJECT_DIR/models/$(echo "$MODEL_NAME" | tr '[:upper:]' '[:lower:]')-f16.gguf"
    
    log_step "Converting to GGUF (F16)..."
    python3 "$llama_dir/convert_hf_to_gguf.py" "$model_dir" --outfile "$f16_output" || {
        log_error "Conversion failed"
        return 3
    }
    
    log_success "F16 conversion complete"
    echo "$f16_output"
}

quantize_model() {
    local f16_input="$1"
    local llama_dir="$2"
    local output="$PROJECT_DIR/models/$(echo "$MODEL_NAME" | tr '[:upper:]' '[:lower:]')-${QUANTIZATION}.gguf"
    
    log_step "Quantizing to $QUANTIZATION..."
    "$llama_dir/llama-quantize" "$f16_input" "$output" "$QUANTIZATION" || {
        log_error "Quantization failed"
        return 3
    }
    
    log_success "Quantization complete"
    echo "$output"
}

copy_to_assets() {
    local source="$1"
    local model_subdir="$OUTPUT_DIR/llama-3.2-3b-q4_k_m"
    
    log_step "Copying to assets..."
    mkdir -p "$model_subdir"
    cp "$source" "$model_subdir/$(basename "$source")"
    
    # Create model properties
    cat > "$model_subdir/model.properties" << EOF
model.name=Llama 3.2 3B ${QUANTIZATION}
model.version=3.2
model.size=3B
model.quantization=${QUANTIZATION}
model.type=gguf
model.path=llama-3.2-3b-q4_k_m/$(basename "$source")
model.size_bytes=$(get_file_size "$source")
EOF
    
    log_success "Model copied to assets"
}

show_help() {
    cat << EOF
Egyptian Agent - Llama Model Setup Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --model NAME        Model name (default: Llama-3.2-3B-Instruct)
    --quantization TYPE Quantization type (default: Q4_K_M)
    --output DIR        Output directory (default: app/src/main/assets/model/)
    --hf-token TOKEN    HuggingFace token for gated models
    --skip-download     Skip download if model exists
    --clean             Remove intermediate files
    --log-file PATH     Write log to file
    --ci                CI/CD mode
    -h, --help          Show help

REQUIREMENTS:
    - Python 3.8+
    - HuggingFace account (for gated models)
    - ~20GB disk space

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Setup failed
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --model) MODEL_NAME="$2"; shift 2;;
            --quantization) QUANTIZATION="$2"; shift 2;;
            --output) OUTPUT_DIR="$2"; shift 2;;
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
    print_header "Llama Model Setup"
    
    log_info "Model: $MODEL_NAME"
    log_info "Quantization: $QUANTIZATION"
    log_info "Output: $OUTPUT_DIR"
    
    check_prerequisites || exit $?
    
    local llama_dir
    llama_dir=$(setup_llama_cpp) || exit $?
    
    local model_dir
    model_dir=$(download_model) || exit $?
    
    local f16_output
    f16_output=$(convert_model "$model_dir" "$llama_dir") || exit $?
    
    local quantized_output
    quantized_output=$(quantize_model "$f16_output" "$llama_dir") || exit $?
    
    copy_to_assets "$quantized_output"
    
    if [[ "$CLEAN_AFTER" == "true" ]]; then
        log_step "Cleaning intermediate files..."
        rm -f "$f16_output"
    fi
    
    print_header "Setup Complete"
    log_success "Model ready at: $OUTPUT_DIR/llama-3.2-3b-q4_k_m/"
    log_info "Size: $(format_size $(get_file_size "$quantized_output"))"
}

trap 'log_error "Interrupted"; exit 1' INT TERM
main "$@"
