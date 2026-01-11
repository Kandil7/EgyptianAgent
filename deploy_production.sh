#!/bin/bash

# Production deployment script for Egyptian Agent
# Deploys the APK as a system app on Honor X6c devices

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting Egyptian Agent production deployment...${NC}"

# Configuration
DEVICE_SERIAL=${DEVICE_SERIAL:-""}
APK_PATH=${APK_PATH:-"app/build/outputs/apk/release/app-release.apk"}
SYSTEM_APP_DIR="/system/priv-app/EgyptianAgent"
TEMP_DIR="/sdcard"
PACKAGE_NAME="com.egyptian.agent"
BACKUP_DIR="/sdcard/backup_$(date +%Y%m%d_%H%M%S)"

# Function to check prerequisites
check_prerequisites() {
    echo -e "${GREEN}Checking prerequisites...${NC}"
    
    # Check if ADB is available
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}Error: ADB is not installed or not in PATH${NC}"
        exit 1
    fi
    
    # Check if device is connected
    if [ -z "$DEVICE_SERIAL" ]; then
        DEVICE_COUNT=$(adb devices | grep -c "device$")
        if [ "$DEVICE_COUNT" -eq 0 ]; then
            echo -e "${RED}Error: No device connected${NC}"
            exit 1
        elif [ "$DEVICE_COUNT" -gt 1 ]; then
            echo -e "${RED}Error: Multiple devices connected. Please specify DEVICE_SERIAL.${NC}"
            adb devices
            exit 1
        fi
    else
        # Check if the specified device is connected
        if ! adb -s "$DEVICE_SERIAL" devices | grep -q "device$"; then
            echo -e "${RED}Error: Device $DEVICE_SERIAL is not connected${NC}"
            exit 1
        fi
    fi
    
    # Check if device is rooted
    if [ -z "$DEVICE_SERIAL" ]; then
        IS_ROOTED=$(adb shell su -c "id" 2>/dev/null | grep -c "uid=0")
    else
        IS_ROOTED=$(adb -s "$DEVICE_SERIAL" shell su -c "id" 2>/dev/null | grep -c "uid=0")
    fi
    
    if [ "$IS_ROOTED" -eq 0 ]; then
        echo -e "${RED}Error: Device is not rooted or SU access denied${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Prerequisites check passed${NC}"
}

# Function to prepare device for installation
prepare_device() {
    echo -e "${GREEN}Preparing device for installation...${NC}"
    
    # Remount system partition as writable
    if [ -z "$DEVICE_SERIAL" ]; then
        adb shell su -c "mount -o rw,remount /system"
        RESULT=$?
    else
        adb -s "$DEVICE_SERIAL" shell su -c "mount -o rw,remount /system"
        RESULT=$?
    fi
    
    if [ $RESULT -ne 0 ]; then
        echo -e "${RED}Error: Failed to remount system partition as writable${NC}"
        exit 1
    fi
    
    # Create backup of existing installation if present
    if [ -z "$DEVICE_SERIAL" ]; then
        HAS_EXISTING=$(adb shell su -c "[ -d $SYSTEM_APP_DIR ] && echo 1 || echo 0")
    else
        HAS_EXISTING=$(adb -s "$DEVICE_SERIAL" shell su -c "[ -d $SYSTEM_APP_DIR ] && echo 1 || echo 0")
    fi
    
    if [ "$HAS_EXISTING" -eq 1 ]; then
        echo -e "${YELLOW}Existing installation found, creating backup...${NC}"
        
        if [ -z "$DEVICE_SERIAL" ]; then
            adb shell su -c "mkdir -p $BACKUP_DIR"
            adb shell su -c "cp -r $SYSTEM_APP_DIR $BACKUP_DIR/"
        else
            adb -s "$DEVICE_SERIAL" shell su -c "mkdir -p $BACKUP_DIR"
            adb -s "$DEVICE_SERIAL" shell su -c "cp -r $SYSTEM_APP_DIR $BACKUP_DIR/"
        fi
        
        echo -e "${GREEN}Backup created at $BACKUP_DIR${NC}"
    fi
}

# Function to install the APK as a system app
install_system_app() {
    echo -e "${GREEN}Installing Egyptian Agent as system app...${NC}"
    
    # Push APK to device
    echo -e "${GREEN}Pushing APK to device...${NC}"
    if [ -z "$DEVICE_SERIAL" ]; then
        adb push "$APK_PATH" "$TEMP_DIR/"
    else
        adb -s "$DEVICE_SERIAL" push "$APK_PATH" "$TEMP_DIR/"
    fi
    
    # Create system app directory
    if [ -z "$DEVICE_SERIAL" ]; then
        adb shell su -c "mkdir -p $SYSTEM_APP_DIR"
    else
        adb -s "$DEVICE_SERIAL" shell su -c "mkdir -p $SYSTEM_APP_DIR"
    fi
    
    # Copy APK to system directory
    APK_FILENAME=$(basename "$APK_PATH")
    if [ -z "$DEVICE_SERIAL" ]; then
        adb shell su -c "cp $TEMP_DIR/$APK_FILENAME $SYSTEM_APP_DIR/EgyptianAgent.apk"
        adb shell su -c "chmod 644 $SYSTEM_APP_DIR/EgyptianAgent.apk"
    else
        adb -s "$DEVICE_SERIAL" shell su -c "cp $TEMP_DIR/$APK_FILENAME $SYSTEM_APP_DIR/EgyptianAgent.apk"
        adb -s "$DEVICE_SERIAL" shell su -c "chmod 644 $SYSTEM_APP_DIR/EgyptianAgent.apk"
    fi
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to copy APK to system directory${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}APK installed to system directory${NC}"
}

