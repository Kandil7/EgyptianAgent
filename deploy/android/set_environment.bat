@echo off
echo ============================================
echo Setting Android SDK Environment Variables
echo ============================================
echo.

setx ANDROID_HOME "C:\Android\Sdk"
setx PATH "%PATH%;C:\Android\Sdk\platform-tools;C:\Android\Sdk\cmdline-tools\latest\bin"

echo.
echo Environment variables set:
echo   ANDROID_HOME = C:\Android\Sdk
echo   PATH updated with platform-tools and cmdline-tools
echo.
echo NOTE: Environment variable changes require a new terminal session to take effect.
echo.

echo Verifying ADB installation...
adb version 2>&1 || echo "ADB not in current PATH (restart terminal to apply changes)"

echo.
echo ============================================
echo Setup Complete!
echo ============================================
