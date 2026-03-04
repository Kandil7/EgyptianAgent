#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - FunctionGemma Model Download Script
# =============================================================================
#
# PURPOSE:
#   Downloads the FunctionGemma-270M-IT model from HuggingFace or uses a local
#   model file. Supports resume downloads, integrity verification, and multiple
#   quantization options.
#
# USAGE:
#   ./scripts/model/download_functiongemma_model.sh [OPTIONS]
#
# OPTIONS:
#   --local PATH        Use local model file instead of downloading
#   --output DIR        Output directory (default: app/src/main/assets/models/)
#   --quantization TYPE Quantization type: Q4_K_M, Q5_K_M, Q8_0 (default: Q4_K_M)
#   --source SOURCE     Download source: huggingface, modelscope (default: huggingface)
#   --resume            Resume interrupted download
#   --verify PATH       Verify an existing model file
#   --info              Show model information
#   --log-file PATH     Write download log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/model/download_functiongemma_model.sh
#   ./scripts/model/download_functiongemma_model.sh --local /path/to/model.gguf
#   ./scripts/model/download_functiongemma_model.sh --quantization Q8_0
#   ./scripts/model/download_functiongemma_model.sh --verify model.gguf
#   ./scripts/model/download_functiongemma_model.sh --info
#
# MODEL INFO:
#   Name:       google/functiongemma-270m-it
#   Format:     GGUF (Q4_K_M quantized)
#   Parameters: 270 million
#   Size:       ~288MB (Q4_K_M)
#   Context:    2048 tokens
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

# =============================================================================
# Configuration
# =============================================================================

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly LOG_DIR="$PROJECT_DIR/build/logs"
readonly DOWNLOAD_TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# Default configuration
LOCAL_MODEL=""
OUTPUT_DIR="$PROJECT_DIR/app/src/main/assets/models"
QUANTIZATION="Q4_K_M"
DOWNLOAD_SOURCE="huggingface"
RESUME_DOWNLOAD=false
VERIFY_MODEL=""
SHOW_INFO=false
LOG_FILE=""
CI_MODE=false

# Model configuration
MODEL_NAME="functiongemma-270m-it"

# Download URLs
declare -A MODEL_URLS=(
    ["Q4_K_M"]="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-Q4_K_M.gguf"
    ["Q5_K_M"]="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-Q5_K_M.gguf"
    ["Q8_0"]="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-Q8_0.gguf"
)

# Backup URLs (ModelScope for China region)
declare -A BACKUP_URLS=(
    ["huggingface_backup"]="https://hf-mirror.com/google/functiongemma-270m-it/resolve/main/"
)

