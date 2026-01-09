#!/bin/bash
# Egyptian Agent - Production Installation Script
# Installs the Egyptian Agent as a system app on Honor X6c devices

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Egyptian Agent Production Installation Script ===${NC}"
echo -e "${BLUE}For Honor X6c (MediaTek Helio G81 Ultra)${NC}"
echo

# Configuration
APK_NAME="EgyptianAgent-release.apk"
SYSTEM_DIR="/system/priv-app/EgyptianAgent"
LOG_FILE="/sdcard/egyptian_agent_install.log"
PACKAGE_NAME="com.egyptian.agent"

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

# Function to log messages
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
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
    IS_ROOTED=$(adb shell id | grep -c "uid=0")
    if [ "$IS_ROOTED" -ne 1 ]; then
        print_error "Device is not rooted! Egyptian Agent requires root access to function as a system app."
        exit 1
    fi
    
    log "Prerequisites check passed"
}

# Find APK file
find_apk() {
    log "Looking for APK file..."
    
    # Check if APK exists in common build locations
    if [ -f "app/build/outputs/apk/release/$APK_NAME" ]; then
        APK_PATH="app/build/outputs/apk/release/$APK_NAME"
    elif [ -f "$APK_NAME" ]; then
        APK_PATH="$APK_NAME"
    else
        print_error "APK file not found! Please build the project first using './gradlew assembleRelease'"
        exit 1
    fi
    
    log "Found APK at: $APK_PATH"
}

