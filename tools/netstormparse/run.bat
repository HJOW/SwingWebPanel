@echo off
rem Run netstormparse with JDK 6. ASCII only: this file is stored as MS949.
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 2
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\netstormparse\build.bat 1>&2
  exit /b 2
)
set "ENGINE=%DIR%..\..\engine\target\swingwebpanel-js.jar"
if not exist "%ENGINE%" (
  echo [ERROR] Build the engine first: build.bat -pl engine 1>&2
  exit /b 2
)
"%JAVA%" -cp "%DIR%build\classes;%ENGINE%" netstormparse.NetstormParse %*
exit /b %ERRORLEVEL%

