@echo off
echo Copying cmdline-tools to SDK location...
robocopy "K:\temp\android-cmdline-extract\cmdline-tools" "C:\Android\Sdk\cmdline-tools\latest" /E /NFL /NDL
echo Done!
