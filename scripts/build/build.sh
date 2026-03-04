#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Main Build Script
# =============================================================================
#
# PURPOSE:
#   Builds the Egyptian Agent application for Honor X6c and compatible devices.
#   Supports debug/release builds, native library compilation, and device deployment.
#
# USAGE:
#   ./scripts/build/build.sh [OPTIONS]
#
# OPTIONS:
#   --release           Build release APK (default: debug)
#   --debug             Build debug APK
#   --clean             Clean build artifacts before building
#   --native            Build with native libraries (llama.cpp, whisper.cpp)
#   --install           Install APK on connected device after build
#   --target DEVICE     Target device profile (default: honor-x6c)
#   --parallel JOBS     Number of parallel build jobs (default: auto)
#   --verbose           Enable verbose build output
#   --log-file PATH     Write build log to specified file
#   --ci                CI/CD mode (non-interactive, machine-readable output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/build/build.sh --release --clean
#   ./scripts/build/build.sh --native --install
#   ./scripts/build/build.sh --release --target honor-x6c --parallel 4
#   ./scripts/build/build.sh --ci --log-file build.log
#
# ENVIRONMENT VARIABLES:
#   ANDROID_HOME        Android SDK location
#   ANDROID_SDK_ROOT    Android SDK location (alternative)
#   ANDROID_NDK_HOME    Android NDK location (for native builds)
#   JAVA_HOME           Java JDK location
#   GRADLE_OPTS         Gradle JVM options
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
TARGET_DEVICE="honor-x6c"
PARALLEL_JOBS="auto"
VERBOSE=false
CI_MODE=false
LOG_FILE=""

# Colors (disabled in CI mode or non-TTY)
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

print_progress() {
    local current="$1"
    local total="$2"
    local message="$3"
    local bar_width=40
    
    local percent=$((current * 100 / total))
    local filled=$((current * bar_width / total))
    local empty=$((bar_width - filled))
    
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[PROGRESS] $current/$total ($percent%) - $message"
    else
        printf "\r${COLORS[blue]}[%s>%s]${COLORS[nc]} %3d%% - %s" \
            "$(printf '█%.0s' $(seq 1 $filled))" \
            "$(printf '░%.0s' $(seq 1 $empty))" \
            "$percent" \
            "$message"
    fi
}

# =============================================================================
# Utility Functions
# =============================================================================

detect_os() {
    local os
    case "$(uname -s 2>/dev/null || echo "Windows")" in
        Linux*)     os="linux";;
        Darwin*)    os="macos";;
        MINGW*|MSYS*|CYGWIN*) os="windows";;
        *)          os="unknown";;
    esac
    echo "$os"
}

