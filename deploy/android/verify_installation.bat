@echo off
echo ============================================
echo Verifying Android SDK Installation
echo ============================================
echo.

set ANDROID_HOME=C:\Android\Sdk
set PATH=%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo SDK Location: %ANDROID_HOME%
echo.

echo Installed packages:
sdkmanager --list_installed

echo.
echo ============================================
echo Verification Complete
echo ============================================
