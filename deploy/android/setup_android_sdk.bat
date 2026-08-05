@echo off
@rem EgyptianAgent Android SDK Setup

echo === EgyptianAgent Android SDK Setup ===
echo.

@rem Check common Android Studio locations
set ANDROID_HOME=C:\Users\amazon\AppData\Local\Android\Sdk
if not exist "%ANDROID_HOME%" (
    set ANDROID_HOME=C:\Program Files\Android\Android Studio
)
if not exist "%ANDROID_HOME%\cmdline-tools" (
    if exist "C:\Users\amazon\AppData\Local\Android\Sdk" (
        set ANDROID_HOME=C:\Users\amazon\AppData\Local\Android\Sdk
    )
)

echo Checking for Android SDK...
echo.

@rem Check if Android Studio is installed
if exist "C:\Program Files\Android\Android Studio" (
    echo Android Studio found!
    echo Please install Android SDK via Android Studio:
    echo 1. Open Android Studio
    echo 2. Tools -^> SDK Manager
    echo 3. Install Android SDK Platform 34
    echo 4. Install Android SDK Build-Tools
    echo 5. Install Android SDK Command-line Tools
    goto :manual_setup
)

echo Android Studio not found.
echo.
echo Installing command-line tools only...
echo.

@rem Download and setup command-line tools
set TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip
set DOWNLOAD_PATH=%TEMP%\android-commandline.zip
set INSTALL_PATH=C:\Users\amazon\AppData\Local\Android\cmdline-tools

echo Downloading command-line tools...
powershell -Command "Invoke-WebRequest -Uri '%TOOLS_URL%' -OutFile '%DOWNLOAD_PATH%' -UseBasicParsing"

echo Extracting...
powershell -Command "Expand-Archive -Path '%DOWNLOAD_PATH%' -DestinationPath '%TEMP%\cmdline-tools' -Force"

echo Moving to install location...
if not exist "%INSTALL_PATH%" mkdir "%INSTALL_PATH%"
move /Y "%TEMP%\cmdline-tools\cmdline-tools" "%INSTALL_PATH%\latest"

@rem Accept licenses
echo.
echo Accepting licenses...
call "%INSTALL_PATH%\latest\bin\sdkmanager.bat" --licenses

@rem Install required components
echo.
echo Installing SDK Platform 34...
call "%INSTALL_PATH%\latest\bin\sdkmanager.bat" "platforms;android-34" "build-tools;34.0.0" "platform-tools"

echo.
echo SDK installed at: %INSTALL_PATH%
echo.
echo Creating local.properties...

:manual_setup
echo sdk.dir=%ANDROID_HOME% > local.properties
echo.
echo local.properties created with: sdk.dir=%ANDROID_HOME%
echo.
echo If this is incorrect, please edit local.properties with the correct path.
echo.
pause