is_windows() {
    [[ "$(detect_os)" == "windows" ]]
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
        if command -v stat &>/dev/null; then
            stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null || echo "0"
        else
            echo "0"
        fi
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
    local warnings=()
    
    # Check Java
    if ! command -v java &>/dev/null; then
        missing+=("Java JDK 17+")
    else
        local java_version
        java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ "$java_version" -lt 17 ]]; then
            missing+=("Java JDK 17+ (found version $java_version)")
        else
            log_info "Java: $(java -version 2>&1 | head -1)"
        fi
    fi
    
    # Check Gradle wrapper
    local gradlew
    if ! gradlew=$(get_gradlew); then
        missing+=("Gradle wrapper (gradlew)")
    else
        log_info "Gradle wrapper: found"
    fi
    
    # Check Android SDK (optional for build, required for install)
    if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
        if [[ "$INSTALL_ON_DEVICE" == "true" ]]; then
            missing+=("ANDROID_HOME or ANDROID_SDK_ROOT (required for installation)")
        else
            warnings+=("ANDROID_HOME not set - build-only mode")
        fi
    else
        log_info "Android SDK: ${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
    fi
    
    # Check Android NDK (for native builds)
    if [[ "$NATIVE_BUILD" == "true" ]]; then
        if [[ -z "${ANDROID_NDK_HOME:-}" && -z "${ANDROID_NDK_ROOT:-}" ]]; then
            missing+=("ANDROID_NDK_HOME (required for native build)")
        else
            log_info "Android NDK: ${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}"
        fi
    fi
    
    # Check ADB (for installation)
    if [[ "$INSTALL_ON_DEVICE" == "true" ]]; then
        if ! command -v adb &>/dev/null; then
            missing+=("ADB (Android Debug Bridge)")
        else
            log_info "ADB: $(which adb)"
        fi
    fi
    
    # Report warnings
    for warning in "${warnings[@]:-}"; do
        if [[ -n "$warning" ]]; then
            log_warn "$warning"
        fi
    done
    
    # Report missing
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing prerequisites:"
        for item in "${missing[@]}"; do
            log_error "  - $item"
        done
        echo ""
        log_error "Solutions:"
        if [[ " ${missing[*]} " =~ "Java" ]]; then
            echo "  - Install JDK 17+: https://adoptium.net/"
        fi
        if [[ " ${missing[*]} " =~ "Gradle" ]]; then
            echo "  - Ensure you're in the project root directory"
        fi
        if [[ " ${missing[*]} " =~ "ANDROID" ]]; then
            echo "  - Install Android Studio or SDK tools"
            echo "  - Set ANDROID_HOME environment variable"
        fi
        if [[ " ${missing[*]} " =~ "ADB" ]]; then
            echo "  - Install Android SDK platform-tools"
        fi
        return 2
    fi
    
    log_success "All prerequisites satisfied"
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
        echo ""
        log_error "Solutions:"
        echo "  1. Enable USB debugging on your device"
        echo "  2. Connect device via USB cable"
        echo "  3. Accept the USB debugging prompt on device"
        echo "  4. Run: adb devices"
        return 4
    fi
    
    log_info "Found $devices device(s)"
    
    # Show device info
    local device_model
    device_model=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Unknown")
    local device_android
    device_android=$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r' || echo "Unknown")
    
    log_info "Primary device: $device_model (Android $device_android)"
    
    return 0
}

# =============================================================================
# Build Functions
# =============================================================================

clean_build() {
    log_step "Cleaning build artifacts..."
    
    local gradlew
    gradlew=$(get_gradlew)
    
    if [[ "$VERBOSE" == "true" ]]; then
        $gradlew clean
    else
        $gradlew clean --quiet
    fi
    
    # Clean CMake build directory
    if [[ -d "$PROJECT_DIR/app/.cxx" ]]; then
        rm -rf "$PROJECT_DIR/app/.cxx"
        log_info "Cleaned CMake build directory"
    fi
    
    # Clean build output
    if [[ -d "$PROJECT_DIR/app/build" ]]; then
        rm -rf "$PROJECT_DIR/app/build"
        log_info "Cleaned app build directory"
    fi
    
    log_success "Clean completed"
}

build_native_libraries() {
    log_step "Building native libraries..."
    
    local ndk_path="${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}"
    local cpp_dir="$PROJECT_DIR/app/src/main/cpp"
    
    if [[ ! -d "$cpp_dir" ]]; then
        log_warn "Native source directory not found, skipping native build"
        return 0
    fi
    
    log_info "Using Android NDK: $ndk_path"
    
    # Create build directories
    mkdir -p "$PROJECT_DIR/app/.cxx/Release"
    
    # Configure CMake
    cmake -S "$cpp_dir" \
        -B "$PROJECT_DIR/app/.cxx/Release" \
        -DCMAKE_TOOLCHAIN_FILE="$ndk_path/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-21 \
        -DCMAKE_BUILD_TYPE=Release \
        -DUSE_LLAMA_CPP=ON \
        -DUSE_WHISPER=ON \
        2>&1 | while read -r line; do
            if [[ "$VERBOSE" == "true" ]]; then
                echo "$line"
            fi
        done
    
    # Build native libraries
    if [[ "$PARALLEL_JOBS" == "auto" ]]; then
        cmake --build "$PROJECT_DIR/app/.cxx/Release" --parallel
    else
        cmake --build "$PROJECT_DIR/app/.cxx/Release" --parallel "$PARALLEL_JOBS"
    fi
    
    log_success "Native libraries built"
}

