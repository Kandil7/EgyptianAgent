#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - FunctionGemma Build Script
# =============================================================================
#
# PURPOSE:
#   Builds the FunctionGemma variant of the Egyptian Agent application.
#   FunctionGemma is optimized for function calling in Egyptian Arabic dialect.
#
# USAGE:
#   ./scripts/build/build_functiongemma.sh [OPTIONS]
#
# OPTIONS:
#   --release           Build release APK (default: debug)
#   --debug             Build debug APK
#   --clean             Clean build artifacts before building
#   --native            Build with native llama.cpp libraries
#   --install           Install APK on connected device after build
#   --output DIR        Output directory for APK (default: dist/functiongemma)
#   --model PATH        Path to FunctionGemma model file
#   --parallel JOBS     Number of parallel build jobs (default: auto)
#   --verbose           Enable verbose build output
#   --log-file PATH     Write build log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/build/build_functiongemma.sh --release --clean
#   ./scripts/build/build_functiongemma.sh --native --install
#   ./scripts/build/build_functiongemma.sh --model /path/to/model.gguf
#   ./scripts/build/build_functiongemma.sh --ci --log-file build.log
#
# ENVIRONMENT VARIABLES:
#   ANDROID_HOME        Android SDK location
#   ANDROID_NDK_HOME    Android NDK location (for native builds)
#   JAVA_HOME           Java JDK location
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Prerequisites missing
#   3   Build failed
#   4   Installation failed
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
readonly BUILD_TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# Default configuration
BUILD_TYPE="debug"
CLEAN_BUILD=false
NATIVE_BUILD=false
INSTALL_ON_DEVICE=false
OUTPUT_DIR="$PROJECT_DIR/dist/functiongemma"
MODEL_PATH=""
PARALLEL_JOBS="auto"
VERBOSE=false
LOG_FILE=""
CI_MODE=false

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

detect_os() {
    case "$(uname -s 2>/dev/null || echo "Windows")" in
        Linux*)     echo "linux";;
        Darwin*)    echo "macos";;
        MINGW*|MSYS*|CYGWIN*) echo "windows";;
        *)          echo "unknown";;
    esac
}

