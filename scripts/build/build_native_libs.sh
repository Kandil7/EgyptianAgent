#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Native Libraries Build Script
# =============================================================================
#
# PURPOSE:
#   Builds native C++ libraries for the Egyptian Agent application including
#   llama.cpp for LLM inference and whisper.cpp for speech recognition.
#   Optimized for ARM64 and ARMv7 Android devices.
#
# USAGE:
#   ./scripts/build/build_native_libs.sh [OPTIONS]
#
# OPTIONS:
#   --clean             Clean previous build artifacts
#   --release           Build release libraries (default: debug)
#   --debug             Build debug libraries with symbols
#   --abi ABI           Target ABI(s): arm64-v8a, armeabi-v7a, x86_64, x86
#                       (default: arm64-v8a,armeabi-v7a)
#   --api-level LEVEL   Android API level (default: 21)
#   --parallel JOBS     Number of parallel build jobs (default: auto)
#   --verbose           Enable verbose CMake output
#   --log-file PATH     Write build log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/build/build_native_libs.sh
#   ./scripts/build/build_native_libs.sh --clean --release
#   ./scripts/build/build_native_libs.sh --abi arm64-v8a --parallel 4
#   ./scripts/build/build_native_libs.sh --ci --log-file native_build.log
#
# ENVIRONMENT VARIABLES:
#   ANDROID_NDK_HOME    Android NDK location (required)
#   ANDROID_NDK_ROOT    Android NDK location (alternative)
#   CMAKE               CMake executable path
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Prerequisites missing (NDK not found)
#   3   Build failed
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
BUILD_TYPE="Release"
CLEAN_BUILD=false
TARGET_ABIS="arm64-v8a,armeabi-v7a"
API_LEVEL=21
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

