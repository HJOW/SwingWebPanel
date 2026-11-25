@echo off
rem Build netstormparse with JDK 6. ASCII only: this file is stored as MS949.
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 2
set "ENGINE=%DIR%..\..\engine\target\swingwebpanel-js.jar"
if not exist "%ENGINE%" (
  echo [ERROR] Build the engine first: build.bat -pl engine 1>&2
  exit /b 2
)
if exist "%DIR%build\classes" rmdir /s /q "%DIR%build\classes"
mkdir "%DIR%build\classes"
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -cp "%ENGINE%" -d "%DIR%build\classes" "%DIR%src\netstormparse\NetstormParse.java"
exit /b %ERRORLEVEL%

