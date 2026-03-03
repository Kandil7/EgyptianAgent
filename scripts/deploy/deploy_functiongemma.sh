#!/bin/bash
# FunctionGemma Model Deployment Script
# Deploys the FunctionGemma model to Android device at /data/local/llm/
#
# This script:
# 1. Checks for connected Android device
# 2. Creates necessary directories on device
# 3. Pushes the model file to the device
# 4. Sets appropriate permissions
# 5. Verifies the deployment
#
# Usage:
#   ./scripts/deploy_functiongemma.sh [--model PATH] [--device SERIAL]
#
# Options:
#   --model PATH    Path to model file (default: app/src/main/assets/models/)
#   --device SERIAL Target device serial (for multiple devices)
#   --clean         Remove existing model from device first
#   --help          Show help message

set -e  # Exit on error

# ============================================================================
# Configuration
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default paths
MODEL_DIR="$PROJECT_DIR/app/src/main/assets/models"
MODEL_NAME="functiongemma-270m-it-Q4_K_M.gguf"
DEVICE_MODEL_DIR="/data/local/llm"

# ADB command
ADB="adb"

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

check_adb() {
    if ! command -v adb &> /dev/null; then
        log_error "ADB not found! Please install Android SDK platform-tools."
        log_error ""
        log_error "Installation instructions:"
        log_error "  - Windows: Download from https://developer.android.com/studio/releases/platform-tools"
        log_error "  - macOS: brew install android-platform-tools"
        log_error "  - Linux: sudo apt install android-tools-adb"
        exit 1
    fi
    
    log_info "ADB found: $(which adb)"
}

check_device_connection() {
    local device_serial="$1"
    
    log_step "Checking device connection..."
    
    # Get device list
    local devices
    if [ -n "$device_serial" ]; then
        devices=$($ADB -s "$device_serial" devices 2>/dev/null | grep -v "^$" | grep "device$" || echo "")
        if [ -z "$devices" ]; then
            log_error "Device with serial '$device_serial' not found or not connected"
            return 1
        fi
        log_info "Using device: $device_serial"
    else
        local device_count=$($ADB devices 2>/dev/null | grep -c "device$" || echo "0")
        
        if [ "$device_count" -eq 0 ]; then
            log_error "No connected devices found!"
            log_error ""
            log_error "Troubleshooting:"
            log_error "  1. Enable USB debugging on your device"
            log_error "  2. Connect device via USB"
            log_error "  3. Accept the USB debugging prompt on device"
            log_error "  4. Run: adb devices"
            return 1
        fi
        
        if [ "$device_count" -gt 1 ]; then
            log_warn "Multiple devices connected ($device_count)"
            log_warn "Use --device SERIAL to specify target device"
            echo ""
            $ADB devices
            echo ""
        fi
        
        log_info "Found $device_count device(s)"
    fi
    
    # Get device info
    local device_model=$($ADB shell getprop ro.product.model 2>/dev/null || echo "Unknown")
    local device_android=$($ADB shell getprop ro.build.version.release 2>/dev/null || echo "Unknown")
    local device_sdk=$($ADB shell getprop ro.build.version.sdk 2>/dev/null || echo "Unknown")
    
    log_info "Device: $device_model (Android $device_android, SDK $device_sdk)"
    
    return 0
}

get_file_size() {
    local file="$1"
    if [ -f "$file" ]; then
        if command -v stat &> /dev/null; then
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
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1073741824}") GB"
    elif [ "$bytes" -ge 1048576 ]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1048576}") MB"
    elif [ "$bytes" -ge 1024 ]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1024}") KB"
    else
        echo "$bytes B"
    fi
}

check_device_storage() {
    local required_mb="$1"
    
    log_step "Checking device storage..."
    
    # Get available storage on /data partition
    local available=$($ADB shell df /data 2>/dev/null | tail -1 | awk '{print $4}' || echo "0")
    
    if [ "$available" = "0" ] || [ -z "$available" ]; then
        log_warn "Could not determine available storage"
        return 0
    fi
    
    local available_mb=$((available / 1024))
    
    log_info "Available storage: $(format_size $((available * 1024)))"
    
    if [ "$available_mb" -lt "$required_mb" ]; then
        log_error "Insufficient storage! Need ${required_mb}MB, have ${available_mb}MB"
        return 1
    fi
    
    log_info "Storage check passed (need ${required_mb}MB)"
    return 0
}

# ============================================================================
# Deployment Functions
# ============================================================================

create_device_directory() {
    log_step "Creating device directory..."
    
    # Create directory on device
    $ADB shell "mkdir -p $DEVICE_MODEL_DIR"
    
    # Verify directory was created
    if $ADB shell "test -d $DEVICE_MODEL_DIR && echo 'exists'"; then
        log_success "Directory created: $DEVICE_MODEL_DIR"
    else
        log_error "Failed to create directory on device"
        return 1
    fi
    
    return 0
}

clean_device_model() {
    log_step "Removing existing model from device..."
    
    $ADB shell "rm -f $DEVICE_MODEL_DIR/$MODEL_NAME"
    
    log_info "Existing model removed"
    return 0
}