run_gradle_build() {
    local gradlew
    gradlew=$(get_gradlew)
    
    local build_task="assembleDebug"
    local gradle_flags=""
    
    if [[ "$BUILD_TYPE" == "release" ]]; then
        build_task="assembleRelease"
    fi
    
    if [[ "$NATIVE_BUILD" == "true" ]]; then
        gradle_flags="-PuseLlamaCpp=true -PuseWhisper=true"
    fi
    
    if [[ "$PARALLEL_JOBS" != "auto" ]]; then
        gradle_flags="$gradle_flags --parallel"
    fi
    
    if [[ "$VERBOSE" == "true" ]]; then
        gradle_flags="$gradle_flags --info"
    fi
    
    log_step "Building Egyptian Agent ($BUILD_TYPE)..."
    echo ""
    
    # Run build with progress
    local start_time
    start_time=$(date +%s)
    
    if [[ "$CI_MODE" == "true" ]]; then
        $gradlew :app:$build_task $gradle_flags
    else
        $gradlew :app:$build_task $gradle_flags --console=plain
    fi
    
    local build_status=$?
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    
    if [[ $build_status -ne 0 ]]; then
        log_error "Build failed after ${duration}s"
        return 3
    fi
    
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
        log_error "APK output directory not found: $apk_dir"
        return 3
    fi
    
    local apk_file
    apk_file=$(find "$apk_dir" -name "*.apk" -type f | head -1)
    
    if [[ -z "$apk_file" || ! -f "$apk_file" ]]; then
        log_error "No APK file found in $apk_dir"
        return 3
    fi
    
    echo "$apk_file"
}

verify_apk() {
    local apk_file="$1"
    
    log_step "Verifying APK..."
    
    local size_bytes
    size_bytes=$(get_file_size "$apk_file")
    local size_formatted
    size_formatted=$(format_size "$size_bytes")
    
    log_info "APK: $(basename "$apk_file")"
    log_info "Size: $size_formatted"
    log_info "Path: $apk_file"
    
    # Verify APK is valid
    if command -v aapt2 &>/dev/null; then
        if ! aapt2 dump badging "$apk_file" &>/dev/null; then
            log_error "APK validation failed - file may be corrupted"
            return 3
        fi
        log_info "APK validation: passed"
    fi
    
    # Check minimum size
    local min_size=$((50 * 1024 * 1024))  # 50MB minimum
    if [[ "$size_bytes" -lt "$min_size" ]]; then
        log_warn "APK size is smaller than expected (< 50MB)"
    fi
    
    return 0
}

install_apk() {
    local apk_file="$1"
    
    log_step "Installing APK on device..."
    
    # Uninstall existing version
    adb uninstall com.egyptian.agent 2>/dev/null || true
    
    # Install new version
    if ! adb install -r -d "$apk_file"; then
        log_error "Installation failed"
        echo ""
        log_error "Solutions:"
        echo "  1. Ensure device is unlocked"
        echo "  2. Check for sufficient storage space"
        echo "  3. Try: adb uninstall com.egyptian.agent"
        return 4
    fi
    
    log_success "APK installed successfully"
    
    # Grant permissions
    log_info "Granting permissions..."
    adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO 2>/dev/null || true
    adb shell pm grant com.egyptian.agent android.permission.CALL_PHONE 2>/dev/null || true
    
    return 0
}

