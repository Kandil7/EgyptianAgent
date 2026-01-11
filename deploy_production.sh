#!/bin/bash
# Production Deployment Script for Egyptian Agent
# Deploys the Egyptian Agent as a system app on Honor X6c devices

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Egyptian Agent Production Deployment ===${NC}"
echo -e "${BLUE}Deploying to Honor X6c (MediaTek Helio G81 Ultra)${NC}"

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."
BUILD_DIR="$PROJECT_DIR/app/build/outputs/apk"
APK_NAME="EgyptianAgent-production.apk"
SYSTEM_APP_DIR="/system/priv-app/EgyptianAgent"
BACKUP_DIR="/sdcard/egyptian_agent_backup"

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

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."

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

    # Check if device is rooted
    IS_ROOTED=$(adb shell su -c "id" 2>/dev/null | grep -c "uid=0")
    if [ "$IS_ROOTED" -eq 0 ]; then
        print_error "Device is not rooted! Egyptian Agent requires root access to function as a system app."
        exit 1
    fi

    print_status "All prerequisites met"
}

# Build the application
build_application() {
    print_status "Building Egyptian Agent application..."

    cd "$PROJECT_DIR"
    
    # Clean previous build
    ./gradlew clean
    
    # Build release APK
    ./gradlew assembleRelease
    
    # Verify APK was created
    if [ ! -f "$BUILD_DIR/release/app-release.apk" ]; then
        print_error "APK file not found at $BUILD_DIR/release/app-release.apk"
        exit 1
    fi
    
    # Rename APK for clarity
    cp "$BUILD_DIR/release/app-release.apk" "$BUILD_DIR/release/$APK_NAME"
    
    print_status "Application built successfully: $BUILD_DIR/release/$APK_NAME"
}

# Backup existing installation
backup_existing() {
    print_status "Checking for existing installation..."
    
    EXISTS=$(adb shell "[ -d $SYSTEM_APP_DIR ] && echo 'exists' || echo 'not_exists'")
    
    if [ "$EXISTS" = "exists" ]; then
        print_status "Creating backup of existing installation..."
        adb shell su -c "mkdir -p $BACKUP_DIR"
        adb shell su -c "cp -r $SYSTEM_APP_DIR $BACKUP_DIR/$(date +%Y%m%d_%H%M%S)"
        print_status "Backup created at $BACKUP_DIR"
    else
        print_status "No existing installation found"
    fi
}

# Deploy as system app
deploy_system_app() {
    print_status "Deploying Egyptian Agent as system app..."
    
    # Push APK to device
    adb push "$BUILD_DIR/release/$APK_NAME" /sdcard/
    
    # Create system app directory
    adb shell su -c "mkdir -p $SYSTEM_APP_DIR"
    
    # Copy APK to system directory
    adb shell su -c "cp /sdcard/$APK_NAME $SYSTEM_APP_DIR/EgyptianAgent.apk"
    
    # Set proper permissions
    adb shell su -c "chmod 644 $SYSTEM_APP_DIR/EgyptianAgent.apk"
    
    # Set proper ownership
    adb shell su -c "chown 0:0 $SYSTEM_APP_DIR/EgyptianAgent.apk"
    
    print_status "Egyptian Agent deployed to system partition"
}

# Optimize for Honor X6c
optimize_for_honor() {
    print_status "Optimizing for Honor X6c..."
    
    # Disable battery optimization for the app
    adb shell su -c "dumpsys deviceidle whitelist +com.egyptian.agent"
    
    # Set app to be unrestricted
    adb shell cmd appops set com.egyptian.agent RUN_IN_BACKGROUND allow
    adb shell cmd appops set com.egyptian.agent SYSTEM_ALERT_WINDOW allow
    
    print_status "Honor X6c optimizations applied"
}

# Restart services
restart_services() {
    print_status "Restarting services..."
    
    # Force stop the app if running
    adb shell am force-stop com.egyptian.agent
    
    # Reboot the device to ensure system app is properly loaded
    print_warning "Rebooting device to complete installation..."
    adb reboot
    
    print_status "Device reboot initiated. Please wait for it to restart."
}

# Main deployment process
main() {
    print_status "Starting Egyptian Agent production deployment"
    
    check_prerequisites
    build_application
    backup_existing
    deploy_system_app
    optimize_for_honor
    restart_services
    
    print_status "Egyptian Agent production deployment completed!"
    print_status "After device restarts, the app will be available as a system service."
}

# Run main process
main