push_model_to_device() {
    local model_file="$1"
    
    log_step "Pushing model to device..."
    
    local model_size=$(get_file_size "$model_file")
    local model_size_mb=$((model_size / 1024 / 1024))
    
    log_info "Model: $(basename "$model_file")"
    log_info "Size: $(format_size $model_size) (${model_size_mb}MB)"
    log_info "Destination: $DEVICE_MODEL_DIR/"
    echo ""
    
    # Push file with progress
    if command -v pv &> /dev/null; then
        # Use pv for progress if available
        pv "$model_file" | $ADB shell "cat > $DEVICE_MODEL_DIR/$MODEL_NAME"
    else
        # Standard push
        $ADB push "$model_file" "$DEVICE_MODEL_DIR/$MODEL_NAME"
    fi
    
    # Verify file was pushed
    local remote_size=$($ADB shell "ls -l $DEVICE_MODEL_DIR/$MODEL_NAME 2>/dev/null | awk '{print \$5}'" || echo "0")
    
    if [ "$remote_size" = "0" ] || [ -z "$remote_size" ]; then
        log_error "Failed to verify model on device"
        return 1
    fi
    
    log_info "Remote size: $(format_size $remote_size)"
    
    # Compare sizes
    if [ "$remote_size" != "$model_size" ]; then
        local diff=$((model_size - remote_size))
        if [ "$diff" -lt 0 ]; then
            diff=$((-diff))
        fi
        
        # Allow small differences due to filesystem overhead
        if [ "$diff" -gt 1024 ]; then
            log_warn "Size mismatch! Local: $model_size, Remote: $remote_size"
        fi
    fi
    
    log_success "Model pushed successfully"
    return 0
}

set_permissions() {
    log_step "Setting permissions..."
    
    # Set readable permissions
    $ADB shell "chmod 644 $DEVICE_MODEL_DIR/$MODEL_NAME"
    
    # Set ownership (if possible)
    $ADB shell "chown shell:shell $DEVICE_MODEL_DIR/$MODEL_NAME" 2>/dev/null || true
    
    log_info "Permissions set: 644 (rw-r--r--)"
    return 0
}

verify_deployment() {
    log_step "Verifying deployment..."
    
    # Check file exists
    if ! $ADB shell "test -f $DEVICE_MODEL_DIR/$MODEL_NAME && echo 'exists'"; then
        log_error "Model file not found on device!"
        return 1
    fi
    
    # Get file info
    local file_info=$($ADB shell "ls -lh $DEVICE_MODEL_DIR/$MODEL_NAME" 2>/dev/null)
    log_info "File info: $file_info"
    
    # Check file size
    local remote_size=$($ADB shell "ls -l $DEVICE_MODEL_DIR/$MODEL_NAME 2>/dev/null | awk '{print \$5}'" || echo "0")
    local remote_size_mb=$((remote_size / 1024 / 1024))
    
    if [ "$remote_size_mb" -lt 200 ]; then
        log_error "Model file too small on device! Expected ~288MB, got ${remote_size_mb}MB"
        return 1
    fi
    
    # Check file permissions
    local perms=$($ADB shell "ls -l $DEVICE_MODEL_DIR/$MODEL_NAME 2>/dev/null | awk '{print \$1}'" || echo "")
    log_info "Permissions: $perms"
    
    # Quick integrity check (first 4 bytes should be "GGUF")
    local magic=$($ADB shell "head -c 4 $DEVICE_MODEL_DIR/$MODEL_NAME" 2>/dev/null || echo "")
    if [ "$magic" != "GGUF" ]; then
        log_warn "File magic check failed (expected 'GGUF', got '$magic')"
        log_warn "This may indicate a corrupted transfer"
    else
        log_info "GGUF magic bytes verified"
    fi
    
    log_success "Deployment verification passed"
    return 0
}

test_model_load() {
    log_step "Testing model accessibility..."
    
    # Try to read first few bytes to verify accessibility
    local test_read=$($ADB shell "head -c 100 $DEVICE_MODEL_DIR/$MODEL_NAME | wc -c" 2>/dev/null || echo "0")
    
    if [ "$test_read" -lt 100 ]; then
        log_warn "Could not read model file for testing"
        return 1
    fi
    
    log_info "Model file is accessible and readable"
    return 0
}

# ============================================================================
# Main Functions
# ============================================================================

