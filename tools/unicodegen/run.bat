@echo off
rem Run unicodegen with JDK 6. ASCII only: this file is stored as MSWIN949.
setlocal
set "DIR=%~dp0"
if "%JAVA6_HOME%"=="" set "JAVA6_HOME=C:\Program Files\Java\jdk1.6.0_45"
if not exist "%DIR%build\classes\unicodegen\UnicodeGen.class" call "%DIR%build.bat"
if errorlevel 1 exit /b 1
"%JAVA6_HOME%\bin\java.exe" -cp "%DIR%build\classes" unicodegen.UnicodeGen %*
exit /b %ERRORLEVEL%
