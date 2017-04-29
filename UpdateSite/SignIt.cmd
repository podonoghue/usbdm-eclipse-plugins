 @echo off
rem if "%1"=="" goto usage

set HOME_DIR=%~dp0

set PASS=
set /p PASS=Enter Certificate Password:
cls
if [%PASS%] == []  goto done

set KEYTOOL="C:\Program Files\Java\jdk1.8.0_131\bin\keytool.exe"
set SIGNTOOL="C:\Program Files\Java\jdk1.8.0_131\bin\jarsigner.exe"
set STORE="C:\Apps\Certificates\cacerts"
set ALIAS=myKey
rem set ALIAS={0483c6d5-103a-436c-bca0-dfe8ad043339}
rem set TIMESTAMP_URL=http://www.startssl.com/timestamp
set TIMESTAMP_URL=http://timestamp.globalsign.com/scripts/timestamp.dll 

for %%f in (%HOME_DIR%\plugins\*.jar %HOME_DIR%\features\*.jar) do %SIGNTOOL% %%f  "%ALIAS%" -tsa %TIMESTAMP_URL% -keystore %STORE% -storepass %PASS% -keypass %PASS%
goto done

:usage
echo.
echo "Usage SignIt <password>"
echo.

:done
pause