@echo off
rem Compile unicodegen with JDK 6. ASCII only: this file is stored as MSWIN949.
setlocal
set "DIR=%~dp0"
if "%JAVA6_HOME%"=="" set "JAVA6_HOME=C:\Program Files\Java\jdk1.6.0_45"
if not exist "%JAVA6_HOME%\bin\javac.exe" (
  echo [ERROR] JDK 6 not found: %JAVA6_HOME% 1>&2
  exit /b 1
)
if exist "%DIR%build\classes" rmdir /s /q "%DIR%build\classes"
mkdir "%DIR%build\classes"
"%JAVA6_HOME%\bin\javac.exe" -encoding UTF-8 -source 1.6 -target 1.6 -d "%DIR%build\classes" "%DIR%src\unicodegen\UnicodeGen.java"
exit /b %ERRORLEVEL%
