@echo off
rem Run the webfetch tool. (ASCII only: this file is stored as MS949.)
rem Run JRE: WEBFETCH_JAVA_HOME, else JAVA6_HOME, else the default JDK 6 path.
rem JRE 6u45 cannot negotiate TLS 1.2 / SNI, so point WEBFETCH_JAVA_HOME to a newer JRE for HTTPS.
setlocal
set "DIR=%~dp0"
set "RUNHOME=%WEBFETCH_JAVA_HOME%"
if "%RUNHOME%"=="" set "RUNHOME=%JAVA6_HOME%"
if "%RUNHOME%"=="" set "RUNHOME=C:\Program Files\Java\jdk1.6.0_45"
if not exist "%RUNHOME%\bin\java.exe" (
  echo [ERROR] JRE not found: %RUNHOME% 1>&2
  exit /b 1
)
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\webfetch\build.bat 1>&2
  exit /b 1
)
"%RUNHOME%\bin\java.exe" -cp "%DIR%build\classes" webfetch.WebFetch %*
exit /b %ERRORLEVEL%
