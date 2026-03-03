#!/bin/bash

# Egyptian Agent Security Audit Script
# Automated security checks for Week 1 P0 Critical Fixes
# 
# This script validates:
# - No network calls in production code
# - Cleartext traffic disabled
# - CloudFallback removed
# - Dangerous permissions removed
# - Security components present

set -e

echo "=========================================="
echo "Egyptian Agent Security Audit"
echo "Week 1 P0 Critical Fixes Verification"
echo "=========================================="
echo ""

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_SRC="$PROJECT_ROOT/app/src/main"
FAILED=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass() {
    echo -e "${GREEN}✓${NC} $1"
}

fail() {
    echo -e "${RED}✗${NC} $1"
    FAILED=1
}

warn() {
    echo -e "${YELLOW}!${NC} $1"
}

echo "Checking for network calls..."
echo "-------------------------------------------"
# Check for network libraries in production code (excluding test files)
if grep -r "okhttp3\|HttpURLConnection" "$APP_SRC/java/" --include="*.java" --include="*.kt" 2>/dev/null | grep -v "/test/" | grep -v "CommandSanitizer" | grep -v "DataEncryptionManager"; then
    fail "Network calls found in production code!"
else
    pass "No network calls found in production code"
fi

echo ""
echo "Checking for cleartext traffic..."
echo "-------------------------------------------"
if grep -q 'usesCleartextTraffic="true"' "$APP_SRC/AndroidManifest.xml"; then
    fail "Cleartext traffic is enabled!"
else
    pass "Cleartext traffic is disabled"
fi

echo ""
echo "Checking for network security config..."
echo "-------------------------------------------"
if [ -f "$APP_SRC/res/xml/network_security_config.xml" ]; then
    if grep -q 'cleartextTrafficPermitted="false"' "$APP_SRC/res/xml/network_security_config.xml"; then
        pass "Network security config properly configured"
    else
        fail "Network security config allows cleartext traffic"
    fi
else
    fail "Network security config file missing!"
fi

echo ""
echo "Checking for CloudFallback..."
echo "-------------------------------------------"
if [ -f "$APP_SRC/java/com/egyptian/agent/hybrid/CloudFallback.java" ]; then
    fail "CloudFallback still exists!"
else
    pass "CloudFallback has been removed"
fi

echo ""
echo "Checking for dangerous permissions..."
echo "-------------------------------------------"
DANGEROUS_PERMS=("QUERY_ALL_PACKAGES" "PACKAGE_USAGE_STATS" "DEVICE_POWER")
for perm in "${DANGEROUS_PERMS[@]}"; do
    if grep -q "$perm" "$APP_SRC/AndroidManifest.xml"; then
        fail "Dangerous permission found: $perm"
    else
        pass "Dangerous permission removed: $perm"
    fi
done

# Check for INTERNET and ACCESS_NETWORK_STATE (should be removed for 100% local claim)
if grep -q 'android.permission.INTERNET' "$APP_SRC/AndroidManifest.xml"; then
    warn "INTERNET permission still present (consider removing for 100% local operation)"
else
    pass "INTERNET permission removed"
fi

if grep -q 'android.permission.ACCESS_NETWORK_STATE' "$APP_SRC/AndroidManifest.xml"; then
    warn "ACCESS_NETWORK_STATE permission still present"
else
    pass "ACCESS_NETWORK_STATE permission removed"
fi

echo ""
echo "Checking for custom permissions..."
echo "-------------------------------------------"
CUSTOM_PERMS=("PERMISSION_VOICE_SERVICE" "PERMISSION_BOOT" "PERMISSION_EMERGENCY")
for perm in "${CUSTOM_PERMS[@]}"; do
    if grep -q "$perm" "$APP_SRC/AndroidManifest.xml"; then
        pass "Custom permission defined: $perm"
    else
        fail "Custom permission missing: $perm"
    fi
done

echo ""
echo "Checking for security components..."
echo "-------------------------------------------"
SECURITY_FILES=(
    "java/com/egyptian/agent/security/CommandSanitizer.java"
    "java/com/egyptian/agent/security/DataEncryptionManager.java"
    "java/com/egyptian/agent/emergency/EmergencyConfirmationDialog.kt"
    "xml/voice_interaction_service.xml"
)

for file in "${SECURITY_FILES[@]}"; do
    if [ -f "$APP_SRC/$file" ]; then
        pass "Security component present: $file"
    else
        fail "Security component missing: $file"
    fi
done

echo ""
echo "Checking for emergency rate limiting..."
echo "-------------------------------------------"
if grep -q "EMERGENCY_COOLDOWN_MS" "$APP_SRC/java/com/egyptian/agent/executors/EmergencyHandler.java"; then
    pass "Emergency rate limiting implemented"
else
    fail "Emergency rate limiting not found"
fi

echo ""
echo "Checking for command sanitization..."
echo "-------------------------------------------"
if grep -q "ALLOWED_COMMANDS" "$APP_SRC/java/com/egyptian/agent/security/CommandSanitizer.java"; then
    pass "Command allowlist implemented"
else
    fail "Command allowlist not found"
fi

if grep -q "CommandSanitizer" "$APP_SRC/java/com/egyptian/agent/system/SystemPrivilegeManager.java"; then
    pass "CommandSanitizer integrated in SystemPrivilegeManager"
else
    fail "CommandSanitizer not integrated"
fi

echo ""
echo "Checking for memory leak fixes..."
echo "-------------------------------------------"
if grep -q "mainHandler.removeCallbacksAndMessages" "$APP_SRC/java/com/egyptian/agent/core/VoiceService.java"; then
    pass "VoiceService Handler memory leak fixed"
else
    fail "VoiceService Handler memory leak not fixed"
fi

if grep -q "wakeLock = null" "$APP_SRC/java/com/egyptian/agent/core/VoiceService.java"; then
    pass "VoiceService WakeLock memory leak fixed"
else
    fail "VoiceService WakeLock memory leak not fixed"
fi

echo ""
echo "=========================================="
echo "Security Audit Summary"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All security checks passed!${NC}"
    echo ""
    echo "Week 1 P0 Critical Fixes Status: COMPLETE"
    exit 0
else
    echo -e "${RED}✗ Some security checks failed!${NC}"
    echo ""
    echo "Week 1 P0 Critical Fixes Status: INCOMPLETE"
    echo "Please review and fix the failed checks above."
    exit 1
fi
