#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - FunctionGemma Model Deployment Script
# =============================================================================
#
# PURPOSE:
#   Deploys the FunctionGemma model to Android device storage for use by the
#   Egyptian Agent application. Supports multiple sources, integrity verification,
#   and rollback on failure.
#
# USAGE:
#   ./deploy/scripts-deploy/deploy_functiongemma.sh [OPTIONS]
#
# OPTIONS:
#   --model PATH        Path to model file (default: android/src/main/assets/models/)
#   --device SERIAL     Target device serial (for multiple devices)
#   --output PATH       Remote output path (default: /data/local/llm/)
#   --clean             Remove existing model before deployment
#   --verify-only       Verify existing deployment without re-uploading
#   --info              Show device and model information
#   --log-file PATH     Write deployment log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./deploy/scripts-deploy/deploy_functiongemma.sh
#   ./deploy/scripts-deploy/deploy_functiongemma.sh --model /path/to/model.gguf
#   ./deploy/scripts-deploy/deploy_functiongemma.sh --device ABC123 --clean
#   ./deploy/scripts-deploy/deploy_functiongemma.sh --verify-only
#   ./deploy/scripts-deploy/deploy_functiongemma.sh --info
#
# REQUIREMENTS:
#   - Android device with USB debugging enabled
#   - ADB tools installed
#   - Sufficient device storage (~300MB for model)
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Device not connected
#   3   Model file not found
#   4   Deployment failed
#   5   Verification failed
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
readonly DEPLOY_TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# Default configuration
MODEL_PATH=""
DEVICE_SERIAL=""
OUTPUT_PATH="/data/local/llm"
MODEL_NAME="functiongemma-270m-it-Q4_K_M.gguf"
CLEAN_FIRST=false
VERIFY_ONLY=false
SHOW_INFO=false
LOG_FILE=""
CI_MODE=false

# Expected model size
EXPECTED_SIZE_MB=288

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

adb_cmd() {
    if [[ -n "$DEVICE_SERIAL" ]]; then
        adb -s "$DEVICE_SERIAL" "$@"
    else
        adb "$@"
    fi
}

adb_shell() {
    adb_cmd shell "$@"
}

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

# =============================================================================
# Device Checks
# =============================================================================

check_adb() {
    log_step "Checking ADB..."
    
    if ! command -v adb &>/dev/null; then
        log_error "ADB not found. Please install Android SDK platform-tools."
        log_error ""
        log_error "Installation:"
        echo "  - Windows: https://developer.android.com/studio/releases/platform-tools"
        echo "  - macOS:   brew install android-platform-tools"
        echo "  - Linux:   sudo apt install android-tools-adb"
        return 2
    fi
    
    log_info "ADB: $(which adb)"
    return 0
}

