@echo off
rem if "%1"=="" goto usage

set HOME_DIR=%~dp0

set PASS=
set /p PASS=Enter Certificate Password:
cls
if [%PASS%] == []  goto done

set SIGNTOOL="C:\Program Files\Java\jdk1.8.0_201\bin\jarsigner.exe"
set ALIAS=myKey

rem set TIMESTAMP_URL=http://timestamp.globalsign.com/scripts/timestamp.dll 
set TIMESTAMP_URL=http://time.certum.pl/
set PROVIDER_CONFIG="%HOME_DIR%\Provider.cfg"
set OWNER="Open Source Developer, Peter O'Donoghue"
set CERT_CHAIN="%HOME_DIR%\CertificateChain.pem"

for %%f in (%HOME_DIR%\plugins\*.jar %HOME_DIR%\features\*.jar) do %SIGNTOOL% -certchain %CERT_CHAIN% -keystore NONE -tsa %TIMESTAMP_URL% -storetype PKCS11 -providerClass sun.security.pkcs11.SunPKCS11 -providerArg %PROVIDER_CONFIG% -storepass %PASS% %%f %OWNER% 

rem for %%f in (%HOME_DIR%\plugins\*.jar %HOME_DIR%\features\*.jar) do %SIGNTOOL% %%f  "%ALIAS%" -tsa %TIMESTAMP_URL% -keystore NONE -storepass %PASS% -keypass %PASS%
goto done

:usage
echo.
echo "Usage SignIt <password>"
echo.

:done
pause