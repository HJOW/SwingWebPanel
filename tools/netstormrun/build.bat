@echo off
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 2
set "PRODUCT=%DIR%..\..\browser\target\swingwebpanel.jar"
if not exist "%PRODUCT%" (
  echo Build the product JAR first. 1>&2
  exit /b 2
)
if not exist "%DIR%build\classes" mkdir "%DIR%build\classes"
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -cp "%PRODUCT%" -d "%DIR%build\classes" "%DIR%src\netstormrun\NetstormRun.java"
exit /b %ERRORLEVEL%
