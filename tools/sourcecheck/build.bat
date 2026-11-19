@echo off
rem Compile the sourcecheck tool with JDK 6. (ASCII only: stored as MS949)
setlocal enabledelayedexpansion
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 1
if exist "%DIR%build\classes" rmdir /s /q "%DIR%build\classes"
mkdir "%DIR%build\classes"
cd /d "%DIR%"
if exist build\sources.txt del build\sources.txt
for %%f in (src\sourcecheck\*.java) do echo src/sourcecheck/%%~nxf>>build\sources.txt
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -d build\classes @build\sources.txt
exit /b %ERRORLEVEL%