generate_build_report() {
    local apk_file="$1"
    local report_file="$LOG_DIR/build_report_$BUILD_TIMESTAMP.txt"
    
    log_step "Generating build report..."
    
    cat > "$report_file" << EOF
===============================================================================
Egyptian Agent - Build Report
===============================================================================

Build Information:
  Build Type:      $BUILD_TYPE
  Target Device:   $TARGET_DEVICE
  Timestamp:       $BUILD_TIMESTAMP
  Native Build:    $NATIVE_BUILD
  Clean Build:     $CLEAN_BUILD

Environment:
  OS:              $(detect_os)
  Hostname:        $(hostname 2>/dev/null || echo "unknown")
  User:            $(whoami 2>/dev/null || echo "unknown")

Git Information:
  Branch:          $(git branch --show-current 2>/dev/null || echo "unknown")
  Commit:          $(git rev-parse HEAD 2>/dev/null || echo "unknown")
  Status:          $(git diff-index --quiet HEAD -- 2>/dev/null && echo "clean" || echo "modified")

Build Tools:
  Java:            $(java -version 2>&1 | head -1 || echo "unknown")
  Gradle:          $(get_gradlew 2>/dev/null && ./gradlew --version 2>/dev/null | head -1 || echo "unknown")
  Android SDK:     ${ANDROID_HOME:-$ANDROID_SDK_ROOT:-not set}
  Android NDK:     ${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT:-not set}

Build Artifacts:
  APK Path:        $apk_file
  APK Size:        $(format_size $(get_file_size "$apk_file"))
  APK SHA256:      $(sha256sum "$apk_file" 2>/dev/null | cut -d' ' -f1 || echo "not computed")

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
Egyptian Agent - Main Build Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --release           Build release APK (default: debug)
    --debug             Build debug APK
    --clean             Clean build artifacts before building
    --native            Build with native libraries (llama.cpp, whisper.cpp)
    --install           Install APK on connected device after build
    --target DEVICE     Target device profile (default: honor-x6c)
    --parallel JOBS     Number of parallel build jobs (default: auto)
    --verbose           Enable verbose build output
    --log-file PATH     Write build log to specified file
    --ci                CI/CD mode (non-interactive, machine-readable output)
    -h, --help          Show this help message

EXAMPLES:
    # Basic debug build
    $SCRIPT_NAME

    # Release build with clean
    $SCRIPT_NAME --release --clean

    # Build with native libraries and install
    $SCRIPT_NAME --native --install

    # CI/CD build with logging
    $SCRIPT_NAME --release --ci --log-file build.log

    # Parallel build for faster compilation
    $SCRIPT_NAME --release --parallel 4

ENVIRONMENT VARIABLES:
    ANDROID_HOME        Android SDK location
    ANDROID_SDK_ROOT    Android SDK location (alternative)
    ANDROID_NDK_HOME    Android NDK location (for native builds)
    JAVA_HOME           Java JDK location
    GRADLE_OPTS         Gradle JVM options

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Build failed
    4   Installation failed
    5   Invalid arguments

OUTPUT:
    Debug APK:   app/build/outputs/apk/debug/app-debug.apk
    Release APK: app/build/outputs/apk/release/app-release.apk
    Logs:        build/logs/

For more information, see: docs/deployment/DEPLOYMENT_GUIDE.md
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
            --target)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --target requires an argument"
                    return 5
                fi
                TARGET_DEVICE="$2"
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
                # Disable colors in CI mode
                COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]='')
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                echo "Use --help for usage information"
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
    local exit_code=0
    
    # Parse arguments
    if ! parse_arguments "$@"; then
        exit $?
    fi
    
    # Initialize logging
    init_logging
    
    # Print header
    print_header "Egyptian Agent Build Script"
    
    echo ""
    log_info "Build Configuration:"
    echo "  Build Type:    $BUILD_TYPE"
    echo "  Target Device: $TARGET_DEVICE"
    echo "  Clean Build:   $CLEAN_BUILD"
    echo "  Native Build:  $NATIVE_BUILD"
    echo "  Install:       $INSTALL_ON_DEVICE"
    echo "  Parallel Jobs: $PARALLEL_JOBS"
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
    
    # Run Gradle build
    if ! run_gradle_build; then
        exit $?
    fi
    
    # Find and verify APK
    local apk_file
    apk_file=$(find_apk) || exit $?
    
    if ! verify_apk "$apk_file"; then
        exit $?
    fi
    
    echo ""
    
    # Install if requested
    if [[ "$INSTALL_ON_DEVICE" == "true" ]]; then
        if ! install_apk "$apk_file"; then
            exit $?
        fi
        echo ""
    fi
    
    # Generate build report
    generate_build_report "$apk_file"
    
    # Print summary
    print_header "Build Summary"
    
    log_success "Build completed successfully!"
    echo ""
    echo "  APK Location: $apk_file"
    echo "  APK Size:     $(format_size $(get_file_size "$apk_file"))"
    echo "  Build Log:    $LOG_DIR/build_report_$BUILD_TIMESTAMP.txt"
    echo ""
    
    if [[ "$BUILD_TYPE" == "release" ]]; then
        log_warn "For production deployment:"
        echo "  1. Sign the APK with your release keystore"
        echo "  2. Install as system app: ./scripts/deploy/deploy_production.sh"
        echo "  3. Verify installation: ./scripts/utils/verify_implementation.sh"
    fi
    
    echo ""
    
    return 0
}

# Trap errors
trap 'log_error "Build interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