# Function to apply Honor-specific optimizations
apply_honor_optimizations() {
    echo -e "${GREEN}Applying Honor-specific optimizations...${NC}"
    
    # Push optimization script to device
    OPTIMIZATION_SCRIPT="honor_battery_fix.sh"
    if [ -f "scripts/$OPTIMIZATION_SCRIPT" ]; then
        if [ -z "$DEVICE_SERIAL" ]; then
            adb push "scripts/$OPTIMIZATION_SCRIPT" "$TEMP_DIR/"
            adb shell su -c "sh $TEMP_DIR/$OPTIMIZATION_SCRIPT"
        else
            adb -s "$DEVICE_SERIAL" push "scripts/$OPTIMIZATION_SCRIPT" "$TEMP_DIR/"
            adb -s "$DEVICE_SERIAL" shell su -c "sh $TEMP_DIR/$OPTIMIZATION_SCRIPT"
        fi
        echo -e "${GREEN}Honor battery optimizations applied${NC}"
    else
        echo -e "${YELLOW}Honor optimization script not found, skipping...${NC}"
    fi
}

# Function to restart services
restart_services() {
    echo -e "${GREEN}Restarting services...${NC}"
    
    # Force stop the app if running
    if [ -z "$DEVICE_SERIAL" ]; then
        adb shell am force-stop "$PACKAGE_NAME"
        # Reboot the device to ensure system app takes effect
        echo -e "${YELLOW}Rebooting device to complete installation...${NC}"
        adb reboot
    else
        adb -s "$DEVICE_SERIAL" shell am force-stop "$PACKAGE_NAME"
        # Reboot the device to ensure system app takes effect
        echo -e "${YELLOW}Rebooting device to complete installation...${NC}"
        adb -s "$DEVICE_SERIAL" reboot
    fi
}

# Function to verify installation
verify_installation() {
    echo -e "${GREEN}Waiting for device to reboot...${NC}"
    sleep 30  # Wait for device to start
    
    # Wait for device to be available
    echo -e "${GREEN}Waiting for device to be available...${NC}"
    until [ -z "$DEVICE_SERIAL" ] && adb devices | grep -q "device$" || [ -n "$DEVICE_SERIAL" ] && adb -s "$DEVICE_SERIAL" devices | grep -q "device$"; do
        sleep 5
        echo -e "${YELLOW}Still waiting...${NC}"
    done
    
    sleep 10  # Additional wait for services to start
    
    # Verify app is installed
    if [ -z "$DEVICE_SERIAL" ]; then
        IS_INSTALLED=$(adb shell pm list packages | grep -c "$PACKAGE_NAME")
    else
        IS_INSTALLED=$(adb -s "$DEVICE_SERIAL" shell pm list packages | grep -c "$PACKAGE_NAME")
    fi
    
    if [ "$IS_INSTALLED" -eq 1 ]; then
        echo -e "${GREEN}Egyptian Agent successfully installed as system app${NC}"
        
        # Check if it's running as system app
        if [ -z "$DEVICE_SERIAL" ]; then
            APP_INFO=$(adb shell dumpsys package "$PACKAGE_NAME" | grep -i "userId")
        else
            APP_INFO=$(adb -s "$DEVICE_SERIAL" shell dumpsys package "$PACKAGE_NAME" | grep -i "userId")
        fi
        
        if echo "$APP_INFO" | grep -q "1000\|0"; then
            echo -e "${GREEN}App is running with system privileges${NC}"
        else
            echo -e "${YELLOW}App may not be running with system privileges${NC}"
        fi
    else
        echo -e "${RED}Error: Egyptian Agent not found after installation${NC}"
        exit 1
    fi
}

# Main deployment process
main() {
    echo -e "${GREEN}Starting deployment process for Egyptian Agent${NC}"
    echo "  - APK Path: $APK_PATH"
    echo "  - Device Serial: ${DEVICE_SERIAL:-Not specified (using first available)}"
    echo "  - System App Directory: $SYSTEM_APP_DIR"
    
    check_prerequisites
    prepare_device
    install_system_app
    apply_honor_optimizations
    restart_services
    verify_installation
    
    echo -e "${GREEN}Deployment completed successfully!${NC}"
    echo -e "${GREEN}Egyptian Agent is now installed as a system app on your Honor X6c device.${NC}"
    echo -e "${GREEN}The device has been rebooted to complete the installation.${NC}"
}

# Run main function
main "$@"