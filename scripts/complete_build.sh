#!/bin/bash
# Complete build script for Egyptian Agent with Llama 3.2 3B integration
# This script handles model setup, native library compilation, and APK building

set -e  # Exit on any error

echo "=== Egyptian Agent - Complete Build Pipeline ==="
echo "Target: Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM)"
echo ""

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo "Checking prerequisites..."

if ! command_exists python3; then
    echo "Error: python3 is required but not installed."
    exit 1
fi

if ! command_exists pip; then
    echo "Error: pip is required but not installed."
    exit 1
fi

if ! command_exists git; then
    echo "Error: git is required but not installed."
    exit 1
fi

if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME environment variable is required."
    echo "Set it to your Android SDK installation path."
    exit 1
fi

if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$ANDROID_NDK_ROOT" ]; then
    echo "Error: ANDROID_NDK_HOME or ANDROID_NDK_ROOT environment variable is required."
    echo "Set it to your Android NDK installation path."
    exit 1
fi

# Use ANDROID_NDK_ROOT if ANDROID_NDK_HOME is not set
if [ -z "$ANDROID_NDK_HOME" ]; then
    ANDROID_NDK_HOME=$ANDROID_NDK_ROOT
fi

echo "✓ All prerequisites met"
echo ""

# Step 1: Setup the Llama model
echo "Step 1: Setting up Llama 3.2 3B model..."
if [ ! -f "app/src/main/assets/model/llama-3.2-3b-Q4_K_M.gguf" ]; then
    echo "Model not found, running setup script..."
    chmod +x scripts/setup_llama_model.sh
    ./scripts/setup_llama_model.sh
else
    echo "✓ Model already exists: $(ls -lh app/src/main/assets/model/llama-3.2-3b-Q4_K_M.gguf | awk '{print $5}')"
fi
echo ""

# Step 2: Build native libraries
echo "Step 2: Building native libraries..."
NEEDS_BUILD=false

# Check if native libraries exist
if [ ! -f "app/src/main/jniLibs/arm64-v8a/libllama_native.so" ] || [ ! -f "app/src/main/jniLibs/armeabi-v7a/libllama_native.so" ]; then
    NEEDS_BUILD=true
else
    # Check if source files are newer than built libraries
    if [ "app/src/main/cpp/llama_native.cpp" -nt "app/src/main/jniLibs/arm64-v8a/libllama_native.so" ]; then
        NEEDS_BUILD=true
    fi
fi

if [ "$NEEDS_BUILD" = true ]; then
    echo "Native libraries need to be rebuilt..."
    chmod +x scripts/build_native_libs.sh
    ./scripts/build_native_libs.sh
else
    echo "✓ Native libraries already exist and are up to date"
fi
echo ""

# Step 3: Build the Android application
echo "Step 3: Building Android application..."
echo "Running: ./gradlew :app:assembleDebug"

# Make gradlew executable
chmod +x gradlew

# Build the app
./gradlew :app:assembleDebug

echo ""

# Step 4: Verify build success
echo "Step 4: Verifying build..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✓ APK built successfully!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
    echo "APK size: $(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')"
    
    # Show some build statistics
    echo ""
    echo "Build Statistics:"
    echo "- Model size: $(ls -lh app/src/main/assets/model/llama-3.2-3b-Q4_K_M.gguf | awk '{print $5}')"
    echo "- APK size: $(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')"
    echo "- Native libs: $(ls -la app/src/main/jniLibs/*/libllama_native.so | wc -l) architectures"
    
    echo ""
    echo "=== Egyptian Agent Build Complete! ==="
    echo "To install on device: adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "For production release: ./gradlew :app:assembleRelease"
else
    echo "✗ Error: APK build failed"
    exit 1
fi