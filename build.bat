@echo off
rem Compile the product with JDK 6 and create build\swingwebpanel.jar. (ASCII only, stored as MS949)
rem Usage: build.bat   (set JAVA6_HOME to choose the JDK 6 directory)
setlocal enabledelayedexpansion
cd /d "%~dp0"
call "%CD%\scripts\jdk6.bat" || exit /b 1

if exist build\classes rmdir /s /q build\classes
if exist build\swingwebpanel.jar del build\swingwebpanel.jar
mkdir build\classes
if exist build\sources.txt del build\sources.txt
set "ROOTDIR=%CD%\"
rem Write the source list as relative paths with forward slashes.
for /r "src" %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:%ROOTDIR%=!"
  set "p=!p:\=/!"
  echo !p!>>build\sources.txt
)
if not exist build\sources.txt (
  echo [ERROR] no java sources under src 1>&2
  exit /b 1
)
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -g -d build\classes @build\sources.txt || exit /b 1

rem Copy non-java resources into the class path root.
robocopy src build\classes /E /XF *.java /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
  echo [ERROR] resource copy failed 1>&2
  exit /b 1
)

> build\MANIFEST.MF echo Manifest-Version: 1.0
>> build\MANIFEST.MF echo Implementation-Title: SwingWebPanel
>> build\MANIFEST.MF echo Implementation-Version: 0.1.0
>> build\MANIFEST.MF echo Created-By: build.bat (JDK 6)
"%JAR%" cfm build\swingwebpanel.jar build\MANIFEST.MF -C build\classes . || exit /b 1
echo [OK] build\swingwebpanel.jar created
exit /b 0
