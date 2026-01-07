#!/bin/bash
# Honor X6c Battery Optimization Fix
# Critical for background services to work properly

set -e

LOG_FILE="/sdcard/honor_battery_fix.log"
PACKAGE_NAME="com.egyptian.agent"

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

# Apply battery optimization exemptions
apply_battery_fixes() {
    log "Applying battery optimization exemptions..."

    # Disable battery optimization for the app
    dumpsys deviceidle whitelist +$PACKAGE_NAME
    log "Added to device idle whitelist"

    # Set app to high priority
    cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME ACTIVATE_CELLULAR_DATA allow
    cmd appops set $PACKAGE_NAME WAKE_LOCK allow
    log "Set app operations permissions"

    # Honor specific battery settings
    settings put global device_idle_constants "inactive_timeout=3600000,light_idle_timeout=3600000,deep_idle_timeout=3600000"
    log "Applied Honor-specific battery constants"

    # Set background data restrictions
    settings put global bg_data_restricted_$PACKAGE_NAME 0
    log "Disabled background data restrictions"

    # Set app as high priority in Honor's battery manager
    if [ -f "/data/system/batterymanager.xml" ]; then
        sed -i "/$PACKAGE_NAME/d" /data/system/batterymanager.xml
        echo "<app package=\"$PACKAGE_NAME\" priority=\"1\" />" >> /data/system/batterymanager.xml
        log "Updated Honor battery manager configuration"
    fi
}

# Apply doze mode exemptions
apply_doze_fixes() {
    log "Applying doze mode exemptions..."

    # Disable doze mode restrictions
    settings put global low_power_sticky 0
    settings put global low_power_triggered_time 0
    settings put global low_power_mode_enabled 0

    # Set app as exempt from doze
    dumpsys deviceidle exempt $PACKAGE_NAME

    log "Doze mode exemptions applied"
}

# Apply system UI visibility settings
apply_ui_fixes() {
    log "Applying system UI visibility settings..."

    # Keep screen on when app is active
    settings put system stay_on_while_plugged_in 7

    # Disable screen timeout for system app
    settings put system screen_off_timeout 2147483647

    log "UI visibility settings applied"
}

# Main function
main() {
    echo "============================================="
    echo "  Honor X6c Battery Optimization Fix"
    echo "  For: Egyptian Agent Voice Assistant"
    echo "============================================="
    echo

    # Initialize log file
    echo "" > $LOG_FILE

    # Run fixes
    check_root
    apply_battery_fixes
    apply_doze_fixes
    apply_ui_fixes

    echo
    log "SUCCESS: All battery optimization fixes applied!"
    log "The app should now work properly in background"

    # Reboot recommendation
    log "RECOMMENDATION: Reboot device to apply all changes"
    read -p "Press Enter to reboot device..."
    reboot
}

# Execute main function
main