# Prepare device for installation
prepare_device() {
    log "Preparing device for installation..."
    
    # Reboot to system if in recovery/fastboot
    if [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; then
        print_status "Rebooting device to system..."
        adb reboot
        sleep 30
        # Wait for device to be available
        until adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; do
            sleep 5
        done
    fi
    
    # Check if device is responsive
    if ! adb shell echo "test" >/dev/null 2>&1; then
        print_error "Device is not responsive!"
        exit 1
    fi
    
    log "Device prepared successfully"
}

# Install as system app
install_system_app() {
    log "Installing Egyptian Agent as system app..."
    
    # Push APK to device
    print_status "Pushing APK to device..."
    adb push "$APK_PATH" /sdcard/
    
    # Create system directory and install APK
    print_status "Installing as system app..."
    adb shell su -c "
        # Create system directory
        mkdir -p $SYSTEM_DIR
        
        # Copy APK to system directory
        cp /sdcard/$(basename $APK_PATH) $SYSTEM_DIR/
        
        # Set proper permissions
        chmod 644 $SYSTEM_DIR/$(basename $APK_PATH)
        
        # Set SELinux context (for Android 7+)
        chcon u:object_r:system_file:s0 $SYSTEM_DIR/$(basename $APK_PATH) 2>/dev/null || true
        
        # Sync to disk
        sync
    "
    
    log "System app installation completed"
}

# Grant critical permissions
grant_permissions() {
    log "Granting critical permissions..."
    
    # Grant system-level permissions
    adb shell su -c "
        # Grant system alert window permission
        pm grant $PACKAGE_NAME android.permission.SYSTEM_ALERT_WINDOW
        
        # Grant accessibility service permission (if needed)
        pm grant $PACKAGE_NAME android.permission.BIND_ACCESSIBILITY_SERVICE 2>/dev/null || true
        
        # Grant device admin permissions
        dpm set-device-owner $PACKAGE_NAME/.receivers.AdminReceiver 2>/dev/null || true
    "
    
    # Additional permissions that might need to be set manually
    adb shell pm grant $PACKAGE_NAME android.permission.CALL_PHONE
    adb shell pm grant $PACKAGE_NAME android.permission.READ_CONTACTS
    adb shell pm grant $PACKAGE_NAME android.permission.RECORD_AUDIO
    adb shell pm grant $PACKAGE_NAME android.permission.BODY_SENSORS
    adb shell pm grant $PACKAGE_NAME android.permission.ACTIVITY_RECOGNITION
    adb shell pm grant $PACKAGE_NAME android.permission.SEND_SMS
    adb shell pm grant $PACKAGE_NAME android.permission.ACCESS_FINE_LOCATION
    
    log "Permissions granted"
}

# Apply Honor-specific optimizations
apply_honor_optimizations() {
    log "Applying Honor-specific optimizations..."
    
    # Push and execute battery optimization fixes
    if [ -f "scripts/honor_battery_fix.sh" ]; then
        print_status "Applying Honor battery optimizations..."
        adb push scripts/honor_battery_fix.sh /sdcard/
        adb shell su -c "sh /sdcard/honor_battery_fix.sh"
    else
        print_warning "honor_battery_fix.sh not found, skipping optimizations"
    fi
    
    # Disable aggressive battery optimization for the app
    adb shell su -c "
        dumpsys deviceidle whitelist +$PACKAGE_NAME
        cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow
        cmd appops set $PACKAGE_NAME ACTIVATE_CELLULAR_DATA allow
        cmd appops set $PACKAGE_NAME WAKE_LOCK allow
    "
    
    log "Honor optimizations applied"
}

# Configure auto-start
configure_auto_start() {
    log "Configuring auto-start..."
    
    # Enable the voice service
    adb shell am startservice -n $PACKAGE_NAME/.core.VoiceService
    
    # Enable boot receiver
    adb shell pm enable $PACKAGE_NAME/.receivers.BootReceiver
    
    log "Auto-start configured"
}

# Verify installation
verify_installation() {
    log "Verifying installation..."
    
    # Check if app is installed
    if adb shell pm list packages | grep -q $PACKAGE_NAME; then
        print_status "Egyptian Agent is installed"
    else
        print_error "Installation verification failed - app not found"
        exit 1
    fi
    
    # Check if it's in the system app directory
    if adb shell ls $SYSTEM_DIR/$(basename $APK_PATH) >/dev/null 2>&1; then
        print_status "Egyptian Agent is installed as system app"
    else
        print_warning "Egyptian Agent may not be installed as system app"
    fi
    
    # Check if service is running
    if adb shell pidof $PACKAGE_NAME >/dev/null 2>&1; then
        print_status "Egyptian Agent service is running"
    else
        print_warning "Egyptian Agent service is not running, attempting to start..."
        adb shell am startservice -n $PACKAGE_NAME/.core.VoiceService
    fi
    
    log "Installation verification completed"
}

# Create uninstall script
create_uninstall_script() {
    log "Creating uninstall script..."
    
    UNINSTALL_SCRIPT="/sdcard/uninstall_egyptian_agent.sh"
    cat > /tmp/uninstall_script.sh << EOF
#!/system/bin/sh
# Uninstall script for Egyptian Agent

PACKAGE_NAME="$PACKAGE_NAME"
SYSTEM_DIR="$SYSTEM_DIR"

echo "Uninstalling Egyptian Agent..."

# Stop the service
am kill \$PACKAGE_NAME

# Remove system app directory
rm -rf \$SYSTEM_DIR

# Uninstall package
pm uninstall \$PACKAGE_NAME

# Remove from device owner (if set)
dpm remove-active-admin \$PACKAGE_NAME/.receivers.AdminReceiver 2>/dev/null || true

# Remove from whitelist
dumpsys deviceidle unwhitelist +\$PACKAGE_NAME 2>/dev/null || true

echo "Egyptian Agent uninstalled successfully"
EOF

    adb push /tmp/uninstall_script.sh $UNINSTALL_SCRIPT
    adb shell su -c "chmod 755 $UNINSTALL_SCRIPT"
    
    log "Uninstall script created at $UNINSTALL_SCRIPT"
}

# Main installation process
main() {
    echo "============================================="
    echo "  Egyptian Agent Production Installation"
    echo "  Device: $(adb shell getprop ro.product.model 2>/dev/null)"
    echo "  Android: $(adb shell getprop ro.build.version.release 2>/dev/null)"
    echo "============================================="
    echo
    
    # Initialize log file
    echo "" > $LOG_FILE
    
    # Run installation steps
    check_prerequisites
    find_apk
    prepare_device
    install_system_app
    grant_permissions
    apply_honor_optimizations
    configure_auto_start
    verify_installation
    create_uninstall_script
    
    echo
    log "SUCCESS: Egyptian Agent installed as system app!"
    print_status "The application is now installed and running as a system app"
    print_status "Rebooting device to finalize installation..."
    
    # Reboot to ensure all changes take effect
    adb reboot
    
    echo
    print_status "Installation completed successfully!"
    print_status "After reboot, the Egyptian Agent will be fully operational"
    print_status "To uninstall, run: adb shell su -c '$UNINSTALL_SCRIPT'"
}

# Execute main function
main