#!/bin/bash

# Production build script for Egyptian Agent
# Builds the optimized APK for deployment on Honor X6c devices

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting Egyptian Agent production build...${NC}"

# Check if we're in the right directory
if [ ! -f "build.gradle" ] || [ ! -f "app/build.gradle" ]; then
    echo -e "${RED}Error: Not in project root directory${NC}"
    exit 1
fi

# Check if required tools are available
if ! command -v ./gradlew &> /dev/null; then
    echo -e "${RED}Error: Gradle wrapper not found${NC}"
    exit 1
fi

if ! command -v adb &> /dev/null; then
    echo -e "${YELLOW}Warning: ADB not found, skipping device checks${NC}"
fi

# Build configuration
BUILD_TYPE="release"
TARGET_DEVICE="honor-x6c"
BUILD_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BUILD_DIR="build_production_${BUILD_TIMESTAMP}"

echo -e "${GREEN}Build configuration:${NC}"
echo "  - Build Type: $BUILD_TYPE"
echo "  - Target Device: $TARGET_DEVICE"
echo "  - Timestamp: $BUILD_TIMESTAMP"

# Create build directory
mkdir -p "$BUILD_DIR"

# Clean previous builds
echo -e "${GREEN}Cleaning previous builds...${NC}"
./gradlew clean

# Run tests before building
echo -e "${GREEN}Running tests...${NC}"
./gradlew test

# Build the APK with optimizations
echo -e "${GREEN}Building optimized APK...${NC}"
./gradlew assembleRelease --no-daemon

# Verify the APK was created
APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}Error: APK was not created at $APK_PATH${NC}"
    exit 1
fi

# Optimize the APK using zipalign
echo -e "${GREEN}Optimizing APK with zipalign...${NC}"
ZIPALIGNED_APK="$BUILD_DIR/egyptian_agent_${TARGET_DEVICE}_${BUILD_TIMESTAMP}_optimized.apk"
zipalign -v 4 "$APK_PATH" "$ZIPALIGNED_APK"

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: zipalign failed${NC}"
    exit 1
fi

# Generate APK signature
echo -e "${GREEN}Signing APK...${NC}"
SIGNED_APK="$BUILD_DIR/egyptian_agent_${TARGET_DEVICE}_${BUILD_TIMESTAMP}_signed.apk"

# Use the debug keystore for now (in production, use a proper release keystore)
if [ -f "keystore/debug.keystore" ]; then
    KEYSTORE_PATH="keystore/debug.keystore"
else
    # Create a debug keystore if it doesn't exist
    echo -e "${YELLOW}Creating debug keystore...${NC}"
    mkdir -p keystore
    keytool -genkey -v -keystore keystore/debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000
    KEYSTORE_PATH="keystore/debug.keystore"
fi

apksigner sign --ks "$KEYSTORE_PATH" --out "$SIGNED_APK" "$ZIPALIGNED_APK"

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: APK signing failed${NC}"
    exit 1
fi

# Verify the signed APK
apksigner verify "$SIGNED_APK"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: APK verification failed${NC}"
    exit 1
fi

echo -e "${GREEN}Build successful!${NC}"
echo "  - Signed APK: $SIGNED_APK"
echo "  - Size: $(du -h "$SIGNED_APK" | cut -f1)"

# Create a build info file
cat > "$BUILD_DIR/build_info.txt" << EOF
Egyptian Agent Production Build
==============================

Build Date: $(date)
Build Type: $BUILD_TYPE
Target Device: $TARGET_DEVICE
Build Timestamp: $BUILD_TIMESTAMP
APK Path: $SIGNED_APK
APK Size: $(du -h "$SIGNED_APK" | cut -f1)

Git Commit: $(git rev-parse HEAD 2>/dev/null || echo "unknown")
Git Branch: $(git branch --show-current 2>/dev/null || echo "unknown")

Build Environment:
  - OS: $(uname -s)
  - Architecture: $(uname -m)
  - Gradle Version: $(./gradlew --version | grep "Gradle" | cut -d' ' -f2)
EOF

echo -e "${GREEN}Build information saved to: $BUILD_DIR/build_info.txt${NC}"

# Optional: Install on connected device
if command -v adb &> /dev/null && adb devices | grep -q "device$"; then
    echo -e "${YELLOW}Installing on connected device...${NC}"
    adb install -r "$SIGNED_APK"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully installed on device${NC}"
    else
        echo -e "${RED}Failed to install on device${NC}"
    fi
else
    echo -e "${YELLOW}No connected device found, skipping installation${NC}"
fi

echo -e "${GREEN}Production build completed successfully!${NC}"
echo "Output APK: $SIGNED_APK"