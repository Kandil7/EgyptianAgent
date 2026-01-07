#!/bin/bash
# Egyptian Agent Build Script

set -e

# Default values
BUILD_TYPE="release"
TARGET_DEVICE="honor-x6c"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --release)
            BUILD_TYPE="release"
            shift
            ;;
        --debug)
            BUILD_TYPE="debug"
            shift
            ;;
        --target)
            TARGET_DEVICE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--release|--debug] [--target <device>]"
            exit 1
            ;;
    esac
done

echo "Building Egyptian Agent for $TARGET_DEVICE with $BUILD_TYPE configuration..."

# Check if gradle wrapper exists
if [ ! -f "gradlew" ]; then
    echo "Error: gradlew not found. Please run this script from the project root."
    exit 1
fi

# Build the project
echo "Building APK..."
./gradlew assemble$BUILD_TYPE

echo "Build completed successfully!"
echo "APK location: app/build/outputs/apk/$BUILD_TYPE/EgyptianAgent-$BUILD_TYPE.apk"

# If target is honor-x6c, also run optimization tasks
if [ "$TARGET_DEVICE" = "honor-x6c" ]; then
    echo "Running Honor X6c optimizations..."
    ./gradlew optimizeAssets
    echo "Honor X6c optimizations completed."
fi

echo "Build process finished."