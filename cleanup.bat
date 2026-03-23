@echo off
echo Cleaning up temporary installation files...
del /Q "K:\temp\android-cmdline.zip"
rmdir /S /Q "K:\temp\android-cmdline-extract"
rmdir /S /Q "K:\temp\android-cmdline"
echo Cleanup complete!
