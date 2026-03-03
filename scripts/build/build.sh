#!/bin/bash
# Egyptian Agent Build Script
# Builds the Egyptian Agent app for Honor X6c devices
# Cross-platform: Linux, macOS, Windows (Git Bash/WSL)

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Egyptian Agent Build Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Target: Honor X6c (MediaTek Helio G81 Ultra)${NC}"
echo ""

# Detect OS
OS="$(uname -s 2>/dev/null || echo "Windows")"
log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Check if Gradle wrapper is available
GRADLEW="./gradlew"
if [ ! -f "$GRADLEW" ]; then
    if [ -f "gradlew.bat" ]; then
        GRADLEW="./gradlew.bat"
        log_info "Using gradlew.bat (Windows)"
    else
        log_error "Gradle wrapper not found!"
        log_error "Please ensure you're in the project root directory."
        exit 1
    fi
fi

# Make gradlew executable (Unix only)
if [[ "$OS" != "Windows"* ]] && [[ "$OS" != "MINGW"* ]]; then
    chmod +x "$GRADLEW"
fi

# Parse command line arguments
BUILD_TYPE="debug"
TARGET_DEVICE="honor-x6c"
CLEAN_BUILD=false
INSTALL_ON_DEVICE=false
NATIVE_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --debug)
            BUILD_TYPE="debug"
            shift
            ;;
        --release)
            BUILD_TYPE="release"
            shift
            ;;
        --target)
            TARGET_DEVICE="$2"
            shift 2
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --install)
            INSTALL_ON_DEVICE=true
            shift
            ;;
        --native)
            NATIVE_BUILD=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --debug       Build debug APK (default)"
            echo "  --release     Build release APK"
            echo "  --target      Target device (default: honor-x6c)"
            echo "  --clean       Clean before building"
            echo "  --install     Install on connected device"
            echo "  --native      Build with native libraries (llama.cpp)"
            echo "  -h, --help    Show this help"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

log_step "Build Configuration:"
echo "  Build Type: $BUILD_TYPE"
echo "  Target Device: $TARGET_DEVICE"
echo "  Clean Build: $CLEAN_BUILD"
echo "  Install on Device: $INSTALL_ON_DEVICE"
echo "  Native Build: $NATIVE_BUILD"
echo ""

# Clean build if requested
if [ "$CLEAN_BUILD" = true ]; then
    log_step "Cleaning previous build..."
    "$GRADLEW" clean
fi

# Initialize submodules if native build requested
if [ "$NATIVE_BUILD" = true ]; then
    log_step "Initializing native libraries..."
    if [ -f "initialize_submodules.sh" ]; then
        chmod +x initialize_submodules.sh
        ./initialize_submodules.sh
    fi
fi

# Build the application
log_step "Building Egyptian Agent ($BUILD_TYPE)..."

if [ "$BUILD_TYPE" = "release" ]; then
    if [ "$NATIVE_BUILD" = true ]; then
        "$GRADLEW" assembleRelease -PuseLlamaCpp=true -PuseWhisper=true
    else
        "$GRADLEW" assembleRelease
    fi
    APK_PATH="app/build/outputs/apk/release"
else
    if [ "$NATIVE_BUILD" = true ]; then
        "$GRADLEW" assembleDebug -PuseLlamaCpp=true -PuseWhisper=true
    else
        "$GRADLEW" assembleDebug
    fi
    APK_PATH="app/build/outputs/apk/debug"
fi

# Find the generated APK
if [ -d "$APK_PATH" ]; then
    APK_FILE=$(find "$APK_PATH" -name "*.apk" -type f | head -1)
    if [ -n "$APK_FILE" ]; then
        log_info "Build completed successfully!"
        log_info "APK location: $APK_FILE"
        log_info "APK size: $(du -h "$APK_FILE" 2>/dev/null | cut -f1 || echo "unknown")"
    else
        log_error "No APK file found in $APK_PATH"
        exit 1
    fi
else
    log_error "APK output directory not found: $APK_PATH"
    exit 1
fi

# Install on device if requested
if [ "$INSTALL_ON_DEVICE" = true ]; then
    log_step "Installing on connected device..."

    # Check if ADB is available
    if ! command -v adb &> /dev/null; then
        log_error "ADB not found! Please install Android SDK platform-tools."
        exit 1
    fi

    # Check if device is connected
    DEVICE_COUNT=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        log_error "No connected devices found!"
        exit 1
    fi

    log_info "Found $DEVICE_COUNT device(s)"
    
    # Install the APK
    adb install -r "$APK_FILE"

    if [ $? -eq 0 ]; then
        log_info "APK installed successfully!"
    else
        log_error "Failed to install APK!"
        exit 1
    fi
fi

# Summary
echo ""
log_info "=========================================="
log_info "Build Summary"
log_info "=========================================="
log_info "Build Type: $BUILD_TYPE"
log_info "Output: $APK_FILE"
log_info "=========================================="

if [ "$BUILD_TYPE" = "release" ]; then
    echo ""
    log_warn "For system-level installation (required for full functionality):"
    echo "  1. Ensure device is rooted with Magisk"
    echo "  2. Run: ./deploy_production.sh"
    echo "  Or manually:"
    echo "    adb push $APK_FILE /sdcard/"
    echo "    adb shell su -c 'mkdir -p /system/priv-app/EgyptianAgent'"
    echo "    adb shell su -c 'cp /sdcard/$(basename $APK_FILE) /system/priv-app/EgyptianAgent/'"
    echo "    adb shell su -c 'chmod 644 /system/priv-app/EgyptianAgent/$(basename $APK_FILE)'"
    echo "    adb reboot"
fi

echo ""
log_info "Build process completed!"