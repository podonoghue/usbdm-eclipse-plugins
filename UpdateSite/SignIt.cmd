@echo off
rem if "%1"=="" goto usage

set PASS=
set /p PASS=Enter Certificate Password:
cls
if [%PASS%] == []  goto done

set KEYTOOL="c:\Program Files\Java\jdk1.8.0_60\bin\keytool.exe"
set SIGNTOOL="c:\Program Files\Java\jdk1.8.0_60\bin\jarsigner.exe"
set STORE="C:\Apps\Certificates\cacerts"
set ALIAS=mykey
set TIMESTAMP_URL=http://www.startssl.com/timestamp

for %%f in (plugins\*.jar features\*.jar) do %SIGNTOOL% %%f  "%ALIAS%" -tsa %TIMESTAMP_URL% -keystore %STORE% -storepass %PASS% -keypass %PASS%
goto done

:usage
echo.
echo "Usage SignIt <password>"
echo.

:done
pause