get_ndk_path() {
    if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
        echo "$ANDROID_NDK_HOME"
    elif [[ -n "${ANDROID_NDK_ROOT:-}" ]]; then
        echo "$ANDROID_NDK_ROOT"
    elif [[ -n "${ANDROID_HOME:-}" ]]; then
        # Try to find NDK in Android SDK
        local ndk_dir="$ANDROID_HOME/ndk"
        if [[ -d "$ndk_dir" ]]; then
            ls -d "$ndk_dir"/* 2>/dev/null | head -1
        fi
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
    
    # Check Android NDK
    local ndk_path
    ndk_path=$(get_ndk_path)
    
    if [[ -z "$ndk_path" || ! -d "$ndk_path" ]]; then
        missing+=("Android NDK (set ANDROID_NDK_HOME)")
    else
        log_info "Android NDK: $ndk_path"
        export ANDROID_NDK_HOME="$ndk_path"
    fi
    
    # Check CMake
    if ! command -v cmake &>/dev/null; then
        # Try NDK's bundled CMake
        if [[ -n "$ndk_path" && -f "$ndk_path/build/cmake/bin/cmake" ]]; then
            export PATH="$ndk_path/build/cmake/bin:$PATH"
            log_info "CMake: Using NDK bundled version"
        else
            missing+=("CMake")
        fi
    else
        log_info "CMake: $(cmake --version | head -1)"
    fi
    
    # Check Ninja (optional but recommended)
    if command -v ninja &>/dev/null; then
        log_info "Ninja: $(ninja --version | head -1)"
    else
        log_warn "Ninja not found - using Make instead (slower)"
    fi
    
    # Check source directory
    local cpp_dir="$PROJECT_DIR/app/src/main/cpp"
    if [[ ! -d "$cpp_dir" ]]; then
        missing+=("Native source directory ($cpp_dir)")
    else
        log_info "Source directory: $cpp_dir"
    fi
    
    # Report missing
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing prerequisites:"
        for item in "${missing[@]}"; do
            log_error "  - $item"
        done
        echo ""
        log_error "Solutions:"
        if [[ " ${missing[*]} " =~ "NDK" ]]; then
            echo "  - Install Android NDK via Android Studio SDK Manager"
            echo "  - Or download from: https://developer.android.com/ndk/downloads"
            echo "  - Set ANDROID_NDK_HOME environment variable"
        fi
        if [[ " ${missing[*]} " =~ "CMake" ]]; then
            echo "  - Install CMake: https://cmake.org/download/"
            echo "  - Or use NDK bundled CMake"
        fi
        return 2
    fi
    
    log_success "All prerequisites satisfied"
    return 0
}

# =============================================================================
# Build Functions
# =============================================================================

clean_build() {
    log_step "Cleaning native build artifacts..."
    
    local build_dir="$PROJECT_DIR/app/.cxx"
    
    if [[ -d "$build_dir" ]]; then
        rm -rf "$build_dir"
        log_info "Cleaned: $build_dir"
    fi
    
    local jni_libs_dir="$PROJECT_DIR/app/src/main/jniLibs"
    if [[ -d "$jni_libs_dir" ]]; then
        rm -rf "$jni_libs_dir"
        log_info "Cleaned: $jni_libs_dir"
    fi
    
    log_success "Clean completed"
}

build_for_abi() {
    local abi="$1"
    local build_type="$2"
    
    log_step "Building for $abi ($build_type)..."
    
    local ndk_path="${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}"
    local cpp_dir="$PROJECT_DIR/app/src/main/cpp"
    local build_dir="$PROJECT_DIR/app/.cxx/$build_type/$abi"
    local jni_libs_dir="$PROJECT_DIR/app/src/main/jniLibs/$abi"
    
    # Create directories
    mkdir -p "$build_dir"
    mkdir -p "$jni_libs_dir"
    
    # Configure CMake
    log_info "Configuring CMake for $abi..."
    
    local cmake_args=(
        -S "$cpp_dir"
        -B "$build_dir"
        -DCMAKE_TOOLCHAIN_FILE="$ndk_path/build/cmake/android.toolchain.cmake"
        -DANDROID_ABI="$abi"
        -DANDROID_PLATFORM="android-$API_LEVEL"
        -DCMAKE_BUILD_TYPE="$build_type"
        -DUSE_LLAMA_CPP=ON
        -DUSE_WHISPER=ON
        -DFUNCTIONGEMMA_ENABLED=ON
    )
    
    if [[ "$VERBOSE" == "true" ]]; then
        cmake "${cmake_args[@]}" 2>&1 | tee -a "$LOG_DIR/cmake_configure_$abi.log"
    else
        cmake "${cmake_args[@]}" > "$LOG_DIR/cmake_configure_$abi.log" 2>&1
    fi
    
    # Build
    log_info "Compiling native libraries..."
    
    local build_args=(
        --build "$build_dir"
        --config "$build_type"
    )
    
    if [[ "$PARALLEL_JOBS" == "auto" ]]; then
        build_args+=(--parallel)
    else
        build_args+=(--parallel "$PARALLEL_JOBS")
    fi
    
    local start_time
    start_time=$(date +%s)
    
    if [[ "$VERBOSE" == "true" ]]; then
        cmake "${build_args[@]}" 2>&1 | tee -a "$LOG_DIR/cmake_build_$abi.log"
    else
        cmake "${build_args[@]}" > "$LOG_DIR/cmake_build_$abi.log" 2>&1
    fi
    
    local build_status=$?
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [[ $build_status -ne 0 ]]; then
        log_error "Build failed for $abi after ${duration}s"
        log_error "Check log: $LOG_DIR/cmake_build_$abi.log"
        return 3
    fi
    
    # Copy built libraries to jniLibs
    log_info "Copying libraries to jniLibs..."
    
    find "$build_dir" -name "*.so" -type f -exec cp {} "$jni_libs_dir/" \; 2>/dev/null || true
    
    # Report built libraries
    local lib_count
    lib_count=$(find "$jni_libs_dir" -name "*.so" -type f | wc -l)
    local total_size=0
    
    for lib in "$jni_libs_dir"/*.so; do
        if [[ -f "$lib" ]]; then
            local size
            size=$(get_file_size "$lib")
            total_size=$((total_size + size))
            log_info "  $(basename "$lib"): $(format_size "$size")"
        fi
    done
    
    log_success "Built $lib_count libraries for $abi (${duration}s, $(format_size "$total_size"))"
    return 0
}

verify_libraries() {
    log_step "Verifying built libraries..."
    
    local jni_libs_dir="$PROJECT_DIR/app/src/main/jniLibs"
    local errors=0
    
    if [[ ! -d "$jni_libs_dir" ]]; then
        log_error "jniLibs directory not found"
        return 3
    fi
    
    # Check for required libraries
    local required_libs=("libllama.so" "libggml.so")
    
    for abi_dir in "$jni_libs_dir"/*/; do
        if [[ ! -d "$abi_dir" ]]; then
            continue
        fi
        
        local abi
        abi=$(basename "$abi_dir")
        log_info "Checking $abi..."
        
        for lib in "${required_libs[@]}"; do
            if [[ -f "$abi_dir/$lib" ]]; then
                log_info "  ✓ $lib"
            else
                log_warn "  ✗ $lib (missing)"
                ((errors++)) || true
            fi
        done
    done
    
    if [[ $errors -gt 0 ]]; then
        log_warn "Verification completed with $errors warning(s)"
    else
        log_success "All libraries verified"
    fi
    
    return 0
}

