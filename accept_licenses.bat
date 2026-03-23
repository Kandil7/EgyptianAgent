@echo off
echo ============================================
echo Android SDK License Acceptance
echo ============================================
echo.

set ANDROID_HOME=C:\Android\Sdk
set PATH=%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo Accepting all licenses (method 2)...
echo.

for /L %%i in (1,1,20) do @echo y| sdkmanager --licenses >nul 2>&1

echo.
echo License acceptance attempts complete!
echo.
echo Verifying licenses...
sdkmanager --list >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo SDK Manager is working
) else (
    echo SDK Manager may have issues
)
