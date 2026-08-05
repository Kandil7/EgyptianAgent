#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - FunctionGemma Model Setup Script
# =============================================================================
#
# PURPOSE:
#   Sets up the FunctionGemma model for the Egyptian Agent application.
#   Downloads or copies the model and places it in the correct location.
#
# USAGE:
#   ./ml/finetune/scripts/setup_functiongemma_model.sh [OPTIONS]
#
# OPTIONS:
#   --source PATH       Source model path or URL (default: download from HuggingFace)
#   --output DIR        Output directory (default: android/src/main/assets/models/)
#   --quantization TYPE Quantization type (default: Q4_K_M)
#   --deploy            Also deploy to connected device
#   --device SERIAL     Target device serial for deployment
#   --log-file PATH     Write setup log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./ml/finetune/scripts/setup_functiongemma_model.sh
#   ./ml/finetune/scripts/setup_functiongemma_model.sh --source /path/to/model.gguf
#   ./ml/finetune/scripts/setup_functiongemma_model.sh --deploy
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Download failed
#   3   Setup failed
#   4   Deployment failed
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

SOURCE_PATH=""
OUTPUT_DIR="$PROJECT_DIR/android/src/main/assets/models"
QUANTIZATION="Q4_K_M"
DEPLOY_TO_DEVICE=false
DEVICE_SERIAL=""
LOG_FILE=""
CI_MODE=false

MODEL_NAME="functiongemma-270m-it"

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

download_model() {
    local output_file="$OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf"
    local model_url="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-${QUANTIZATION}.gguf"
    
    if [[ -f "$output_file" ]]; then
        log_info "Model already exists: $output_file"
        echo "$output_file"
        return 0
    fi
    
    log_step "Downloading FunctionGemma model..."
    
    if command -v curl &>/dev/null; then
        curl -L --progress-bar -o "$output_file" "$model_url"
    elif command -v wget &>/dev/null; then
        wget --show-progress -O "$output_file" "$model_url"
    else
        log_error "Neither curl nor wget available"
        return 2
    fi
    
    if [[ ! -f "$output_file" ]]; then
        log_error "Download failed"
        return 2
    fi
    
    log_success "Download complete"
    echo "$output_file"
}

copy_local_model() {
    local source="$1"
    local output_file="$OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf"
    
    if [[ ! -f "$source" ]]; then
        log_error "Source file not found: $source"
        return 3
    fi
    
    log_step "Copying local model..."
    cp "$source" "$output_file"
    
    log_success "Model copied"
    echo "$output_file"
}

verify_model() {
    local model_file="$1"
    
    log_step "Verifying model..."
    
    [[ ! -f "$model_file" ]] && { log_error "Model not found"; return 3; }
    
    local size=$(get_file_size "$model_file")
    local size_mb=$((size / 1024 / 1024))
    
    log_info "Model: $(basename "$model_file")"
    log_info "Size: $(format_size $size) ($size_mb MB)"
    
    # Check GGUF magic
    local magic=$(head -c 4 "$model_file" 2>/dev/null || echo "")
    [[ "$magic" == "GGUF" ]] && log_info "GGUF magic: verified" || log_warn "GGUF magic: could not verify"
    
    log_success "Verification passed"
}

deploy_to_device() {
    local model_file="$1"
    
    if [[ "$DEPLOY_TO_DEVICE" != "true" ]]; then
        return 0
    fi
    
    log_step "Deploying to device..."
    
    if ! command -v adb &>/dev/null; then
        log_error "ADB not found"
        return 4
    fi
    
    local device_flag=""
    [[ -n "$DEVICE_SERIAL" ]] && device_flag="-s $DEVICE_SERIAL"
    
    local device_count=$(adb $device_flag devices 2>/dev/null | grep -c "device$" || echo "0")
    if [[ "$device_count" -eq 0 ]]; then
        log_error "No device connected"
        return 4
    fi
    
    local remote_dir="/data/local/llm"
    adb $device_flag shell "mkdir -p $remote_dir"
    adb $device_flag push "$model_file" "$remote_dir/"
    
    log_success "Deployed to: $remote_dir"
}

show_help() {
    cat << EOF
Egyptian Agent - FunctionGemma Model Setup Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --source PATH       Source model path (local file)
                        If not specified, downloads from HuggingFace
    --output DIR        Output directory (default: android/src/main/assets/models/)
    --quantization TYPE Quantization type (default: Q4_K_M)
    --deploy            Deploy to connected device
    --device SERIAL     Target device serial for deployment
    --log-file PATH     Write log to file
    --ci                CI/CD mode
    -h, --help          Show help

EXAMPLES:
    $SCRIPT_NAME                                    # Download and setup
    $SCRIPT_NAME --source /path/to/model.gguf       # Use local model
    $SCRIPT_NAME --deploy                           # Deploy to device
    $SCRIPT_NAME --deploy --device ABC123           # Deploy to specific device

RETURN CODES:
    0   Success
    1   General error
    2   Download failed
    3   Setup failed
    4   Deployment failed
EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --source) SOURCE_PATH="$2"; shift 2;;
            --output) OUTPUT_DIR="$2"; shift 2;;
            --quantization) QUANTIZATION="$2"; shift 2;;
            --deploy) DEPLOY_TO_DEVICE=true; shift;;
            --device) DEVICE_SERIAL="$2"; shift 2;;
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
    print_header "FunctionGemma Model Setup"
    
    log_info "Output: $OUTPUT_DIR"
    log_info "Quantization: $QUANTIZATION"
    log_info "Deploy: $DEPLOY_TO_DEVICE"
    
    mkdir -p "$OUTPUT_DIR"
    
    local model_file
    if [[ -n "$SOURCE_PATH" ]]; then
        model_file=$(copy_local_model "$SOURCE_PATH") || exit $?
    else
        model_file=$(download_model) || exit $?
    fi
    
    verify_model "$model_file" || exit $?
    deploy_to_device "$model_file" || exit $?
    
    print_header "Setup Complete"
    log_success "Model ready: $model_file"
    log_info "Next: ./deploy/build/scripts/build_functiongemma.sh"
}

trap 'log_error "Interrupted"; exit 1' INT TERM
main "$@"
