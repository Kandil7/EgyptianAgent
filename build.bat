@echo off
@rem EgyptianAgent Build Script - Sets JAVA_HOME and ANDROID_HOME correctly then builds

set JAVA_HOME=C:\Program Files\Java\jdk-21
set ANDROID_HOME=C:\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\build-tools\34.0.0;%PATH%

echo === EgyptianAgent Build ===
echo JAVA_HOME: %JAVA_HOME%
echo ANDROID_HOME: %ANDROID_HOME%
echo Java version:
java -version
echo.
echo Building...
echo.

call gradlew.bat clean assembleDebug

if %ERRORLEVEL% equ 0 (
    echo.
    echo === BUILD SUCCESSFUL ===
    echo.
    echo APK location:
    echo   app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo === BUILD FAILED ===
)

pause
