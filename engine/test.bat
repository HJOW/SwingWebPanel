@echo off
rem Build the engine module, then compile and run the engine tests with JDK 6 without browser classes and without AWT
rem (java.awt.headless=true). (ASCII only, stored as MS949)
rem Usage: engine\test.bat [--filter text]   (set JAVA6_HOME to choose the JDK 6 directory; SWP_ENGINE_BUILT=1 skips the engine build)
setlocal enabledelayedexpansion
cd /d "%~dp0\.."
call "%CD%\scripts\jdk6.bat" || exit /b 1
if not "%SWP_ENGINE_BUILT%"=="1" (
  call "%CD%\engine\build.bat" || exit /b 1
)

if exist build\engine-test-classes rmdir /s /q build\engine-test-classes
mkdir build\engine-test-classes
if exist build\engine-test-sources.txt del build\engine-test-sources.txt
set "ROOTDIR=%CD%\"
for /r "engine\tests\src" %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:%ROOTDIR%=!"
  set "p=!p:\=/!"
  echo !p!>>build\engine-test-sources.txt
)
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp build\engine-classes -sourcepath build\engine-empty -d build\engine-test-classes @build\engine-test-sources.txt || exit /b 1
if exist engine\tests\resources (
  robocopy engine\tests\resources build\engine-test-classes /E /NFL /NDL /NJH /NJS /NP >nul
  if errorlevel 8 (
    echo [ERROR] engine test resource copy failed 1>&2
    exit /b 1
  )
)

set "CP=build\engine-classes;build\engine-test-classes"
rem SWP_ENGINE_FROM_JAR=1 puts only build\swingwebpanel-js.jar on the class path instead of the class directory.
if "%SWP_ENGINE_FROM_JAR%"=="1" set "CP=build\swingwebpanel-js.jar;build\engine-test-classes"
"%JAVA%" -Djava.awt.headless=true -Dswp.engine.jar=build\swingwebpanel-js.jar -cp "%CP%" org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan build\engine-test-classes %*
set "RC=%ERRORLEVEL%"

rem The runner must exit with code 1 for a failing suite.
"%JAVA%" -cp "%CP%" org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.testing.selftest.failing.FailingSuite > build\engine-failing-selftest.log 2>&1
set "FRC=%ERRORLEVEL%"
if not "%FRC%"=="1" (
  echo [ERROR] runner self-check failed: expected exit code 1 for failing suite, got %FRC% 1>&2
  exit /b 1
)
echo [OK] runner exit-code self-check passed (failing suite -^> 1)
exit /b %RC%
