@echo off
rem Run the entitygen tool. (ASCII only: this file is stored as MS949.)
setlocal
set "DIR=%~dp0"
if "%RUNHOME%"=="" set "RUNHOME=%JAVA6_HOME%"
if "%RUNHOME%"=="" set "RUNHOME=C:\Program Files\Java\jdk1.6.0_45"
if not exist "%RUNHOME%\bin\java.exe" (
  echo [ERROR] JRE not found: %RUNHOME% 1>&2
  exit /b 1
)
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\entitygen\build.bat 1>&2
  exit /b 1
)
"%RUNHOME%\bin\java.exe" -cp "%DIR%build\classes" entitygen.EntityGen %*
exit /b %ERRORLEVEL%