# Expected sizes (in MB)
declare -A EXPECTED_SIZES=(
    ["Q4_K_M"]=288
    ["Q5_K_M"]=350
    ["Q8_0"]=520
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
    mkdir -p "$OUTPUT_DIR"
    
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

get_file_size() {
    local file="$1"
    if [[ -f "$file" ]]; then
        stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

format_size() {
    local bytes="$1"
    if [[ $bytes -ge 1073741824 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1073741824}") GB"
    elif [[ $bytes -ge 1048576 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1048576}") MB"
    elif [[ $bytes -ge 1024 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1024}") KB"
    else
        echo "$bytes B"
    fi
}

compute_sha256() {
    local file="$1"
    if [[ -f "$file" ]]; then
        sha256sum "$file" 2>/dev/null | cut -d' ' -f1 || \
        shasum -a 256 "$file" 2>/dev/null | cut -d' ' -f1 || \
        echo "not_computed"
    else
        echo "file_not_found"
    fi
}

check_disk_space() {
    local required_mb="$1"
    local output_dir="$2"
    
    local available
    available=$(df -P "$output_dir" 2>/dev/null | tail -1 | awk '{print $4}' || echo "0")
    local available_mb=$((available / 1024))
    
    if [[ "$available_mb" -lt "$required_mb" ]]; then
        log_error "Insufficient disk space! Need ${required_mb}MB, have ${available_mb}MB"
        return 1
    fi
    
    log_info "Disk space check passed (${available_mb}MB available)"
    return 0
}

# =============================================================================
# Download Functions
# =============================================================================

download_with_curl() {
    local url="$1"
    local output="$2"
    local resume="$3"
    
    local curl_flags=(
        -L
        --progress-bar
        --connect-timeout 30
        --max-time 7200
        --retry 3
        --retry-delay 10
        -o "$output"
    )
    
    if [[ "$resume" == "true" && -f "$output" ]]; then
        curl_flags+=(-C -)
        log_info "Resuming download..."
    fi
    
    curl "${curl_flags[@]}" "$url"
    return $?
}

download_with_wget() {
    local url="$1"
    local output="$2"
    local resume="$3"
    
    local wget_flags=(
        --show-progress
        --progress=bar:force
        --timeout=30
        --tries=3
        --waitretry=10
        -O "$output"
    )
    
    if [[ "$resume" == "true" && -f "$output" ]]; then
        wget_flags+=(--continue)
        log_info "Resuming download..."
    fi
    
    wget "${wget_flags[@]}" "$url"
    return $?
}

download_model() {
    local url="$1"
    local output="$2"
    local resume="$3"
    
    log_step "Downloading FunctionGemma model..."
    log_info "Source: $url"
    log_info "Destination: $output"
    log_info "Expected size: ~${EXPECTED_SIZES[$QUANTIZATION]}MB"
    echo ""
    
    # Try curl first, then wget
    if command -v curl &>/dev/null; then
        log_info "Using curl..."
        if download_with_curl "$url" "$output" "$resume"; then
            return 0
        fi
        log_warn "curl download failed, trying wget..."
    fi
    
    if command -v wget &>/dev/null; then
        log_info "Using wget..."
        if download_with_wget "$url" "$output" "$resume"; then
            return 0
        fi
    fi
    
    log_error "All download attempts failed!"
    return 2
}

# =============================================================================
# Verification Functions
# =============================================================================

verify_model_file() {
    local model_file="$1"
    
    log_step "Verifying model file..."
    
    if [[ ! -f "$model_file" ]]; then
        log_error "Model file not found: $model_file"
        return 3
    fi
    
    local size_bytes
    size_bytes=$(get_file_size "$model_file")
    local size_mb=$((size_bytes / 1024 / 1024))
    local expected_mb="${EXPECTED_SIZES[$QUANTIZATION]:-$EXPECTED_SIZE_MB}"
    
    log_info "Model: $(basename "$model_file")"
    log_info "Size: $(format_size "$size_bytes") ($size_mb MB)"
    log_info "Expected: ~${expected_mb}MB"
    
    # Check minimum size (allow 20% variance)
    local min_size=$((expected_mb * 80 / 100))
    local max_size=$((expected_mb * 120 / 100))
    
    if [[ "$size_mb" -lt "$min_size" ]]; then
        log_error "Model file too small! Expected ~${expected_mb}MB, got ${size_mb}MB"
        log_error "Download may be incomplete or corrupted"
        return 3
    fi
    
    if [[ "$size_mb" -gt "$max_size" ]]; then
        log_warn "Model file larger than expected (${size_mb}MB vs ~${expected_mb}MB)"
    fi
    
    # Check GGUF magic bytes
    local magic
    magic=$(head -c 4 "$model_file" 2>/dev/null || echo "")
    if [[ "$magic" != "GGUF" ]]; then
        log_warn "File may not be a valid GGUF file (magic: $magic)"
    else
        log_info "GGUF magic bytes: verified"
    fi
    
    # Compute checksum
    local checksum
    checksum=$(compute_sha256 "$model_file")
    log_info "SHA256: $checksum"
    
    log_success "Model verification passed"
    return 0
}

verify_existing_model() {
    local model_file="$1"
    
    log_step "Verifying existing model..."
    
    if [[ ! -f "$model_file" ]]; then
        log_error "Model file not found: $model_file"
        return 3
    fi
    
    verify_model_file "$model_file"
    return $?
}

# =============================================================================
# Model Information
# =============================================================================

show_model_info() {
    print_header "FunctionGemma Model Information"
    
    echo "Model:      google/functiongemma-270m-it"
    echo "Format:     GGUF"
    echo "Parameters: 270 million"
    echo "Context:    2048 tokens"
    echo ""
    echo "Quantization Options:"
    echo "  Q4_K_M  - ~288MB (recommended, best size/quality balance)"
    echo "  Q5_K_M  - ~350MB (higher quality)"
    echo "  Q8_0    - ~520MB (near-lossless)"
    echo ""
    echo "Capabilities:"
    echo "  - Egyptian Arabic function calling"
    echo "  - Voice command understanding"
    echo "  - JSON output for function calls"
    echo "  - Optimized for mobile devices"
    echo ""
    echo "Supported Functions:"
    echo "  - call_contact, send_whatsapp, send_voice_message"
    echo "  - set_alarm, read_time, emergency"
    echo "  - open_app, toggle_wifi, toggle_bluetooth"
    echo "  - toggle_flashlight, read_missed_calls"
    echo "  - send_sms, weather_query"
    echo ""
    echo "Download Sources:"
    echo "  - HuggingFace (default)"
    echo "  - ModelScope (for China region)"
    echo ""
}

# =============================================================================
# Help and Usage
# =============================================================================

show_help() {
    cat << EOF
Egyptian Agent - FunctionGemma Model Download Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --local PATH        Use local model file instead of downloading
    --output DIR        Output directory (default: app/src/main/assets/models/)
    --quantization TYPE Quantization type: Q4_K_M, Q5_K_M, Q8_0
                        (default: Q4_K_M)
    --source SOURCE     Download source: huggingface, modelscope
                        (default: huggingface)
    --resume            Resume interrupted download
    --verify PATH       Verify an existing model file
    --info              Show model information
    --log-file PATH     Write download log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    # Download default model (Q4_K_M)
    $SCRIPT_NAME

    # Download with higher quality quantization
    $SCRIPT_NAME --quantization Q8_0

    # Use local model file
    $SCRIPT_NAME --local /path/to/model.gguf

    # Verify existing model
    $SCRIPT_NAME --verify model.gguf

    # Show model information
    $SCRIPT_NAME --info

    # Resume interrupted download
    $SCRIPT_NAME --resume

MODEL INFO:
    Name:       google/functiongemma-270m-it
    Format:     GGUF (Q4_K_M quantized)
    Parameters: 270 million
    Size:       ~288MB (Q4_K_M)
    Context:    2048 tokens

RETURN CODES:
    0   Success
    1   General error
    2   Download failed
    3   Verification failed
    4   Model file not found
    5   Invalid arguments

For more information, see: docs/FUNCTIONGEMMA_QUICKSTART.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --local)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --local requires an argument"
                    return 5
                fi
                LOCAL_MODEL="$2"
                shift 2
                ;;
            --output)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --output requires an argument"
                    return 5
                fi
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --quantization)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --quantization requires an argument"
                    return 5
                fi
                QUANTIZATION="$2"
                if [[ ! "${MODEL_URLS[$QUANTIZATION]+isset}" ]]; then
                    log_error "Invalid quantization type: $QUANTIZATION"
                    log_error "Valid options: Q4_K_M, Q5_K_M, Q8_0"
                    return 5
                fi
                shift 2
                ;;
            --source)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --source requires an argument"
                    return 5
                fi
                DOWNLOAD_SOURCE="$2"
                shift 2
                ;;
            --resume)
                RESUME_DOWNLOAD=true
                shift
                ;;
            --verify)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --verify requires an argument"
                    return 5
                fi
                VERIFY_MODEL="$2"
                shift 2
                ;;
            --info)
                SHOW_INFO=true
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
    print_header "FunctionGemma Model Setup"
    
    echo ""
    log_info "Download Configuration:"
    echo "  Quantization:  $QUANTIZATION"
    echo "  Output Dir:    $OUTPUT_DIR"
    echo "  Source:        $DOWNLOAD_SOURCE"
    echo "  Resume:        $RESUME_DOWNLOAD"
    echo ""
    
    # Handle info mode
    if [[ "$SHOW_INFO" == "true" ]]; then
        show_model_info
        exit 0
    fi
    
    # Handle verify mode
    if [[ -n "$VERIFY_MODEL" ]]; then
        verify_existing_model "$VERIFY_MODEL"
        exit $?
    fi
    
    # Handle local model
    if [[ -n "$LOCAL_MODEL" ]]; then
        if [[ ! -f "$LOCAL_MODEL" ]]; then
            log_error "Local model file not found: $LOCAL_MODEL"
            return 4
        fi
        
        log_step "Using local model..."
        
        # Verify local model
        if ! verify_model_file "$LOCAL_MODEL"; then
            exit $?
        fi
        
        # Copy to output directory
        mkdir -p "$OUTPUT_DIR"
        local output_file="$OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf"
        
        log_info "Copying to: $output_file"
        cp "$LOCAL_MODEL" "$output_file"
        
        log_success "Model copied successfully"
        echo ""
        echo "  Model: $output_file"
        echo "  Size:  $(format_size $(get_file_size "$output_file"))"
        echo ""
        
        return 0
    fi
    
    # Check disk space
    local expected_mb="${EXPECTED_SIZES[$QUANTIZATION]}"
    if ! check_disk_space "$((expected_mb + 100))" "$OUTPUT_DIR"; then
        exit 1
    fi
    
    # Create output directory
    mkdir -p "$OUTPUT_DIR"
    
    # Determine output file path
    local output_file="$OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf"
    
    # Check if model already exists
    if [[ -f "$output_file" ]]; then
        local existing_size
        existing_size=$(get_file_size "$output_file")
        local existing_mb=$((existing_size / 1024 / 1024))
        
        log_info "Model already exists: $output_file"
        log_info "Existing size: ${existing_mb}MB"
        
        if [[ "$existing_mb" -ge "${EXPECTED_SIZES[$QUANTIZATION]}" ]]; then
            echo ""
            if [[ "$CI_MODE" != "true" ]]; then
                read -p "Use existing model? (y/n) " -n 1 -r
                echo ""
            fi
            
            if [[ "$CI_MODE" == "true" || $REPLY =~ ^[Yy]$ ]]; then
                verify_model_file "$output_file"
                exit $?
            fi
        fi
        
        # Remove existing file for fresh download
        if [[ "$RESUME_DOWNLOAD" != "true" ]]; then
            rm -f "$output_file"
            log_info "Removed existing model file"
        fi
    fi
    
    # Get download URL
    local download_url="${MODEL_URLS[$QUANTIZATION]}"
    
    # Download model
    if ! download_model "$download_url" "$output_file" "$RESUME_DOWNLOAD"; then
        log_error ""
        log_error "Download failed!"
        echo ""
        log_error "Manual download instructions:"
        echo "  1. Visit: https://huggingface.co/google/functiongemma-270m-it"
        echo "  2. Download: functiongemma-270m-it-${QUANTIZATION}.gguf"
        echo "  3. Place in: $OUTPUT_DIR/"
        exit 2
    fi
    
    echo ""
    
    # Verify downloaded model
    if ! verify_model_file "$output_file"; then
        exit $?
    fi
    
    # Print summary
    print_header "Download Complete"
    
    log_success "Model downloaded successfully!"
    echo ""
    echo "  Model: $output_file"
    echo "  Size:  $(format_size $(get_file_size "$output_file"))"
    echo "  SHA256: $(compute_sha256 "$output_file")"
    echo ""
    
    log_info "Next steps:"
    echo "  1. Build app: ./scripts/build/build_functiongemma.sh"
    echo "  2. Deploy model: ./scripts/deploy/deploy_functiongemma.sh"
    echo "  3. Test function calling on device"
    echo ""
    
    return 0
}

# Trap errors
trap 'log_error "Download interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
