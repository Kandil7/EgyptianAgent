@echo off
echo ============================================
echo Installing Android SDK Components
echo ============================================
echo.

set ANDROID_HOME=C:\Android\Sdk
set PATH=%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo Installing:
echo   - Platform 34 (android-34)
echo   - Build-Tools 34.0.0
echo   - Platform-Tools (ADB)
echo.

sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

echo.
echo ============================================
echo Installation Complete!
echo ============================================
