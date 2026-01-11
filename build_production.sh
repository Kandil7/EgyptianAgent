#!/bin/bash
# Production Build Script for Egyptian Agent
# Builds the Egyptian Agent app for production deployment on Honor X6c devices

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Egyptian Agent Production Build Script ===${NC}"
echo -e "${BLUE}Building for Honor X6c (MediaTek Helio G81 Ultra)${NC}"

# Check if Gradle is available
if ! command -v ./gradlew &> /dev/null; then
    echo -e "${RED}Gradle wrapper not found!${NC}"
    exit 1
fi

# Function to print status
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Parse command line arguments
BUILD_TYPE="release"
TARGET_DEVICE="honor-x6c"
CLEAN_BUILD=false
INSTALL_ON_DEVICE=false
SIGNING_KEYSTORE=""
SIGNING_ALIAS=""
SIGNING_PASSWORD=""

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
        --keystore)
            SIGNING_KEYSTORE="$2"
            shift 2
            ;;
        --alias)
            SIGNING_ALIAS="$2"
            shift 2
            ;;
        --password)
            SIGNING_PASSWORD="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--debug|--release] [--target <device>] [--clean] [--install] [--keystore <path>] [--alias <alias>] [--password <password>]"
            exit 1
            ;;
    esac
done

print_status "Build configuration:"
echo "  Build Type: $BUILD_TYPE"
echo "  Target Device: $TARGET_DEVICE"
echo "  Clean Build: $CLEAN_BUILD"
echo "  Install on Device: $INSTALL_ON_DEVICE"

# Validate signing credentials if provided
if [ -n "$SIGNING_KEYSTORE" ] && [ -n "$SIGNING_ALIAS" ] && [ -n "$SIGNING_PASSWORD" ]; then
    if [ ! -f "$SIGNING_KEYSTORE" ]; then
        print_error "Keystore file not found: $SIGNING_KEYSTORE"
        exit 1
    fi
    print_status "Using custom signing configuration"
else
    if [ "$BUILD_TYPE" = "release" ]; then
        print_warning "No signing credentials provided. Using debug keystore for release build (NOT FOR PRODUCTION)."
    fi
fi

# Clean build if requested
if [ "$CLEAN_BUILD" = true ]; then
    print_status "Cleaning previous build..."
    ./gradlew clean
fi

# Configure signing if provided
if [ -n "$SIGNING_KEYSTORE" ] && [ -n "$SIGNING_ALIAS" ] && [ -n "$SIGNING_PASSWORD" ]; then
    print_status "Configuring custom signing..."
    # Create temporary gradle properties for signing
    echo "MYAPP_RELEASE_STORE_FILE=$SIGNING_KEYSTORE" >> gradle.properties
    echo "MYAPP_RELEASE_KEY_ALIAS=$SIGNING_ALIAS" >> gradle.properties
    echo "MYAPP_RELEASE_STORE_PASSWORD=$SIGNING_PASSWORD" >> gradle.properties
    echo "MYAPP_RELEASE_KEY_PASSWORD=$SIGNING_PASSWORD" >> gradle.properties
fi

# Build the application
print_status "Building Egyptian Agent ($BUILD_TYPE) for $TARGET_DEVICE..."

if [ "$BUILD_TYPE" = "release" ]; then
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

print_status "Build completed successfully!"
print_status "APK location: $APK_PATH"

# Check if APK was created
if [ ! -f "$APK_PATH" ]; then
    print_error "APK file not found at $APK_PATH"
    exit 1
fi

# Verify APK integrity
print_status "Verifying APK integrity..."
if command -v zipalign &> /dev/null; then
    if zipalign -c -v 4 "$APK_PATH"; then
        print_status "APK integrity check passed"
    else
        print_error "APK integrity check failed"
        exit 1
    fi
else
    print_warning "zipalign not found, skipping integrity check"
fi

# Install on device if requested
if [ "$INSTALL_ON_DEVICE" = true ]; then
    print_status "Installing on connected device..."

    # Check if ADB is available
    if ! command -v adb &> /dev/null; then
        print_error "ADB not found! Please install Android SDK platform-tools."
        exit 1
    fi

    # Check if device is connected
    DEVICE_COUNT=$(adb devices | grep -c "device$")
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        print_error "No connected devices found!"
        exit 1
    fi

    # Install the APK
    adb install -r "$APK_PATH"

    if [ $? -eq 0 ]; then
        print_status "APK installed successfully!"
    else
        print_error "Failed to install APK!"
        exit 1
    fi
fi

# For system app installation (requires root)
if [ "$BUILD_TYPE" = "release" ] && [ "$INSTALL_ON_DEVICE" = true ]; then
    echo ""
    print_warning "For system-level installation (required for full functionality):"
    echo "  1. Ensure device is rooted with Magisk"
    echo "  2. Run: adb push $APK_PATH /sdcard/"
    echo "  3. Run: adb shell su -c 'mkdir -p /system/priv-app/EgyptianAgent'"
    echo "  4. Run: adb shell su -c 'cp /sdcard/$(basename $APK_PATH) /system/priv-app/EgyptianAgent/'"
    echo "  5. Run: adb shell su -c 'chmod 644 /system/priv-app/EgyptianAgent/$(basename $APK_PATH)'"
    echo "  6. Reboot device"
fi

# Create checksum for verification
print_status "Creating checksum for verification..."
sha256sum "$APK_PATH" > "${APK_PATH}.sha256"
print_status "Checksum saved to: ${APK_PATH}.sha256"

# Clean up temporary signing properties if added
if [ -n "$SIGNING_KEYSTORE" ]; then
    sed -i '/MYAPP_RELEASE_STORE_FILE/d' gradle.properties
    sed -i '/MYAPP_RELEASE_KEY_ALIAS/d' gradle.properties
    sed -i '/MYAPP_RELEASE_STORE_PASSWORD/d' gradle.properties
    sed -i '/MYAPP_RELEASE_KEY_PASSWORD/d' gradle.properties
    print_status "Cleaned up temporary signing properties"
fi

print_status "Production build process completed!"