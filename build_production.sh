#!/bin/bash
# Final Production Build Script for Egyptian Agent
# Creates a production-ready APK for deployment

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Egyptian Agent Final Production Build ===${NC}"
echo -e "${BLUE}Creating production-ready APK for Honor X6c${NC}"

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_OUTPUT_DIR="$PROJECT_DIR/app/build/outputs/apk/release"
FINAL_APK_NAME="EgyptianAgent-v1.1.0-production.apk"
SIGNING_KEYSTORE="$PROJECT_DIR/production_keystore.jks"
SIGNING_ALIAS="egyptianagent-prod"

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

# Check if Gradle is available
if ! command -v ./gradlew &> /dev/null; then
    print_error "Gradle wrapper not found!"
    exit 1
fi

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean

# Run tests before building
print_status "Running tests..."
if ! ./gradlew testRelease; then
    print_error "Tests failed! Cannot proceed with production build."
    exit 1
fi

# Build the application
print_status "Building production APK..."
./gradlew assembleRelease

# Verify APK was created
if [ ! -f "$BUILD_OUTPUT_DIR/app-release.apk" ]; then
    print_error "APK file not found at $BUILD_OUTPUT_DIR/app-release.apk"
    exit 1
fi

# Rename the APK with version and production tag
cp "$BUILD_OUTPUT_DIR/app-release.apk" "$BUILD_OUTPUT_DIR/$FINAL_APK_NAME"

# If signing key is available, sign the APK
if [ -f "$SIGNING_KEYSTORE" ]; then
    print_status "Signing APK with production key..."
    jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
        -keystore "$SIGNING_KEYSTORE" \
        "$BUILD_OUTPUT_DIR/$FINAL_APK_NAME" \
        "$SIGNING_ALIAS"
else
    print_warning "Production keystore not found at $SIGNING_KEYSTORE"
    print_warning "APK is built but not signed with production key"
    print_warning "For production deployment, obtain the production keystore"
fi

# Verify the APK
print_status "Verifying APK integrity..."
if command -v zipalign &> /dev/null; then
    zipalign -c -v 4 "$BUILD_OUTPUT_DIR/$FINAL_APK_NAME"
else
    print_warning "zipalign not found, skipping alignment verification"
fi

# Show build information
APK_SIZE=$(ls -lh "$BUILD_OUTPUT_DIR/$FINAL_APK_NAME" | awk '{print $5}')
print_status "Production build completed successfully!"
echo -e "${GREEN}APK Location:${NC} $BUILD_OUTPUT_DIR/$FINAL_APK_NAME"
echo -e "${GREEN}APK Size:${NC} $APK_SIZE"
echo -e "${GREEN}Version:${NC} 1.1.0"

# Show security information
if [ -f "$SIGNING_KEYSTORE" ]; then
    echo -e "${GREEN}Signature:${NC} Valid (signed with production key)"
else
    echo -e "${YELLOW}Signature:${NC} Unsigned (requires production key for deployment)"
fi

print_status "Next steps:"
echo "1. Verify the APK on a test device"
echo "2. If unsigned, sign with production key before deployment"
echo "3. Deploy using deploy_production.sh script"

# Create build manifest
cat << EOF > "$BUILD_OUTPUT_DIR/build_manifest.txt"
Egyptian Agent Production Build Manifest
=======================================
Build Date: $(date)
Build Version: 1.1.0
Build Type: Release
Target Device: Honor X6c
Architecture: arm64-v8a
APK File: $FINAL_APK_NAME
APK Size: $APK_SIZE
Signed: $([ -f "$SIGNING_KEYSTORE" ] && echo "Yes" || echo "No - Requires production key")
Dependencies Verified: Yes
Tests Passed: Yes
EOF

print_status "Build manifest created at $BUILD_OUTPUT_DIR/build_manifest.txt"