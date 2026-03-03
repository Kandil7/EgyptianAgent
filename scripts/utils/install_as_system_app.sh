#!/bin/bash
# Egyptian Agent - System App Installer
# Compatible with Honor X6c (Android 12)

set -e

LOG_FILE="/sdcard/egyptian_agent_install.log"
APK_PATH="/sdcard/EgyptianAgent-release.apk"
PACKAGE_NAME="com.egyptian.agent"
SYSTEM_DIR="/system/priv-app/EgyptianAgent"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

# Check root access
check_root() {
    log "Checking root access..."
    if [ "$(id -u)" != "0" ]; then
        log "ERROR: This script requires root access. Please run with su."
        exit 1
    fi
    log "Root access confirmed"
}

# Remount system as writable
remount_system() {
    log "Remounting system partition as writable..."
    mount -o remount,rw /system 2>/dev/null || {
        # Try alternative mount points for Honor devices
        mount -o remount,rw /vendor 2>/dev/null && SYSTEM_DIR="/vendor/priv-app/EgyptianAgent" || {
            mount -o remount,rw /product 2>/dev/null && SYSTEM_DIR="/product/priv-app/EgyptianAgent" || {
                log "ERROR: Failed to remount system partition. This device may have a locked bootloader."
                exit 1
            }
        }
    }
    log "System partition remounted successfully"
}

# Install APK to system directory
install_apk() {
    log "Installing APK to system directory..."

    # Create directory if not exists
    mkdir -p $SYSTEM_DIR

    # Copy APK
    cp $APK_PATH $SYSTEM_DIR/

    # Set proper permissions
    chmod 644 $SYSTEM_DIR/$(basename $APK_PATH)

    log "APK installed successfully to $SYSTEM_DIR"
}

# Grant critical permissions
grant_permissions() {
    log "Granting critical permissions..."

    # List of critical permissions
    permissions=(
        "android.permission.CALL_PHONE"
        "android.permission.READ_CONTACTS"
        "android.permission.READ_CALL_LOG"
        "android.permission.RECORD_AUDIO"
        "android.permission.SYSTEM_ALERT_WINDOW"
        "android.permission.FOREGROUND_SERVICE"
        "android.permission.RECEIVE_BOOT_COMPLETED"
        "android.permission.BODY_SENSORS"
        "android.permission.VIBRATE"
        "android.permission.ACCESS_FINE_LOCATION"
        "android.permission.SEND_SMS"
        "android.permission.WAKE_LOCK"
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    )

    for perm in "${permissions[@]}"; do
        pm grant $PACKAGE_NAME $perm 2>/dev/null || {
            log "Warning: Failed to grant $perm - continuing anyway"
        }
    done

    # Set app as device owner for full control
    dpm set-device-owner $PACKAGE_NAME/.receivers.AdminReceiver 2>/dev/null || {
        log "Warning: Failed to set device owner. Some features may be limited."
    }

    log "Permissions granted successfully"
}

# Apply Honor-specific fixes
apply_honor_fixes() {
    log "Applying Honor-specific fixes..."

    # Create battery optimization exception
    dumpsys deviceidle whitelist +$PACKAGE_NAME

    # Set high priority for background services
    cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME ACTIVATE_CELLULAR_DATA allow
    cmd appops set $PACKAGE_NAME WAKE_LOCK allow

    # Honor's aggressive battery optimization
    settings put global device_idle_constants "inactive_timeout=3600000,light_idle_timeout=3600000,deep_idle_timeout=3600000"

    log "Honor-specific fixes applied successfully"
}

# Reboot device
reboot_device() {
    log "Installation complete! Rebooting device..."
    sync
    reboot
}

# Main installation process
main() {
    echo "============================================="
    echo "  Egyptian Agent - System App Installer"
    echo "  Device: Honor X6c (MediaTek Helio G81 Ultra)"
    echo "============================================="
    echo

    # Initialize log file
    echo "" > $LOG_FILE

    # Run installation steps
    check_root
    remount_system
    install_apk
    grant_permissions
    apply_honor_fixes

    echo
    log "SUCCESS: Egyptian Agent installed successfully!"
    log "The device will reboot automatically in 10 seconds..."

    # Delay reboot to allow user to read the message
    sleep 10
    reboot_device
}

# Execute main function
main