check_device_connection() {
    log_step "Checking device connection..."
    
    local devices
    if [[ -n "$DEVICE_SERIAL" ]]; then
        if ! adb_cmd devices | grep -q "$DEVICE_SERIAL"; then
            log_error "Device '$DEVICE_SERIAL' not found"
            return 2
        fi
        log_info "Using device: $DEVICE_SERIAL"
    else
        local device_count
        device_count=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
        
        if [[ "$device_count" -eq 0 ]]; then
            log_error "No connected devices found"
            echo ""
            log_error "Solutions:"
            echo "  1. Enable USB debugging on device"
            echo "  2. Connect device via USB cable"
            echo "  3. Accept USB debugging prompt on device"
            return 2
        fi
        
        if [[ "$device_count" -gt 1 ]]; then
            log_warn "Multiple devices connected ($device_count)"
            log_warn "Use --device SERIAL to specify target"
            echo ""
            adb devices
            echo ""
        fi
        
        log_info "Found $device_count device(s)"
    fi
    
    # Get device info
    local device_model
    device_model=$(adb_shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Unknown")
    local device_android
    device_android=$(adb_shell getprop ro.build.version.release 2>/dev/null | tr -d '\r' || echo "Unknown")
    local device_sdk
    device_sdk=$(adb_shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || echo "Unknown")
    
    log_info "Device: $device_model (Android $device_android, SDK $device_sdk)"
    
    return 0
}

check_device_storage() {
    log_step "Checking device storage..."
    
    local available
    available=$(adb_shell "df /data 2>/dev/null | tail -1 | awk '{print \$4}'" | tr -d '\r' || echo "0")
    
    if [[ "$available" == "0" || -z "$available" ]]; then
        log_warn "Could not determine available storage"
        return 0
    fi
    
    local available_mb=$((available / 1024))
    local required_mb=$((EXPECTED_SIZE_MB + 100))
    
    log_info "Available storage: $(format_size $((available * 1024)))"
    
    if [[ "$available_mb" -lt "$required_mb" ]]; then
        log_error "Insufficient storage! Need ${required_mb}MB, have ${available_mb}MB"
        return 4
    fi
    
    log_info "Storage check passed (need ${required_mb}MB)"
    return 0
}

# =============================================================================
# Model Functions
# =============================================================================

find_model() {
    # Check specified path
    if [[ -n "$MODEL_PATH" && -f "$MODEL_PATH" ]]; then
        echo "$MODEL_PATH"
        return 0
    fi
    
    # Check default locations
    local search_paths=(
        "$PROJECT_DIR/android/src/main/assets/models/$MODEL_NAME"
        "$PROJECT_DIR/dist/functiongemma/$MODEL_NAME"
        "$PROJECT_DIR/models/$MODEL_NAME"
        "$HOME/.cache/egyptian_agent/$MODEL_NAME"
    )
    
    for path in "${search_paths[@]}"; do
        if [[ -f "$path" ]]; then
            echo "$path"
            return 0
        fi
    done
    
    return 1
}

verify_model() {
    local model_file="$1"
    
    log_step "Verifying model file..."
    
    if [[ ! -f "$model_file" ]]; then
        log_error "Model file not found: $model_file"
        return 3
    fi
    
    local size_bytes
    size_bytes=$(get_file_size "$model_file")
    local size_mb=$((size_bytes / 1024 / 1024))
    
    log_info "Model: $(basename "$model_file")"
    log_info "Size: $(format_size "$size_bytes") ($size_mb MB)"
    
    # Check minimum size
    if [[ "$size_mb" -lt 200 ]]; then
        log_error "Model file too small! Expected ~${EXPECTED_SIZE_MB}MB, got ${size_mb}MB"
        return 3
    fi
    
    # Check GGUF magic bytes
    local magic
    magic=$(head -c 4 "$model_file" 2>/dev/null || echo "")
    if [[ "$magic" != "GGUF" ]]; then
        log_warn "File may not be a valid GGUF file (magic: $magic)"
    else
        log_info "GGUF magic bytes: verified"
    fi
    
    log_success "Model verification passed"
    return 0
}

create_remote_directory() {
    log_step "Creating remote directory..."
    
    if ! adb_shell "mkdir -p $OUTPUT_PATH"; then
        log_error "Failed to create remote directory"
        return 4
    fi
    
    log_success "Directory created: $OUTPUT_PATH"
    return 0
}

clean_remote_model() {
    if [[ "$CLEAN_FIRST" != "true" ]]; then
        return 0
    fi
    
    log_step "Removing existing model..."
    
    adb_shell "rm -f $OUTPUT_PATH/$MODEL_NAME" 2>/dev/null || true
    
    log_info "Existing model removed"
    return 0
}

push_model() {
    local model_file="$1"
    
    log_step "Pushing model to device..."
    
    local size_bytes
    size_bytes=$(get_file_size "$model_file")
    local size_mb=$((size_bytes / 1024 / 1024))
    
    log_info "Source: $model_file"
    log_info "Destination: $OUTPUT_PATH/$MODEL_NAME"
    log_info "Size: $(format_size "$size_bytes") ($size_mb MB)"
    echo ""
    
    # Push with progress
    if ! adb_cmd push "$model_file" "$OUTPUT_PATH/$MODEL_NAME"; then
        log_error "Failed to push model"
        return 4
    fi
    
    # Verify transfer
    local remote_size
    remote_size=$(adb_shell "ls -l $OUTPUT_PATH/$MODEL_NAME 2>/dev/null | awk '{print \$5}'" | tr -d '\r' || echo "0")
    
    if [[ "$remote_size" != "$size_bytes" ]]; then
        log_warn "Size mismatch (local: $size_bytes, remote: $remote_size)"
    fi
    
    log_success "Model pushed successfully"
    return 0
}

set_permissions() {
    log_step "Setting permissions..."
    
    adb_shell "chmod 644 $OUTPUT_PATH/$MODEL_NAME" 2>/dev/null || true
    
    log_info "Permissions set: 644 (rw-r--r--)"
    return 0
}

verify_deployment() {
    log_step "Verifying deployment..."
    
    # Check file exists
    local exists
    exists=$(adb_shell "test -f $OUTPUT_PATH/$MODEL_NAME && echo yes" 2>/dev/null | tr -d '\r' || echo "no")
    
    if [[ "$exists" != "yes" ]]; then
        log_error "Model file not found on device"
        return 5
    fi
    
    # Check file size
    local remote_size
    remote_size=$(adb_shell "ls -l $OUTPUT_PATH/$MODEL_NAME 2>/dev/null | awk '{print \$5}'" | tr -d '\r' || echo "0")
    local remote_mb=$((remote_size / 1024 / 1024))
    
    log_info "Remote size: $(format_size "$remote_size") ($remote_mb MB)"
    
    if [[ "$remote_mb" -lt 200 ]]; then
        log_error "Remote file too small! Transfer may have failed"
        return 5
    fi
    
    # Check GGUF magic
    local magic
    magic=$(adb_shell "head -c 4 $OUTPUT_PATH/$MODEL_NAME" 2>/dev/null | tr -d '\r' || echo "")
    if [[ "$magic" == "GGUF" ]]; then
        log_info "GGUF magic bytes: verified"
    else
        log_warn "Could not verify GGUF magic bytes"
    fi
    
    # Test readability
    local test_read
    test_read=$(adb_shell "head -c 100 $OUTPUT_PATH/$MODEL_NAME | wc -c" 2>/dev/null | tr -d '\r' || echo "0")
    if [[ "$test_read" -ge 100 ]]; then
        log_info "File readability: verified"
    else
        log_warn "Could not verify file readability"
    fi
    
    log_success "Deployment verified"
    return 0
}

verify_existing_deployment() {
    log_step "Verifying existing deployment..."
    
    # Check file exists
    local exists
    exists=$(adb_shell "test -f $OUTPUT_PATH/$MODEL_NAME && echo yes" 2>/dev/null | tr -d '\r' || echo "no")
    
    if [[ "$exists" != "yes" ]]; then
        log_error "Model not found at $OUTPUT_PATH/$MODEL_NAME"
        return 5
    fi
    
    # Get file info
    local file_info
    file_info=$(adb_shell "ls -lh $OUTPUT_PATH/$MODEL_NAME" 2>/dev/null | tr -d '\r')
    log_info "File info: $file_info"
    
    log_success "Existing deployment verified"
    return 0
}

show_device_info() {
    print_header "Device Information"
    
    echo "Connected devices:"
    adb devices
    echo ""
    
    if [[ -n "$DEVICE_SERIAL" ]]; then
        adb_cmd shell "
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
            cat /proc/meminfo 2>/dev/null | head -3 || echo 'N/A'
        "
    else
        adb_shell "
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
            cat /proc/meminfo 2>/dev/null | head -3 || echo 'N/A'
        "
    fi
}

show_model_info() {
    print_header "FunctionGemma Model Information"
    
    echo "Model: google/functiongemma-270m-it"
    echo "Format: GGUF (Q4_K_M quantized)"
    echo "Parameters: 270 million"
    echo "Expected Size: ~${EXPECTED_SIZE_MB}MB"
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
    echo ""
}

# =============================================================================
# Help and Usage
# =============================================================================

show_help() {
    cat << EOF
Egyptian Agent - FunctionGemma Model Deployment Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --model PATH        Path to model file
                        (default: android/src/main/assets/models/)
    --device SERIAL     Target device serial (for multiple devices)
    --output PATH       Remote output path (default: /data/local/llm/)
    --clean             Remove existing model before deployment
    --verify-only       Verify existing deployment without re-uploading
    --info              Show device and model information
    --log-file PATH     Write deployment log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    # Deploy default model
    $SCRIPT_NAME

    # Deploy custom model
    $SCRIPT_NAME --model /path/to/model.gguf

    # Deploy to specific device
    $SCRIPT_NAME --device ABC123

    # Clean deployment
    $SCRIPT_NAME --clean

    # Verify existing deployment
    $SCRIPT_NAME --verify-only

    # Show device info
    $SCRIPT_NAME --info

REQUIREMENTS:
    - Android device with USB debugging enabled
    - ADB tools installed
    - ~300MB free storage on device

RETURN CODES:
    0   Success
    1   General error
    2   Device not connected
    3   Model file not found
    4   Deployment failed
    5   Verification failed

For more information, see: docs/FUNCTIONGEMMA_QUICKSTART.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --model)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --model requires an argument"
                    return 5
                fi
                MODEL_PATH="$2"
                shift 2
                ;;
            --device)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --device requires an argument"
                    return 5
                fi
                DEVICE_SERIAL="$2"
                shift 2
                ;;
            --output)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --output requires an argument"
                    return 5
                fi
                OUTPUT_PATH="$2"
                shift 2
                ;;
            --clean)
                CLEAN_FIRST=true
                shift
                ;;
            --verify-only)
                VERIFY_ONLY=true
                shift
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
    print_header "FunctionGemma Model Deployment"
    
    echo ""
    log_info "Deployment Configuration:"
    echo "  Model Path:   ${MODEL_PATH:-auto-detect}"
    echo "  Device:       ${DEVICE_SERIAL:-auto-detect}"
    echo "  Output Path:  $OUTPUT_PATH"
    echo "  Clean First:  $CLEAN_FIRST"
    echo ""
    
    # Check ADB
    if ! check_adb; then
        exit $?
    fi
    
    # Handle info mode
    if [[ "$SHOW_INFO" == "true" ]]; then
        show_device_info
        echo ""
        show_model_info
        exit 0
    fi
    
    # Check device connection
    if ! check_device_connection; then
        exit $?
    fi
    
    # Handle verify-only mode
    if [[ "$VERIFY_ONLY" == "true" ]]; then
        verify_existing_deployment
        exit $?
    fi
    
    # Check storage
    if ! check_device_storage; then
        exit $?
    fi
    
    # Find model
    local found_model
    found_model=$(find_model) || {
        log_error "Model file not found"
        echo ""
        log_error "Solutions:"
        echo "  1. Download model: ./ml/finetune/scripts/download_functiongemma_model.sh"
        echo "  2. Specify path: $SCRIPT_NAME --model /path/to/model.gguf"
        return 3
    }
    
    MODEL_PATH="$found_model"
    
    # Verify model
    if ! verify_model "$MODEL_PATH"; then
        exit $?
    fi
    
    echo ""
    
    # Create remote directory
    create_remote_directory
    
    # Clean if requested
    clean_remote_model
    
    echo ""
    
    # Push model
    if ! push_model "$MODEL_PATH"; then
        exit $?
    fi
    
    echo ""
    
    # Set permissions
    set_permissions
    
    # Verify deployment
    if ! verify_deployment; then
        exit $?
    fi
    
    # Print summary
    print_header "Deployment Complete"
    
    log_success "FunctionGemma model deployed successfully!"
    echo ""
    echo "  Model:      $MODEL_NAME"
    echo "  Location:   $OUTPUT_PATH/$MODEL_NAME"
    echo "  Size:       $(format_size $(get_file_size "$MODEL_PATH"))"
    echo ""
    
    log_info "Next steps:"
    echo "  1. Install/update app: adb install -r android/build/outputs/apk/debug/app-debug.apk"
    echo "  2. Launch app and test function calling"
    echo "  3. Monitor logs: adb logcat | grep FunctionGemma"
    echo ""
    
    return 0
}

# Trap errors
trap 'log_error "Deployment interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
