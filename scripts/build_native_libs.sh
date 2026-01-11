#!/bin/bash
# Script to build the native Llama library for Egyptian Agent
# This script compiles the native library for Android

set -e  # Exit on any error

echo "=== Egyptian Agent - Native Library Build ==="

# Check if required tools are available
if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$ANDROID_NDK_ROOT" ]; then
    echo "Error: ANDROID_NDK_HOME or ANDROID_NDK_ROOT environment variable is required."
    echo "Set it to your Android NDK installation path."
    exit 1
fi

# Use ANDROID_NDK_ROOT if ANDROID_NDK_HOME is not set
if [ -z "$ANDROID_NDK_HOME" ]; then
    ANDROID_NDK_HOME=$ANDROID_NDK_ROOT
fi

echo "Using Android NDK at: $ANDROID_NDK_HOME"

# Check if llama.cpp exists
if [ ! -d "llama.cpp" ]; then
    echo "Error: llama.cpp directory not found."
    echo "Run scripts/setup_llama_model.sh first to download llama.cpp"
    exit 1
fi

# Create build directories
mkdir -p app/.cxx
mkdir -p app/libs/arm64-v8a
mkdir -p app/libs/armeabi-v7a

echo "Building llama.cpp for Android..."

# Build for arm64-v8a (64-bit ARM)
echo "Building for arm64-v8a..."
cmake -S llama.cpp -B llama.cpp/android_build_arm64 \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release \
    -DLLAMA_CURL=ON \
    -DLLAMA_BLAS=ON \
    -DLLAMA_BLAS_VENDOR=OpenBLAS

cmake --build llama.cpp/android_build_arm64 --parallel

# Build for armeabi-v7a (32-bit ARM)
echo "Building for armeabi-v7a..."
cmake -S llama.cpp -B llama.cpp/android_build_arm32 \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=armeabi-v7a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release \
    -DLLAMA_CURL=ON \
    -DLLAMA_BLAS=ON \
    -DLLAMA_BLAS_VENDOR=OpenBLAS

cmake --build llama.cpp/android_build_arm32 --parallel

# Copy the built libraries to the appropriate directories
echo "Copying native libraries to Android project..."
cp llama.cpp/android_build_arm64/libllama.so app/src/main/jniLibs/arm64-v8a/libllama.so
cp llama.cpp/android_build_arm64/libggml.so app/src/main/jniLibs/arm64-v8a/libggml.so
cp llama.cpp/android_build_arm64/libllava.so app/src/main/jniLibs/arm64-v8a/libllava.so 2>/dev/null || true

cp llama.cpp/android_build_arm32/libllama.so app/src/main/jniLibs/armeabi-v7a/libllama.so
cp llama.cpp/android_build_arm32/libggml.so app/src/main/jniLibs/armeabi-v7a/libggml.so
cp llama.cpp/android_build_arm32/libllava.so app/src/main/jniLibs/armeabi-v7a/libllava.so 2>/dev/null || true

# Build the main native library that wraps llama functionality
echo "Building main native wrapper library..."

# Create the main native library build
cmake -S . -B app/.cxx/build \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release

cmake --build app/.cxx/build --parallel

# Copy the main native library
cp app/.cxx/build/libllama_native.so app/src/main/jniLibs/arm64-v8a/libllama_native.so

# Also build for 32-bit if needed
cmake -S . -B app/.cxx/build32 \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=armeabi-v7a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release

cmake --build app/.cxx/build32 --parallel

cp app/.cxx/build32/libllama_native.so app/src/main/jniLibs/armeabi-v7a/libllama_native.so

echo "=== Native library build completed successfully! ==="
echo "Libraries copied to:"
echo "  - app/src/main/jniLibs/arm64-v8a/"
echo "  - app/src/main/jniLibs/armeabi-v7a/"

# Verify the libraries exist
if [ -f "app/src/main/jniLibs/arm64-v8a/libllama_native.so" ] && [ -f "app/src/main/jniLibs/armeabi-v7a/libllama_native.so" ]; then
    echo "✓ Native libraries built successfully"
else
    echo "✗ Error: Native libraries not found"
    exit 1
fi

echo ""
echo "Next steps:"
echo "1. Build the Android app: ./gradlew :app:assembleDebug"
echo "2. Install on device: adb install app/build/outputs/apk/debug/app-debug.apk"