get_gradlew() {
    if [[ -f "$PROJECT_DIR/gradlew" ]]; then
        echo "./gradlew"
    elif [[ -f "$PROJECT_DIR/gradlew.bat" ]]; then
        echo "./gradlew.bat"
    else
        return 1
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

get_file_size() {
    local file="$1"
    if [[ -f "$file" ]]; then
        stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

# =============================================================================
# Prerequisite Checks
# =============================================================================

check_prerequisites() {
    log_step "Checking prerequisites..."
    local missing=()
    
    # Check Java
    if ! command -v java &>/dev/null; then
        missing+=("Java JDK 17+")
    else
        log_info "Java: $(java -version 2>&1 | head -1)"
    fi
    
    # Check Gradle wrapper
    if ! get_gradlew &>/dev/null; then
        missing+=("Gradle wrapper")
    else
        log_info "Gradle wrapper: found"
    fi
    
    # Check Android SDK
    if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
        if [[ "$INSTALL_ON_DEVICE" == "true" ]]; then
            missing+=("ANDROID_HOME (required for installation)")
        fi
    else
        log_info "Android SDK: ${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
    fi
    
    # Check Android NDK for native builds
    if [[ "$NATIVE_BUILD" == "true" ]]; then
        if [[ -z "${ANDROID_NDK_HOME:-}" && -z "${ANDROID_NDK_ROOT:-}" ]]; then
            log_warn "ANDROID_NDK_HOME not set - native build may fail"
        else
            log_info "Android NDK: ${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}"
        fi
    fi
    
    # Check ADB for installation
    if [[ "$INSTALL_ON_DEVICE" == "true" ]]; then
        if ! command -v adb &>/dev/null; then
            missing+=("ADB")
        else
            log_info "ADB: $(which adb)"
        fi
    fi
    
    # Report missing
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing prerequisites:"
        for item in "${missing[@]}"; do
            log_error "  - $item"
        done
        return 2
    fi
    
    log_success "Prerequisites check passed"
    return 0
}

check_device_connection() {
    if [[ "$INSTALL_ON_DEVICE" != "true" ]]; then
        return 0
    fi
    
    log_step "Checking device connection..."
    
    local devices
    devices=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
    
    if [[ "$devices" -eq 0 ]]; then
        log_error "No connected Android devices found"
        return 4
    fi
    
    log_info "Found $devices device(s)"
    return 0
}

# =============================================================================
# Build Functions
# =============================================================================

clean_build() {
    log_step "Cleaning build artifacts..."
    
    local gradlew
    gradlew=$(get_gradlew)
    
    $gradlew clean --quiet
    
    # Clean CMake build
    if [[ -d "$PROJECT_DIR/app/.cxx" ]]; then
        rm -rf "$PROJECT_DIR/app/.cxx"
    fi
    
    # Clean output directory
    if [[ -d "$OUTPUT_DIR" ]]; then
        rm -rf "$OUTPUT_DIR"/*
    fi
    
    log_success "Clean completed"
}

build_native_libraries() {
    log_step "Building native libraries for FunctionGemma..."
    
    local ndk_path="${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}"
    
    if [[ -z "$ndk_path" ]]; then
        log_warn "ANDROID_NDK_HOME not set - skipping native build"
        return 0
    fi
    
    local cpp_dir="$PROJECT_DIR/app/src/main/cpp"
    
    if [[ ! -d "$cpp_dir" ]]; then
        log_warn "Native source not found - skipping native build"
        return 0
    fi
    
    mkdir -p "$PROJECT_DIR/app/.cxx/Release"
    
    cmake -S "$cpp_dir" \
        -B "$PROJECT_DIR/app/.cxx/Release" \
        -DCMAKE_TOOLCHAIN_FILE="$ndk_path/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-21 \
        -DCMAKE_BUILD_TYPE=Release \
        -DUSE_LLAMA_CPP=ON \
        -DFUNCTIONGEMMA_VERBOSE=OFF \
        2>&1 | head -20
    
    if [[ "$PARALLEL_JOBS" == "auto" ]]; then
        cmake --build "$PROJECT_DIR/app/.cxx/Release" --parallel
    else
        cmake --build "$PROJECT_DIR/app/.cxx/Release" --parallel "$PARALLEL_JOBS"
    fi
    
    log_success "Native libraries built"
}

build_apk() {
    log_step "Building FunctionGemma APK ($BUILD_TYPE)..."
    
    local gradlew
    gradlew=$(get_gradlew)
    
    local build_task="assembleDebug"
    local gradle_flags="-PuseFunctionGemma=true"
    
    if [[ "$BUILD_TYPE" == "release" ]]; then
        build_task="assembleRelease"
    fi
    
    if [[ "$NATIVE_BUILD" == "true" ]]; then
        gradle_flags="$gradle_flags -PuseLlamaCpp=true"
    fi
    
    if [[ -n "$MODEL_PATH" && -f "$MODEL_PATH" ]]; then
        gradle_flags="$gradle_flags -PmodelPath=$MODEL_PATH"
    fi
    
    local start_time
    start_time=$(date +%s)
    
    if ! $gradlew :app:$build_task $gradle_flags --console=plain; then
        log_error "Build failed"
        return 3
    fi
    
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log_success "Build completed in ${duration}s"
    return 0
}

find_apk() {
    local apk_dir
    if [[ "$BUILD_TYPE" == "release" ]]; then
        apk_dir="$PROJECT_DIR/app/build/outputs/apk/release"
    else
        apk_dir="$PROJECT_DIR/app/build/outputs/apk/debug"
    fi
    
    if [[ ! -d "$apk_dir" ]]; then
        return 1
    fi
    
    find "$apk_dir" -name "*.apk" -type f | head -1
}

verify_apk() {
    local apk_file="$1"
    
    log_step "Verifying APK..."
    
    local size_bytes
    size_bytes=$(get_file_size "$apk_file")
    
    log_info "APK: $(basename "$apk_file")"
    log_info "Size: $(format_size "$size_bytes")"
    log_info "Path: $apk_file"
    
    # FunctionGemma APK should be < 100MB without model
    local size_mb=$((size_bytes / 1024 / 1024))
    if [[ "$size_mb" -gt 150 ]]; then
        log_warn "APK size (${size_mb}MB) is larger than expected"
    fi
    
    return 0
}

copy_apk_to_output() {
    local apk_file="$1"
    
    log_step "Copying APK to output directory..."
    
    local output_apk="$OUTPUT_DIR/egyptian_agent_functiongemma_${BUILD_TYPE}_${BUILD_TIMESTAMP}.apk"
    
    cp "$apk_file" "$output_apk"
    
    log_success "APK copied to: $output_apk"
    echo "$output_apk"
}

copy_native_libs() {
    if [[ "$NATIVE_BUILD" != "true" ]]; then
        return 0
    fi
    
    log_step "Copying native libraries..."
    
    mkdir -p "$OUTPUT_DIR/libs"
    
    if [[ -d "$PROJECT_DIR/app/.cxx/Release" ]]; then
        find "$PROJECT_DIR/app/.cxx/Release" -name "*.so" -exec cp {} "$OUTPUT_DIR/libs/" \; 2>/dev/null || true
        log_info "Native libraries copied to: $OUTPUT_DIR/libs/"
    fi
}

install_apk() {
    local apk_file="$1"
    
    log_step "Installing APK on device..."
    
    adb uninstall com.egyptian.agent 2>/dev/null || true
    
    if ! adb install -r -d "$apk_file"; then
        log_error "Installation failed"
        return 4
    fi
    
    log_success "APK installed successfully"
    return 0
}

generate_build_report() {
    local apk_file="$1"
    local report_file="$OUTPUT_DIR/build_report_$BUILD_TIMESTAMP.txt"
    
    log_step "Generating build report..."
    
    cat > "$report_file" << EOF
===============================================================================
Egyptian Agent - FunctionGemma Build Report
===============================================================================

Build Information:
  Build Type:      $BUILD_TYPE
  Timestamp:       $BUILD_TIMESTAMP
  Native Build:    $NATIVE_BUILD
  Clean Build:     $CLEAN_BUILD
  Model Path:      ${MODEL_PATH:-default}

Environment:
  OS:              $(detect_os)
  Java:            $(java -version 2>&1 | head -1 || echo "unknown")
  Android SDK:     ${ANDROID_HOME:-$ANDROID_SDK_ROOT:-not set}
  Android NDK:     ${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT:-not set}

Git Information:
  Branch:          $(git branch --show-current 2>/dev/null || echo "unknown")
  Commit:          $(git rev-parse HEAD 2>/dev/null || echo "unknown")

Build Artifacts:
  APK Path:        $apk_file
  APK Size:        $(format_size $(get_file_size "$apk_file"))

Build Status:      SUCCESS
===============================================================================
EOF
    
    log_info "Build report: $report_file"
}

# =============================================================================
# Help and Usage
# =============================================================================

show_help() {
    cat << EOF
Egyptian Agent - FunctionGemma Build Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --release           Build release APK (default: debug)
    --debug             Build debug APK
    --clean             Clean build artifacts before building
    --native            Build with native llama.cpp libraries
    --install           Install APK on connected device
    --output DIR        Output directory (default: dist/functiongemma)
    --model PATH        Path to FunctionGemma model file
    --parallel JOBS     Number of parallel build jobs (default: auto)
    --verbose           Enable verbose build output
    --log-file PATH     Write build log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    # Basic debug build
    $SCRIPT_NAME

    # Release build with native libraries
    $SCRIPT_NAME --release --native

    # Build with custom model and install
    $SCRIPT_NAME --model /path/to/model.gguf --install

    # CI/CD build
    $SCRIPT_NAME --release --ci --log-file build.log

ENVIRONMENT VARIABLES:
    ANDROID_HOME        Android SDK location
    ANDROID_NDK_HOME    Android NDK location (for native builds)
    JAVA_HOME           Java JDK location

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Build failed
    4   Installation failed
    5   Invalid arguments

OUTPUT:
    APK:     dist/functiongemma/egyptian_agent_functiongemma_*.apk
    Report:  dist/functiongemma/build_report_*.txt
    Logs:    build/logs/

For more information, see: docs/FUNCTIONGEMMA_QUICKSTART.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --release)
                BUILD_TYPE="release"
                shift
                ;;
            --debug)
                BUILD_TYPE="debug"
                shift
                ;;
            --clean)
                CLEAN_BUILD=true
                shift
                ;;
            --native)
                NATIVE_BUILD=true
                shift
                ;;
            --install)
                INSTALL_ON_DEVICE=true
                shift
                ;;
            --output)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --output requires an argument"
                    return 5
                fi
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --model)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --model requires an argument"
                    return 5
                fi
                MODEL_PATH="$2"
                shift 2
                ;;
            --parallel)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --parallel requires an argument"
                    return 5
                fi
                PARALLEL_JOBS="$2"
                shift 2
                ;;
            --verbose)
                VERBOSE=true
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
    print_header "FunctionGemma Build Script"
    
    echo ""
    log_info "Build Configuration:"
    echo "  Build Type:    $BUILD_TYPE"
    echo "  Clean Build:   $CLEAN_BUILD"
    echo "  Native Build:  $NATIVE_BUILD"
    echo "  Install:       $INSTALL_ON_DEVICE"
    echo "  Output Dir:    $OUTPUT_DIR"
    echo ""
    
    # Check prerequisites
    if ! check_prerequisites; then
        exit $?
    fi
    
    # Check device if installing
    if [[ "$INSTALL_ON_DEVICE" == "true" ]]; then
        if ! check_device_connection; then
            exit $?
        fi
    fi
    
    echo ""
    
    # Clean if requested
    if [[ "$CLEAN_BUILD" == "true" ]]; then
        clean_build
        echo ""
    fi
    
    # Build native libraries if requested
    if [[ "$NATIVE_BUILD" == "true" ]]; then
        build_native_libraries
        echo ""
    fi
    
    # Build APK
    if ! build_apk; then
        exit $?
    fi
    
    # Find and verify APK
    local apk_file
    apk_file=$(find_apk)
    
    if [[ -z "$apk_file" ]]; then
        log_error "APK not found"
        return 3
    fi
    
    verify_apk "$apk_file"
    
    echo ""
    
    # Copy to output directory
    local output_apk
    output_apk=$(copy_apk_to_output "$apk_file")
    
    # Copy native libraries
    copy_native_libs
    
    # Install if requested
    if [[ "$INSTALL_ON_DEVICE" == "true" ]]; then
        echo ""
        if ! install_apk "$output_apk"; then
            exit $?
        fi
    fi
    
    # Generate build report
    generate_build_report "$output_apk"
    
    # Print summary
    print_header "Build Summary"
    
    log_success "FunctionGemma build completed!"
    echo ""
    echo "  APK Location: $output_apk"
    echo "  APK Size:     $(format_size $(get_file_size "$output_apk"))"
    echo "  Build Report: $OUTPUT_DIR/build_report_$BUILD_TIMESTAMP.txt"
    echo ""
    
    log_info "Next steps:"
    echo "  1. Download model: ./scripts/model/download_functiongemma_model.sh"
    echo "  2. Deploy model:   ./scripts/deploy/deploy_functiongemma.sh"
    echo "  3. Test function calling on device"
    echo ""
    
    return 0
}

# Trap errors
trap 'log_error "Build interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
