@echo off
rem Compile the cabundle tool with JDK 6. (ASCII only: this file is stored as MS949.)
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 1
if exist "%DIR%build\classes" rmdir /s /q "%DIR%build\classes"
mkdir "%DIR%build\classes"
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -d "%DIR%build\classes" "%DIR%src\cabundle\CaBundle.java"
exit /b %ERRORLEVEL%
