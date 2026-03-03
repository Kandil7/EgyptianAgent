#!/bin/bash
# FunctionGemma Build Script
# Builds the FunctionGemma variant of Egyptian Agent
#
# Features:
# - Clean build with FunctionGemma flags
# - Native library compilation (llama.cpp backend)
# - APK size verification
# - Output to dist/ directory
#
# Usage:
#   ./build_functiongemma.sh [--clean] [--release] [--native]
#
# Options:
#   --clean    Clean previous build artifacts
#   --release  Build release APK (default: debug)
#   --native   Build with native llama.cpp libraries
#   --help     Show this help message

set -e  # Exit on any error

# ============================================================================
# Configuration
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
OUTPUT_DIR="$SCRIPT_DIR/dist/functiongemma"
BUILD_DIR="$SCRIPT_DIR/app/build"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Build configuration
BUILD_TYPE="debug"
CLEAN_BUILD=false
NATIVE_BUILD=false
FUNCTIONGEMMA_MODEL_URL="https://huggingface.co/google/functiongemma-270m-it/resolve/main/functiongemma-270m-it-Q4_K_M.gguf"

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
    
    # Check for Gradle wrapper
    if [ ! -f "$PROJECT_DIR/gradlew" ] && [ ! -f "$PROJECT_DIR/gradlew.bat" ]; then
        log_error "Gradle wrapper not found!"
        log_error "Please ensure you're in the project root directory."
        exit 1
    fi
    
    # Check for Android SDK
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        log_warn "ANDROID_HOME or ANDROID_SDK_ROOT not set"
        log_warn "Build may fail if Android SDK is not in PATH"
    fi
    
    # Check for Java
    if ! command -v java &> /dev/null; then
        log_error "Java not found! Please install JDK 17+"
        exit 1
    fi
    
    log_info "Prerequisites check passed"
}

get_gradlew() {
    if [ -f "$PROJECT_DIR/gradlew" ]; then
        echo "./gradlew"
    elif [ -f "$PROJECT_DIR/gradlew.bat" ]; then
        echo "./gradlew.bat"
    else
        log_error "Gradle wrapper not found"
        exit 1
    fi
}

get_file_size() {
    local file="$1"
    if [ -f "$file" ]; then
        if command -v du &> /dev/null; then
            du -h "$file" | cut -f1
        else
            # Windows fallback
            stat -c%s "$file" 2>/dev/null || echo "unknown"
        fi
    else
        echo "not found"
    fi
}

# ============================================================================
# Build Functions
# ============================================================================

clean_build() {
    log_step "Cleaning previous build artifacts..."
    
    local gradlew=$(get_gradlew)
    
    # Clean Gradle build
    $gradlew clean --quiet
    
    # Clean CMake build directory
    if [ -d "$PROJECT_DIR/app/.cxx" ]; then
        rm -rf "$PROJECT_DIR/app/.cxx"
        log_info "Cleaned CMake build directory"
    fi
    
    # Clean output directory
    if [ -d "$OUTPUT_DIR" ]; then
        rm -rf "$OUTPUT_DIR"
        log_info "Cleaned output directory"
    fi
    
    log_success "Clean completed"
}

build_native_libraries() {
    log_step "Building native libraries for FunctionGemma..."
    
    # Check for Android NDK
    if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$ANDROID_NDK_ROOT" ]; then
        log_warn "ANDROID_NDK_HOME not set - building without native libraries"
        log_warn "Set ANDROID_NDK_HOME to build llama.cpp backend"
        return 0
    fi
    
    local ndk_path="${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}"
    log_info "Using Android NDK: $ndk_path"
    
    # Create build directory
    mkdir -p "$PROJECT_DIR/app/.cxx/Release"
    
    # Build with CMake
    cd "$PROJECT_DIR/app/src/main/cpp"
    
    cmake -S . -B "$PROJECT_DIR/app/.cxx/Release" \
        -DCMAKE_TOOLCHAIN_FILE="$ndk_path/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-21 \
        -DCMAKE_BUILD_TYPE=Release \
        -DUSE_LLAMA_CPP=ON \
        -DFUNCTIONGEMMA_VERBOSE_LOGGING=OFF
    
    cmake --build "$PROJECT_DIR/app/.cxx/Release" --parallel
    
    # Also build for 32-bit ARM (armeabi-v7a)
    log_info "Building for armeabi-v7a..."
    cmake -S . -B "$PROJECT_DIR/app/.cxx/Release32" \
        -DCMAKE_TOOLCHAIN_FILE="$ndk_path/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=armeabi-v7a \
        -DANDROID_PLATFORM=android-21 \
        -DCMAKE_BUILD_TYPE=Release \
        -DUSE_LLAMA_CPP=ON
    
    cmake --build "$PROJECT_DIR/app/.cxx/Release32" --parallel
    
    cd "$PROJECT_DIR"
    
    log_success "Native libraries built successfully"
}

