# Llama 3.2 3B Model Integration Guide

## Overview

The Egyptian Agent now supports Llama 3.2 3B Q4_K_M model integration for enhanced Egyptian dialect understanding. This guide explains how to properly set up the native library for full functionality.

## Prerequisites

To build the native Llama library, you'll need:

1. **llama.cpp repository**: Clone the llama.cpp repository into the project root:
   ```bash
   cd K:\projects\mobile\EgyptianAgent
   git clone https://github.com/ggerganov/llama.cpp.git
   ```

2. **Android NDK**: Ensure you have the Android NDK installed and configured in your SDK

3. **CMake**: Version 3.22.1 or higher

## Building the Native Library

### Step 1: Get the Llama Model

Download the Llama 3.2 3B Q4_K_M model in GGUF format:

1. Obtain the model file (e.g., `llama-3.2-3b-Q4_K_M.gguf`)
2. Place it in `app/src/main/assets/model/`

### Step 2: Build llama.cpp

1. Navigate to the llama.cpp directory:
   ```bash
   cd llama.cpp
   ```

2. Build for Android:
   ```bash
   # For arm64-v8a
   make LLAMA_ANDROID=1 CC=/path/to/ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang
   ```

### Step 3: Build the Project

Once the prerequisites are met, build the project normally:

```bash
./gradlew :app:assembleDebug
```

## Current Implementation Status

The current implementation includes:

- **Java Layer**: Complete integration with error handling and fallback mechanisms
- **Native Stub**: A mock implementation that simulates the native library behavior
- **CMake Configuration**: Properly configured for native builds when dependencies are available

When the native library is not available, the system automatically falls back to the OpenPhone model while maintaining all functionality.

## Troubleshooting

### Missing Native Library

If you see warnings about the native library not loading, ensure:

1. The `llama.cpp` directory exists in the project root
2. The native library was compiled for the correct architecture (arm64-v8a, armeabi-v7a)
3. The `.so` files are in the correct `jniLibs` directory

### Build Errors

Common build errors and solutions:

- **CMake Error**: Ensure CMakeLists.txt paths are correct relative to build directory
- **NDK Not Found**: Verify NDK path in `local.properties`
- **Architecture Mismatch**: Ensure native libraries match target device architecture

## Production Deployment

For production deployment:

1. Ensure the Llama model file is properly compressed and optimized
2. Verify the native library is built for all target architectures
3. Test on target device (Honor X6c) to ensure performance meets requirements
4. Validate Egyptian dialect accuracy meets the 95%+ target

## Fallback Behavior

If the native Llama library is not available:

1. The system will log a warning
2. All functionality will continue to work using the OpenPhone model
3. Users will hear a TTS notification about using default settings
4. Performance and accuracy may be reduced compared to the Llama model

## Performance Notes

- The Q4_K_M quantization reduces model size to ~1.64GB while maintaining quality
- For 6GB RAM devices like the Honor X6c, monitor memory usage during inference
- The system includes memory optimization features specifically for this hardware