deploy_model() {
    local model_file="$1"
    local clean_first="$2"
    
    print_header "Deploying FunctionGemma Model"
    
    # Check ADB
    check_adb
    
    # Check device connection
    if ! check_device_connection "$DEVICE_SERIAL"; then
        exit 1
    fi
    
    # Verify local model exists
    if [ ! -f "$model_file" ]; then
        log_error "Model file not found: $model_file"
        log_error ""
        log_error "Download the model first:"
        log_error "  ./scripts/download_functiongemma_model.sh"
        exit 1
    fi
    
    # Get model size for storage check
    local model_size=$(get_file_size "$model_file")
    local model_size_mb=$((model_size / 1024 / 1024))
    
    # Check device storage
    if ! check_device_storage "$((model_size_mb + 100))"; then
        exit 1
    fi
    
    # Clean if requested
    if [ "$clean_first" = true ]; then
        clean_device_model
    fi
    
    # Create directory
    if ! create_device_directory; then
        exit 1
    fi
    
    # Push model
    if ! push_model_to_device "$model_file"; then
        exit 1
    fi
    
    # Set permissions
    set_permissions
    
    # Verify deployment
    if ! verify_deployment; then
        exit 1
    fi
    
    # Test accessibility
    test_model_load
    
    echo ""
    print_header "Deployment Complete"
    
    echo "Model deployed to: $DEVICE_MODEL_DIR/$MODEL_NAME"
    echo "Model size: $(format_size $model_size)"
    echo ""
    echo "To use in your app, configure the model path:"
    echo "  FunctionGemmaConfig.builder()"
    echo "      .modelPath(\"$DEVICE_MODEL_DIR/$MODEL_NAME\")"
    echo "      .build();"
    echo ""
    
    return 0
}

show_device_info() {
    print_header "Device Information"
    
    echo "Connected devices:"
    $ADB devices
    echo ""
    
    if [ -n "$DEVICE_SERIAL" ]; then
        $ADB -s "$DEVICE_SERIAL" shell "
            echo 'Device Details:'
            echo '  Model: \$(getprop ro.product.model)'
            echo '  Manufacturer: \$(getprop ro.product.manufacturer)'
            echo '  Android Version: \$(getprop ro.build.version.release)'
            echo '  SDK Version: \$(getprop ro.build.version.sdk)'
            echo '  ABI: \$(getprop ro.product.cpu.abi)'
            echo ''
            echo 'Storage:'
            df -h /data 2>/dev/null | head -2
            echo ''
            echo 'Memory:'
            cat /proc/meminfo | head -3
        "
    else
        $ADB shell "
            echo 'Device Details:'
            echo '  Model: \$(getprop ro.product.model)'
            echo '  Manufacturer: \$(getprop ro.product.manufacturer)'
            echo '  Android Version: \$(getprop ro.build.version.release)'
            echo '  SDK Version: \$(getprop ro.build.version.sdk)'
            echo '  ABI: \$(getprop ro.product.cpu.abi)'
            echo ''
            echo 'Storage:'
            df -h /data 2>/dev/null | head -2
            echo ''
            echo 'Memory:'
            cat /proc/meminfo | head -3
        "
    fi
}

show_help() {
    echo "FunctionGemma Model Deployment Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --model PATH    Path to model file"
    echo "                  (default: app/src/main/assets/models/functiongemma-270m-it-Q4_K_M.gguf)"
    echo "  --device SERIAL Target device serial (for multiple devices)"
    echo "  --clean         Remove existing model from device first"
    echo "  --info          Show device information"
    echo "  --verify        Verify existing deployment without re-uploading"
    echo "  --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Deploy default model"
    echo "  $0 --clean                            # Clean deploy"
    echo "  $0 --model /path/to/model.gguf        # Custom model path"
    echo "  $0 --device ABC123                    # Target specific device"
    echo "  $0 --info                             # Show device info"
    echo "  $0 --verify                           # Verify deployment"
    echo ""
    echo "Manual deployment:"
    echo "  1. adb shell mkdir -p /data/local/llm"
    echo "  2. adb push model.gguf /data/local/llm/"
    echo "  3. adb shell chmod 644 /data/local/llm/model.gguf"
    echo ""
}

# ============================================================================
# Main
# ============================================================================

main() {
    print_header "FunctionGemma Deployment Script"
    
    # Parse command line arguments
    MODEL_FILE=""
    CLEAN_FIRST=false
    SHOW_INFO=false
    VERIFY_ONLY=false
    DEVICE_SERIAL=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --model)
                MODEL_FILE="$2"
                shift 2
                ;;
            --device)
                DEVICE_SERIAL="$2"
                shift 2
                ;;
            --clean)
                CLEAN_FIRST=true
                shift
                ;;
            --info)
                SHOW_INFO=true
                shift
                ;;
            --verify)
                VERIFY_ONLY=true
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
    
    # Check ADB first
    check_adb
    
    # Handle special modes
    if [ "$SHOW_INFO" = true ]; then
        show_device_info
        exit 0
    fi
    
    if [ "$VERIFY_ONLY" = true ]; then
        if ! check_device_connection "$DEVICE_SERIAL"; then
            exit 1
        fi
        verify_deployment
        exit $?
    fi
    
    # Set default model path if not specified
    if [ -z "$MODEL_FILE" ]; then
        MODEL_FILE="$MODEL_DIR/$MODEL_NAME"
    fi
    
    # Deploy the model
    deploy_model "$MODEL_FILE" "$CLEAN_FIRST"
    
    log_success "Deployment completed successfully!"
    
    echo ""
    echo "Next steps:"
    echo "  1. Install the app: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "  2. Launch the app and test function calling"
    echo "  3. Monitor logs: adb logcat | grep FunctionGemma"
    echo ""
}

# Run main function
main "$@"