build_apk() {
    log_step "Building FunctionGemma APK ($BUILD_TYPE)..."
    
    local gradlew=$(get_gradlew)
    local build_task="assembleDebug"
    local gradle_flags=""
    
    if [ "$BUILD_TYPE" = "release" ]; then
        build_task="assembleRelease"
        gradle_flags="-PuseLlamaCpp=true"
    fi
    
    if [ "$NATIVE_BUILD" = true ]; then
        gradle_flags="$gradle_flags -PuseLlamaCpp=true -PuseFunctionGemma=true"
    fi
    
    # Build APK
    $gradlew :app:$build_task $gradle_flags --stacktrace
    
    log_success "APK build completed"
}

verify_apk() {
    log_step "Verifying APK..."
    
    local apk_path
    if [ "$BUILD_TYPE" = "release" ]; then
        apk_path="$PROJECT_DIR/app/build/outputs/apk/release"
    else
        apk_path="$PROJECT_DIR/app/build/outputs/apk/debug"
    fi
    
    if [ ! -d "$apk_path" ]; then
        log_error "APK output directory not found: $apk_path"
        exit 1
    fi
    
    # Find APK file
    local apk_file=$(find "$apk_path" -name "*functiongemma*.apk" -o -name "*debug*.apk" -o -name "*release*.apk" 2>/dev/null | head -1)
    
    if [ -z "$apk_file" ]; then
        apk_file=$(find "$apk_path" -name "*.apk" | head -1)
    fi
    
    if [ -z "$apk_file" ] || [ ! -f "$apk_file" ]; then
        log_error "No APK file found in $apk_path"
        exit 1
    fi
    
    local apk_size=$(get_file_size "$apk_file")
    log_info "APK file: $(basename "$apk_file")"
    log_info "APK size: $apk_size"
    
    # Verify size is reasonable (FunctionGemma should be < 100MB without model)
    local size_bytes=$(stat -c%s "$apk_file" 2>/dev/null || stat -f%z "$apk_file" 2>/dev/null || echo "0")
    local size_mb=$((size_bytes / 1024 / 1024))
    
    if [ "$size_mb" -gt 150 ]; then
        log_warn "APK size ($size_mb MB) is larger than expected"
        log_warn "Consider enabling ProGuard/R8 for release builds"
    fi
    
    echo ""
    log_info "APK Verification:"
    echo "  File: $(basename "$apk_file")"
    echo "  Size: $apk_size"
    echo "  Path: $apk_file"
    
    # Copy to output directory
    mkdir -p "$OUTPUT_DIR"
    cp "$apk_file" "$OUTPUT_DIR/"
    
    log_success "APK copied to: $OUTPUT_DIR/$(basename "$apk_file")"
}

copy_native_libs() {
    if [ "$NATIVE_BUILD" = true ]; then
        log_step "Copying native libraries..."
        
        mkdir -p "$OUTPUT_DIR/libs"
        
        # Copy built .so files
        if [ -d "$PROJECT_DIR/app/.cxx/Release" ]; then
            find "$PROJECT_DIR/app/.cxx/Release" -name "*.so" -exec cp {} "$OUTPUT_DIR/libs/" \;
            log_info "Native libraries copied to: $OUTPUT_DIR/libs/"
        fi
    fi
}

print_summary() {
    print_header "Build Summary"
    
    echo "  Build Type: $BUILD_TYPE"
    echo "  Native Build: $NATIVE_BUILD"
    echo "  Output Directory: $OUTPUT_DIR"
    echo ""
    
    if [ -d "$OUTPUT_DIR" ]; then
        echo "  Generated Files:"
        ls -lh "$OUTPUT_DIR" | tail -n +2 | while read line; do
            echo "    $line"
        done
    fi
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  FunctionGemma build completed!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Download model: ./scripts/download_functiongemma_model.sh"
    echo "  2. Deploy to device: ./scripts/deploy_functiongemma.sh"
    echo "  3. Install APK: adb install $OUTPUT_DIR/*.apk"
    echo ""
}

show_help() {
    echo "FunctionGemma Build Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --clean     Clean previous build artifacts before building"
    echo "  --release   Build release APK (default: debug)"
    echo "  --native    Build with native llama.cpp libraries"
    echo "  --help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                          # Build debug APK"
    echo "  $0 --clean                  # Clean build (debug)"
    echo "  $0 --release                # Build release APK"
    echo "  $0 --clean --release        # Clean release build"
    echo "  $0 --native                 # Build with native libraries"
    echo ""
}

# ============================================================================
# Main
# ============================================================================

main() {
    print_header "FunctionGemma Build Script"
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --clean)
                CLEAN_BUILD=true
                shift
                ;;
            --release)
                BUILD_TYPE="release"
                shift
                ;;
            --native)
                NATIVE_BUILD=true
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
    
    echo "Build Configuration:"
    echo "  Build Type: $BUILD_TYPE"
    echo "  Clean Build: $CLEAN_BUILD"
    echo "  Native Build: $NATIVE_BUILD"
    echo ""
    
    # Check prerequisites
    check_prerequisites
    
    # Clean if requested
    if [ "$CLEAN_BUILD" = true ]; then
        clean_build
    fi
    
    # Build native libraries if requested
    if [ "$NATIVE_BUILD" = true ]; then
        build_native_libraries
    fi
    
    # Build APK
    build_apk
    
    # Verify and copy APK
    verify_apk
    
    # Copy native libraries
    copy_native_libs
    
    # Print summary
    print_summary
}

# Run main function
main "$@"
