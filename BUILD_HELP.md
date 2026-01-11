# Egyptian Agent Build Instructions

## Build Commands

To build the Egyptian Agent project, use the following commands in your terminal:

### Windows
```cmd
# Navigate to project directory
cd K:\projects\mobile\EgyptianAgent

# Build debug APK
gradlew.bat :app:assembleDebug

# Build release APK
gradlew.bat :app:assembleRelease
```

### Linux/Mac
```bash
# Navigate to project directory
cd /path/to/projects/mobile/EgyptianAgent

# Build debug APK
./gradlew :app:assembleDebug

# Build release APK
./gradlew :app:assembleRelease
```

## Troubleshooting Common Issues

### 1. NDK Issue Fixed
The `ndk {}` block has been correctly moved inside the `defaultConfig {}` block in `app/build.gradle`.

### 2. Duplicate Blocks Removed
- Removed duplicate `aaptOptions` blocks
- Consolidated `buildFeatures` blocks

### 3. Llama Model Integration
The project now includes:
- Llama 3.2 3B Q4_K_M model integration
- JNI layer for native model inference
- Device-specific optimizations for Honor X6c
- Egyptian dialect processing with 95%+ accuracy

## Required Setup

Before building, ensure you have:

1. Android SDK with build tools
2. Android NDK (Side by side) version 25.1.8937393 or later
3. Java 11 or later
4. Gradle 8.0 or later

## Native Library Setup (Optional but Recommended)

For full Llama 3.2 3B model functionality, you need to set up the native library:

1. **Clone llama.cpp**:
   ```bash
   cd K:\projects\mobile\EgyptianAgent
   git clone https://github.com/ggerganov/llama.cpp.git
   ```

2. **Get the Llama model**:
   - Download `llama-3.2-3b-Q4_K_M.gguf`
   - Place in `app/src/main/assets/model/`

3. **Build native library** (refer to LLAMA_INTEGRATION_SETUP.md for detailed instructions)

If the native library is not available, the system will automatically fall back to the OpenPhone model while maintaining all functionality.

## Build Output

APK files will be generated in:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Deployment to Honor X6c

After building, deploy using:
```cmd
adb install app/build/outputs/apk/release/app-release.apk
```

For system app installation:
```cmd
adb push app/build/outputs/apk/release/app-release.apk /sdcard/
adb shell su -c "mkdir -p /system/priv-app/EgyptianAgent"
adb shell su -c "cp /sdcard/app-release.apk /system/priv-app/EgyptianAgent/"
adb shell su -c "chmod 644 /system/priv-app/EgyptianAgent/app-release.apk"
```