@echo off

set HOME_DIR=%~dp0

set BUILD_DIR="%HOME_DIR%\..\..\usbdm-eclipse-makefiles-build"
set LIB_i386="%BUILD_DIR%\PackageFiles\bin\i386-win-gnu"
set LIB_x86_64="%BUILD_DIR%\PackageFiles\bin\x86_64-win-gnu"
set DEST_i386=%HOME_DIR%\i386
set DEST_x86_64=%HOME_DIR%\x86_64

if not exist %DEST_i386% mkdir %DEST_i386%
cd %DEST_i386%
cd
set FILES=libgcc_s_dw2-1.dll libstdc++-6.dll usbdm.4.dll usbdm-debug.4.dll usbdm-jni.4.dll usbdm-jni-debug.4.dll libusb-1.0.dll
for %%f in (%FILES%) do copy "%LIB_i386%\%%f"

if not exist %DEST_x86_64% mkdir %DEST_x86_64%
cd %DEST_x86_64%
cd
set FILES=libgcc_s_seh-1.dll libstdc++-6.dll libwinpthread-1.dll usbdm.4.dll usbdm-debug.4.dll usbdm-jni.4.dll usbdm-jni-debug.4.dll libusb-1.0.dll
for %%f in (%FILES%) do copy "%LIB_x86_64%\%%f"

pause
