#!/bin/bash
# FunctionGemma Model Download Script
# Downloads the FunctionGemma-270M-IT model from HuggingFace
# or uses a local fine-tuned model if available.
#
# Model: google/functiongemma-270m-it (GGUF Q4_K_M quantized)
# Size: ~288MB
# Format: GGUF (for llama.cpp compatibility)
#
# Usage:
#   ./scripts/download_functiongemma_model.sh [--local PATH] [--output DIR]
#
# Options:
#   --local PATH    Use local model file instead of downloading
#   --output DIR    Output directory (default: app/src/main/assets/models/)
#   --help          Show help message

set -e  # Exit on error

# ============================================================================
# Configuration
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default model configuration
MODEL_NAME="functiongemma-270m-it-Q4_K_M.gguf"
MODEL_URL="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-Q4_K_M.gguf"
MODEL_URL_BACKUP="https://cdn.huggingface.co/google/functiongemma-270m-it/functiongemma-270m-it-Q4_K_M.gguf"

# Alternative quantization options
MODEL_URL_Q8="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-Q8_0.gguf"
MODEL_URL_FP16="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-FP16.gguf"

# Expected model size (Q4_K_M)
EXPECTED_SIZE_MB=288
EXPECTED_SIZE_BYTES=$((EXPECTED_SIZE_MB * 1024 * 1024))

# Output directory
OUTPUT_DIR="$PROJECT_DIR/app/src/main/assets/models"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================================================
# Logging Functions
# ============================================================================

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

log_success() {
    echo -e "${CYAN}[SUCCESS]${NC} $1"
}

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# ============================================================================
# Helper Functions
# ============================================================================

