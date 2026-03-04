#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - GGUF Conversion Script
# =============================================================================
#
# PURPOSE:
#   Converts fine-tuned HuggingFace models to GGUF format with quantization
#   for mobile deployment. Supports FunctionGemma and other compatible models.
#
# USAGE:
#   ./scripts/model/convert_to_gguf.sh [OPTIONS]
#
# OPTIONS:
#   --input DIR         Input model directory (required)
#   --output DIR        Output directory (default: models/)
#   --quantization TYPE Quantization type (default: Q4_K_M)
#   --f16               Keep F16 intermediate file
#   --clean             Remove intermediate files after conversion
#   --log-file PATH     Write conversion log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# QUANTIZATION TYPES:
#   Q4_K_S  - Smallest, lower quality
#   Q4_K_M  - Best size/quality balance (recommended)
#   Q5_K_S  - Smaller, good quality
#   Q5_K_M  - Good size, better quality
#   Q6_K    - Larger, high quality
#   Q8_0    - Largest, near-lossless
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Prerequisites missing
#   3   Conversion failed
#   5   Invalid arguments
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

INPUT_DIR=""
OUTPUT_DIR="$PROJECT_DIR/models"
QUANTIZATION="Q4_K_M"
KEEP_F16=false
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
format_size() { local b=$1; if [[ $b -ge 1073741824 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $b / 1073741824}") GB"; elif [[ $b -ge 1048576 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $b / 1048576}") MB"; elif [[ $b -ge 1024 ]]; then echo "$(awk "BEGIN {printf \"%.2f\", $b / 1024}") KB"; else echo "$b B"; fi; }

check_prerequisites() {
    log_step "Checking prerequisites..."
    local missing=()
    
    command -v python3 &>/dev/null || missing+=("Python 3.8+")
    [[ -d "$INPUT_DIR" ]] || missing+=("Input directory: $INPUT_DIR")
    
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing: ${missing[*]}"
        return 2
    fi
    
    # Check for llama.cpp
    local llama_dir="$PROJECT_DIR/external/llama.cpp"
    if [[ ! -d "$llama_dir" ]]; then
        log_warn "llama.cpp not found, will clone..."
        git clone --depth 1 https://github.com/ggerganov/llama.cpp.git "$llama_dir"
    fi
    
    # Install Python dependencies
    pip3 install --quiet --upgrade pip
    pip3 install --quiet -r "$llama_dir/requirements.txt" 2>/dev/null || true
    
    log_success "Prerequisites ready"
}

convert_to_f16() {
    local model_name=$(basename "$INPUT_DIR")
    local output="$OUTPUT_DIR/${model_name}-f16.gguf"
    local llama_dir="$PROJECT_DIR/external/llama.cpp"
    
    log_step "Converting to GGUF (F16)..."
    
    python3 "$llama_dir/convert_hf_to_gguf.py" "$INPUT_DIR" --outfile "$output" --vocab-type bpe || {
        log_error "F16 conversion failed"
        return 3
    }
    
    local size=$(get_file_size "$output")
    log_success "F16 model: $(format_size $size)"
    echo "$output"
}

quantize_model() {
    local f16_input="$1"
    local model_name=$(basename "$INPUT_DIR")
    local output="$OUTPUT_DIR/${model_name}-${QUANTIZATION}.gguf"
    local llama_dir="$PROJECT_DIR/external/llama.cpp"
    
    log_step "Quantizing to $QUANTIZATION..."
    
    "$llama_dir/llama-quantize" "$f16_input" "$output" "$QUANTIZATION" || {
        log_error "Quantization failed"
        return 3
    }
    
    local size=$(get_file_size "$output")
    log_success "Quantized: $(format_size $size)"
    echo "$output"
}

verify_output() {
    local output="$1"
    
    log_step "Verifying output..."
    
    [[ ! -f "$output" ]] && { log_error "Output not found"; return 3; }
    
    local size=$(get_file_size "$output")
    local size_mb=$((size / 1024 / 1024))
    
    log_info "Output: $(basename "$output")"
    log_info "Size: $(format_size $size) ($size_mb MB)"
    
    # Check GGUF magic
    local magic=$(head -c 4 "$output" 2>/dev/null || echo "")
    [[ "$magic" == "GGUF" ]] && log_info "GGUF magic: verified" || log_warn "GGUF magic: could not verify"
    
    log_success "Verification passed"
}

show_help() {
    cat << EOF
Egyptian Agent - GGUF Conversion Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --input DIR         Input model directory (required)
    --output DIR        Output directory (default: models/)
    --quantization TYPE Quantization type (default: Q4_K_M)
                        Options: Q4_K_S, Q4_K_M, Q5_K_S, Q5_K_M, Q6_K, Q8_0
    --f16               Keep F16 intermediate file
    --clean             Remove intermediate files
    --log-file PATH     Write log to file
    --ci                CI/CD mode
    -h, --help          Show help

EXAMPLES:
    $SCRIPT_NAME --input models/my-finetuned-model
    $SCRIPT_NAME --input models/egyptian-model --quantization Q5_K_M
    $SCRIPT_NAME --input models/model --clean

QUANTIZATION GUIDE:
    Q4_K_M  - Best balance (recommended for mobile)
    Q5_K_M  - Better quality, larger size
    Q8_0    - Near-lossless, largest size

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Conversion failed
    5   Invalid arguments
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --input) INPUT_DIR="$2"; shift 2;;
            --output) OUTPUT_DIR="$2"; shift 2;;
            --quantization) QUANTIZATION="$2"; shift 2;;
            --f16) KEEP_F16=true; shift;;
            --clean) CLEAN_AFTER=true; shift;;
            --log-file) LOG_FILE="$2"; shift 2;;
            --ci) CI_MODE=true; COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]=''); shift;;
            -h|--help) show_help; exit 0;;
            -*) log_error "Unknown option: $1"; exit 5;;
            *) log_error "Unexpected argument: $1"; exit 5;;
        esac
    done
    
    if [[ -z "$INPUT_DIR" ]]; then
        log_error "--input is required"
        exit 5
    fi
}

main() {
    parse_arguments
    init_logging
    print_header "GGUF Conversion"
    
    log_info "Input: $INPUT_DIR"
    log_info "Output: $OUTPUT_DIR"
    log_info "Quantization: $QUANTIZATION"
    
    check_prerequisites || exit $?
    
    local f16_output
    f16_output=$(convert_to_f16) || exit $?
    
    local quantized_output
    quantized_output=$(quantize_model "$f16_output") || exit $?
    
    verify_output "$quantized_output"
    
    if [[ "$CLEAN_AFTER" == "true" && "$KEEP_F16" != "true" ]]; then
        log_step "Cleaning intermediate files..."
        rm -f "$f16_output"
    fi
    
    print_header "Conversion Complete"
    log_success "Output: $quantized_output"
}

trap 'log_error "Interrupted"; exit 1' INT TERM
main "$@"
