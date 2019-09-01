@echo off

set HOME_DIR=%~dp0

set JAVAH_TOOL="c:\Program Files\Java\jdk1.8.0_201\bin\javah.exe"
set CLASS="net.sourceforge.usbdm.jni.Usbdm"

cd bin
cd

echo %JAVAH_TOOL% %CLASS%
%JAVAH_TOOL% %CLASS%

del net_sourceforge_usbdm_jni_Usbdm_*.h
pause
