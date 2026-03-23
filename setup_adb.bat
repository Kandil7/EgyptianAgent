@echo off
echo === EgyptianAgent ADB Setup ===
echo.
echo Downloading ADB from Google...
curl -L -o "%~dp0tools\platform-tools.zip" "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
if errorlevel 1 (
    echo Download failed!
    exit /b 1
)
echo Download complete!
echo.
echo Extracting...
powershell -Command "Expand-Archive -Path '%~dp0tools\platform-tools.zip' -DestinationPath '%~dp0tools\platform-tools' -Force"
echo.
echo Moving to tools folder...
if exist "%~dp0tools\adb" rmdir /s /q "%~dp0tools\adb"
move "%~dp0tools\platform-tools\platform-tools" "%~dp0tools\adb"
if errorlevel 1 (
    echo Move failed, trying alternative...
    powershell -Command "Copy-Item -Path '%~dp0tools\platform-tools\platform-tools' -Destination '%~dp0tools\adb' -Recurse -Force"
)
echo.
echo Cleaning up...
del "%~dp0tools\platform-tools.zip"
rmdir /s /q "%~dp0tools\platform-tools" 2>nul
echo.
echo Testing ADB...
"%~dp0tools\adb\adb.exe" version
if errorlevel 1 (
    echo.
    echo ADB installed but not in PATH.
    echo Add this to your PATH: %~dp0tools\adb
    echo.
    echo Or use full path: "%~dp0tools\adb\adb.exe" devices
) else (
    echo.
    echo === ADB Ready! ===
)
echo.
echo To use ADB, either:
echo 1. Add to PATH: setx PATH "%%PATH%%;%~dp0tools\adb"
echo 2. Or use full path: %~dp0tools\adb\adb.exe
pause
