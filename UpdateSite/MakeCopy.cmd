@echo off

set HOME_DIR=%~dp0

xcopy /e /i /y %HOME_DIR%\..\UpdateSite c:\Users\podonoghue\Documents\Development\VB_Shared\updateSite

pause