generate_build_report() {
    local report_file="$LOG_DIR/native_build_report_$BUILD_TIMESTAMP.txt"
    
    log_step "Generating build report..."
    
    local jni_libs_dir="$PROJECT_DIR/app/src/main/jniLibs"
    local total_size=0
    local total_libs=0
    
    for lib in $(find "$jni_libs_dir" -name "*.so" -type f 2>/dev/null); do
        local size
        size=$(get_file_size "$lib")
        total_size=$((total_size + size))
        ((total_libs++)) || true
    done
    
    cat > "$report_file" << EOF
===============================================================================
Egyptian Agent - Native Libraries Build Report
===============================================================================

Build Information:
  Timestamp:       $BUILD_TIMESTAMP
  Build Type:      $BUILD_TYPE
  Target ABIs:     $TARGET_ABIS
  API Level:       $API_LEVEL
  Parallel Jobs:   $PARALLEL_JOBS

Environment:
  OS:              $(detect_os)
  Android NDK:     ${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}
  CMake:           $(cmake --version 2>/dev/null | head -1 || echo "unknown")
  Ninja:           $(ninja --version 2>/dev/null | head -1 || echo "not installed")

Git Information:
  Branch:          $(git branch --show-current 2>/dev/null || echo "unknown")
  Commit:          $(git rev-parse HEAD 2>/dev/null || echo "unknown")

Build Artifacts:
  Output Directory: $jni_libs_dir
  Total Libraries:  $total_libs
  Total Size:       $(format_size "$total_size")

Libraries:
$(find "$jni_libs_dir" -name "*.so" -type f -exec ls -lh {} \; 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}')

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
Egyptian Agent - Native Libraries Build Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --clean             Clean previous build artifacts
    --release           Build release libraries (default)
    --debug             Build debug libraries with symbols
    --abi ABI           Target ABI(s), comma-separated
                        (default: arm64-v8a,armeabi-v7a)
                        Options: arm64-v8a, armeabi-v7a, x86_64, x86
    --api-level LEVEL   Android API level (default: 21)
    --parallel JOBS     Number of parallel build jobs (default: auto)
    --verbose           Enable verbose CMake output
    --log-file PATH     Write build log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    # Build for all ARM targets (default)
    $SCRIPT_NAME

    # Clean release build for arm64 only
    $SCRIPT_NAME --clean --release --abi arm64-v8a

    # Debug build with verbose output
    $SCRIPT_NAME --debug --verbose

    # CI/CD build
    $SCRIPT_NAME --ci --log-file native_build.log

ENVIRONMENT VARIABLES:
    ANDROID_NDK_HOME    Android NDK location (required)
    ANDROID_NDK_ROOT    Android NDK location (alternative)
    CMAKE               CMake executable path

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Build failed
    5   Invalid arguments

OUTPUT:
    Libraries:  app/src/main/jniLibs/<abi>/*.so
    Logs:       build/logs/cmake_*.log
    Report:     build/logs/native_build_report_*.txt

For more information, see: docs/architecture/NATIVE_LIBRARIES.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --clean)
                CLEAN_BUILD=true
                shift
                ;;
            --release)
                BUILD_TYPE="Release"
                shift
                ;;
            --debug)
                BUILD_TYPE="Debug"
                shift
                ;;
            --abi)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --abi requires an argument"
                    return 5
                fi
                TARGET_ABIS="$2"
                shift 2
                ;;
            --api-level)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --api-level requires an argument"
                    return 5
                fi
                API_LEVEL="$2"
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
    print_header "Native Libraries Build"
    
    echo ""
    log_info "Build Configuration:"
    echo "  Build Type:   $BUILD_TYPE"
    echo "  Target ABIs:  $TARGET_ABIS"
    echo "  API Level:    $API_LEVEL"
    echo "  Clean Build:  $CLEAN_BUILD"
    echo "  Parallel:     $PARALLEL_JOBS"
    echo ""
    
    # Check prerequisites
    if ! check_prerequisites; then
        exit $?
    fi
    
    echo ""
    
    # Clean if requested
    if [[ "$CLEAN_BUILD" == "true" ]]; then
        clean_build
        echo ""
    fi
    
    # Build for each ABI
    local failed=0
    IFS=',' read -ra ABIS <<< "$TARGET_ABIS"
    
    for abi in "${ABIS[@]}"; do
        abi=$(echo "$abi" | tr -d ' ')
        if [[ -n "$abi" ]]; then
            if ! build_for_abi "$abi" "$BUILD_TYPE"; then
                failed=1
                log_error "Build failed for ABI: $abi"
            fi
            echo ""
        fi
    done
    
    if [[ $failed -ne 0 ]]; then
        log_error "Native build failed"
        return 3
    fi
    
    # Verify libraries
    if ! verify_libraries; then
        return $?
    fi
    
    echo ""
    
    # Generate build report
    generate_build_report
    
    # Print summary
    print_header "Native Build Complete"
    
    log_success "Native libraries built successfully!"
    echo ""
    echo "  Output: $PROJECT_DIR/app/src/main/jniLibs/"
    echo "  Report: $LOG_DIR/native_build_report_$BUILD_TIMESTAMP.txt"
    echo ""
    
    log_info "Next steps:"
    echo "  1. Build Android APK: ./scripts/build/build.sh --native"
    echo "  2. Test on device with native libraries"
    echo ""
    
    return 0
}

# Trap errors
trap 'log_error "Build interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
