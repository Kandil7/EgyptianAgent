#!/bin/bash

# Egyptian Agent - Production Build and Deployment Script
# Version: 1.0.0
# Description: Complete build and deployment process for Egyptian Agent

set -e  # Exit on any error

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_TYPE="release"
TARGET_DEVICE="honor-x6c"
APK_NAME="EgyptianAgent-release.apk"
APK_PATH="app/build/outputs/apk/release/$APK_NAME"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if Android SDK is available
    if ! command -v adb &> /dev/null; then
        log_error "ADB (Android Debug Bridge) is not installed or not in PATH"
        exit 1
    fi

    # Check if Gradle is available
    if ! command -v gradle &> /dev/null; then
        log_error "Gradle is not installed or not in PATH"
        exit 1
    fi

    # Check if we're in the project directory
    if [ ! -f "settings.gradle" ] || [ ! -f "build.gradle" ]; then
        log_error "Not in the project root directory"
        exit 1
    fi

    log_info "All prerequisites satisfied"
}

# Initialize submodules
initialize_submodules() {
    log_info "Initializing submodules..."
    if [ -f "initialize_submodules.sh" ]; then
        chmod +x initialize_submodules.sh
        ./initialize_submodules.sh
    else
        git submodule update --init --recursive
    fi
    log_info "Submodules initialized"
}

# Build the application
build_application() {
    log_info "Building Egyptian Agent application..."

    # Make gradlew executable
    chmod +x gradlew

    # Clean previous builds
    log_info "Cleaning previous builds..."
    ./gradlew clean

    # Build the application
    log_info "Building release APK..."
    ./gradlew assembleRelease

    # Verify APK was created
    if [ ! -f "$APK_PATH" ]; then
        log_error "APK was not created at expected location: $APK_PATH"
        exit 1
    fi

    log_info "Application built successfully: $APK_PATH"
    log_info "APK Size: $(du -h "$APK_PATH" | cut -f1)"
}

# Verify build artifacts
verify_build() {
    log_info "Verifying build artifacts..."

    # Check if APK is valid
    if ! aapt dump badging "$APK_PATH" &> /dev/null; then
        log_error "APK appears to be invalid"
        exit 1
    fi

    # Check for required permissions
    permissions=$(aapt dump permissions "$APK_PATH" | grep -o "uses-permission: name=" | wc -l)
    if [ "$permissions" -lt 5 ]; then
        log_warn "APK has fewer permissions than expected: $permissions"
    else
        log_info "APK has required permissions: $permissions"
    fi

    log_info "Build verification completed"
}

# Deploy to device
deploy_to_device() {
    log_info "Deploying to device..."

    # Check if device is connected
    device_count=$(adb devices | grep -c "device$")
    if [ "$device_count" -eq 0 ]; then
        log_error "No Android device connected via ADB"
        exit 1
    fi

    log_info "Connected devices: $device_count"

    # Check if device has root access
    root_check=$(adb shell "su -c 'echo root_access_test'" 2>/dev/null | grep -c "root_access_test")
    if [ "$root_check" -eq 0 ]; then
        log_error "Device does not have root access (required for Egyptian Agent)"
        exit 1
    fi

    log_info "Root access confirmed"

    # Install as system app
    log_info "Installing as system app..."
    
    # Push APK to device
    adb push "$APK_PATH" /sdcard/
    
    # Create system app directory and move APK
    adb shell su -c "mkdir -p /system/priv-app/EgyptianAgent"
    adb shell su -c "cp /sdcard/$APK_NAME /system/priv-app/EgyptianAgent/"
    adb shell su -c "chmod 644 /system/priv-app/EgyptianAgent/$APK_NAME"
    
    log_info "APK installed as system app"
}

# Apply device-specific optimizations
apply_device_optimizations() {
    log_info "Applying device-specific optimizations..."

    # Apply Honor X6c battery optimizations
    if [ -f "scripts/honor_battery_fix.sh" ]; then
        log_info "Applying Honor battery optimizations..."
        adb push scripts/honor_battery_fix.sh /sdcard/
        adb shell su -c "sh /sdcard/honor_battery_fix.sh"
    else
        log_warn "Honor battery fix script not found, skipping..."
    fi

    # Set appropriate permissions for system app
    adb shell su -c "pm grant com.egyptian.agent android.permission.RECORD_AUDIO"
    adb shell su -c "pm grant com.egyptian.agent android.permission.CALL_PHONE"
    adb shell su -c "pm grant com.egyptian.agent android.permission.READ_CONTACTS"
    adb shell su -c "pm grant com.egyptian.agent android.permission.BODY_SENSORS"
    
    log_info "Device optimizations applied"
}

# Restart services
restart_services() {
    log_info "Restarting services..."

    # Force stop and restart the app
    adb shell am force-stop com.egyptian.agent
    sleep 2
    adb shell am start -n com.egyptian.agent/.MainActivity
    
    log_info "Services restarted"
}

# Verify installation
verify_installation() {
    log_info "Verifying installation..."

    # Check if app is installed
    app_installed=$(adb shell pm list packages | grep -c "com.egyptian.agent")
    if [ "$app_installed" -eq 0 ]; then
        log_error "Egyptian Agent not found on device"
        exit 1
    fi

    # Check if it's running as system app
    app_info=$(adb shell dumpsys package com.egyptian.agent | grep -i "system")
    if [ -n "$app_info" ]; then
        log_info "Egyptian Agent installed as system app"
    else
        log_warn "Egyptian Agent may not be installed as system app"
    fi

    # Check if services are running
    service_running=$(adb shell dumpsys activity services | grep -c "EgyptianAgent")
    if [ "$service_running" -gt 0 ]; then
        log_info "Egyptian Agent services are running"
    else
        log_warn "Egyptian Agent services may not be running"
    fi

    log_info "Installation verification completed"
}

# Main execution
main() {
    log_info "Starting Egyptian Agent build and deployment process..."

    check_prerequisites
    initialize_submodules
    build_application
    verify_build
    deploy_to_device
    apply_device_optimizations
    restart_services
    verify_installation

    log_info "Build and deployment completed successfully!"
    log_info "Egyptian Agent is now installed and running on your device."
    log_info "Say 'يا صاحبي' to activate the assistant."
}

# Run main function
main "$@"