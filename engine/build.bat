@echo off
rem Compile only the engine module (own JavaScript engine, no browser/AWT dependency) with JDK 6 and create build\swingwebpanel-js.jar.
rem (ASCII only, stored as MS949)
rem Usage: engine\build.bat   (set JAVA6_HOME to choose the JDK 6 directory)
rem Boundary: the compile class path and source path are one empty directory, so a reference to a browser class fails to compile.
setlocal enabledelayedexpansion
cd /d "%~dp0\.."
call "%CD%\scripts\jdk6.bat" || exit /b 1

if exist build\engine-classes rmdir /s /q build\engine-classes
if exist build\engine-empty rmdir /s /q build\engine-empty
if exist build\swingwebpanel-js.jar del build\swingwebpanel-js.jar
mkdir build\engine-classes
mkdir build\engine-empty
if exist build\engine-sources.txt del build\engine-sources.txt
set "ROOTDIR=%CD%\"
rem Write the source list as relative paths with forward slashes.
for /r "engine\src" %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:%ROOTDIR%=!"
  set "p=!p:\=/!"
  echo !p!>>build\engine-sources.txt
)
if not exist build\engine-sources.txt (
  echo [ERROR] no java sources under engine\src 1>&2
  exit /b 1
)
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp build\engine-empty -sourcepath build\engine-empty -d build\engine-classes @build\engine-sources.txt || exit /b 1

rem Copy non-java resources (javax.script provider registration etc.) into the class path root.
if exist engine\resources (
  robocopy engine\resources build\engine-classes /E /NFL /NDL /NJH /NJS /NP >nul
  if errorlevel 8 (
    echo [ERROR] engine resource copy failed 1>&2
    exit /b 1
  )
)
robocopy engine\src build\engine-classes /E /XF *.java /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
  echo [ERROR] engine source resource copy failed 1>&2
  exit /b 1
)

> build\engine-MANIFEST.MF echo Manifest-Version: 1.0
>> build\engine-MANIFEST.MF echo Implementation-Title: SwingWebPanel JavaScript engine
>> build\engine-MANIFEST.MF echo Implementation-Version: 0.1.0
>> build\engine-MANIFEST.MF echo Created-By: engine\build.bat (JDK 6)
"%JAR%" cfm build\swingwebpanel-js.jar build\engine-MANIFEST.MF -C build\engine-classes . || exit /b 1
echo [OK] build\swingwebpanel-js.jar created
exit /b 0