check_prerequisites() {
    log_step "Checking prerequisites..."
    
    # Check for required tools
    local missing_tools=()
    
    if ! command -v curl &> /dev/null && ! command -v wget &> /dev/null; then
        missing_tools+=("curl or wget")
    fi
    
    if [ ${#missing_tools[@]} -gt 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install the missing tools and try again."
        exit 1
    fi
    
    log_info "Prerequisites check passed"
}

get_file_size() {
    local file="$1"
    if [ -f "$file" ]; then
        if command -v stat &> /dev/null; then
            # Linux/macOS
            stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null || echo "0"
        else
            echo "0"
        fi
    else
        echo "0"
    fi
}

format_size() {
    local bytes=$1
    if [ "$bytes" -ge 1073741824 ]; then
        echo "$(echo "scale=2; $bytes / 1073741824" | bc) GB"
    elif [ "$bytes" -ge 1048576 ]; then
        echo "$(echo "scale=2; $bytes / 1048576" | bc) MB"
    elif [ "$bytes" -ge 1024 ]; then
        echo "$(echo "scale=2; $bytes / 1024" | bc) KB"
    else
        echo "$bytes B"
    fi
}

verify_model() {
    local model_file="$1"
    
    if [ ! -f "$model_file" ]; then
        log_error "Model file not found: $model_file"
        return 1
    fi
    
    local size=$(get_file_size "$model_file")
    local size_mb=$((size / 1024 / 1024))
    
    log_info "Verifying model file..."
    log_info "  File: $(basename "$model_file")"
    log_info "  Size: $(format_size $size) ($size_mb MB)"
    
    # Check minimum size (should be at least 200MB for Q4_K_M)
    if [ "$size_mb" -lt 200 ]; then
        log_error "Model file too small! Expected ~${EXPECTED_SIZE_MB}MB, got ${size_mb}MB"
        log_error "The download may have been corrupted or incomplete."
        return 1
    fi
    
    # Check file extension
    if [[ ! "$model_file" =~ \.gguf$ ]]; then
        log_warn "Model file does not have .gguf extension"
    fi
    
    # Try to read GGUF magic number (first 4 bytes should be "GGUF")
    local magic=$(head -c 4 "$model_file" 2>/dev/null || echo "")
    if [ "$magic" != "GGUF" ]; then
        log_warn "File does not appear to be a valid GGUF file"
        log_warn "Magic bytes: $magic (expected: GGUF)"
    fi
    
    log_success "Model verification passed"
    return 0
}

download_with_curl() {
    local url="$1"
    local output="$2"
    
    log_info "Downloading with curl..."
    curl -L \
        --progress-bar \
        --connect-timeout 30 \
        --max-time 3600 \
        --retry 3 \
        --retry-delay 10 \
        -o "$output" \
        "$url"
    
    return $?
}

download_with_wget() {
    local url="$1"
    local output="$2"
    
    log_info "Downloading with wget..."
    wget --show-progress \
        --progress=bar:force \
        --timeout=30 \
        --tries=3 \
        --waitretry=10 \
        -O "$output" \
        "$url"
    
    return $?
}

download_model() {
    local url="$1"
    local output="$2"
    
    log_step "Downloading FunctionGemma model..."
    log_info "Source: $url"
    log_info "Destination: $output"
    log_info "Expected size: ~${EXPECTED_SIZE_MB}MB"
    echo ""
    
    # Try curl first, then wget
    if command -v curl &> /dev/null; then
        download_with_curl "$url" "$output"
        local result=$?
        if [ $result -eq 0 ]; then
            return 0
        fi
        log_warn "curl download failed, trying wget..."
    fi
    
    if command -v wget &> /dev/null; then
        download_with_wget "$url" "$output"
        return $?
    fi
    
    log_error "No download tool available (curl or wget required)"
    return 1
}

# ============================================================================
# Main Functions
# ============================================================================

download_from_huggingface() {
    print_header "Downloading FunctionGemma Model"
    
    # Create output directory
    mkdir -p "$OUTPUT_DIR"
    
    local model_path="$OUTPUT_DIR/$MODEL_NAME"
    
    # Check if model already exists
    if [ -f "$model_path" ]; then
        local existing_size=$(get_file_size "$model_path")
        local existing_size_mb=$((existing_size / 1024 / 1024))
        
        log_info "Model already exists: $model_path"
        log_info "Existing size: ${existing_size_mb}MB"
        
        if [ "$existing_size_mb" -ge 200 ]; then
            echo ""
            read -p "Use existing model? (y/n) " -n 1 -r
            echo ""
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                verify_model "$model_path"
                return $?
            fi
        fi
        
        # Remove existing file
        rm -f "$model_path"
        log_info "Removed existing model file"
    fi
    
    # Download model
    if ! download_model "$MODEL_URL" "$model_path"; then
        log_warn "Primary download failed, trying backup URL..."
        if ! download_model "$MODEL_URL_BACKUP" "$model_path"; then
            log_error "All download attempts failed!"
            log_error ""
            log_error "Manual download instructions:"
            log_error "  1. Visit: https://huggingface.co/google/functiongemma-270m-it"
            log_error "  2. Download: functiongemma-270m-it-Q4_K_M.gguf"
            log_error "  3. Place in: $OUTPUT_DIR/"
            return 1
        fi
    fi
    
    # Verify downloaded model
    verify_model "$model_path"
    
    echo ""
    log_success "Model downloaded successfully!"
    echo ""
    echo "Model location: $model_path"
    echo "Model size: $(format_size $(get_file_size "$model_path"))"
    
    return 0
}

use_local_model() {
    local local_path="$1"
    
    print_header "Using Local Model"
    
    if [ ! -f "$local_path" ]; then
        log_error "Local model file not found: $local_path"
        return 1
    fi
    
    # Verify the model
    if ! verify_model "$local_path"; then
        return 1
    fi
    
    # Create output directory
    mkdir -p "$OUTPUT_DIR"
    
    # Copy to assets directory
    local dest_path="$OUTPUT_DIR/$MODEL_NAME"
    log_step "Copying model to assets directory..."
    cp "$local_path" "$dest_path"
    
    log_success "Model copied to: $dest_path"
    echo ""
    echo "Model location: $dest_path"
    echo "Model size: $(format_size $(get_file_size "$dest_path"))"
    
    return 0
}

show_model_info() {
    print_header "FunctionGemma Model Information"
    
    echo "Model: google/functiongemma-270m-it"
    echo "Format: GGUF (Q4_K_M quantized)"
    echo "Parameters: 270 million"
    echo "Size: ~${EXPECTED_SIZE_MB}MB"
    echo "Context: 2048 tokens"
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
    echo "  - greeting, thank_you, goodbye"
    echo ""
}

show_help() {
    echo "FunctionGemma Model Download Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --local PATH    Use local model file instead of downloading"
    echo "  --output DIR    Output directory (default: app/src/main/assets/models/)"
    echo "  --info          Show model information"
    echo "  --verify PATH   Verify an existing model file"
    echo "  --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Download from HuggingFace"
    echo "  $0 --local /path/to/model.gguf        # Use local model"
    echo "  $0 --output /custom/path/             # Custom output directory"
    echo "  $0 --verify model.gguf                # Verify existing model"
    echo "  $0 --info                             # Show model info"
    echo ""
    echo "Manual Download:"
    echo "  1. Visit: https://huggingface.co/google/functiongemma-270m-it"
    echo "  2. Download: functiongemma-270m-it-Q4_K_M.gguf"
    echo "  3. Place in: app/src/main/assets/models/"
    echo ""
}

# ============================================================================
# Main
# ============================================================================

main() {
    print_header "FunctionGemma Model Setup"
    
    # Parse command line arguments
    LOCAL_MODEL=""
    VERIFY_MODEL=""
    SHOW_INFO=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --local)
                LOCAL_MODEL="$2"
                shift 2
                ;;
            --output)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --verify)
                VERIFY_MODEL="$2"
                shift 2
                ;;
            --info)
                SHOW_INFO=true
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Handle special modes
    if [ "$SHOW_INFO" = true ]; then
        show_model_info
        exit 0
    fi
    
    if [ -n "$VERIFY_MODEL" ]; then
        verify_model "$VERIFY_MODEL"
        exit $?
    fi
    
    # Check prerequisites
    check_prerequisites
    
    # Use local model or download
    if [ -n "$LOCAL_MODEL" ]; then
        use_local_model "$LOCAL_MODEL"
    else
        download_from_huggingface
    fi
    
    echo ""
    log_success "Model setup completed!"
    echo ""
    echo "Next steps:"
    echo "  1. Build the app: ./build_functiongemma.sh"
    echo "  2. Deploy to device: ./scripts/deploy_functiongemma.sh"
    echo "  3. Run the app and test function calling"
    echo ""
}

# Run main function
main "$@"
