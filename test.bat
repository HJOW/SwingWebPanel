@echo off
rem Build the product, compile and run the tests with JDK 6. (ASCII only, stored as MS949)
rem Usage: test.bat [--filter text]   (set JAVA6_HOME to choose the JDK 6 directory)
setlocal enabledelayedexpansion
cd /d "%~dp0"
call "%CD%\scripts\jdk6.bat" || exit /b 1
call "%CD%\build.bat" || exit /b 1

if exist build\test-classes rmdir /s /q build\test-classes
mkdir build\test-classes
if exist build\test-sources.txt del build\test-sources.txt
set "ROOTDIR=%CD%\"
for /r "tests\src" %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:%ROOTDIR%=!"
  set "p=!p:\=/!"
  echo !p!>>build\test-sources.txt
)
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp build\classes -d build\test-classes @build\test-sources.txt || exit /b 1
robocopy tests\resources build\test-classes /E /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
  echo [ERROR] test resource copy failed 1>&2
  exit /b 1
)

set "CP=build\classes;build\test-classes"
"%JAVA%" -Dswp.jar=build\swingwebpanel.jar -cp "%CP%" org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan build\test-classes %*
set "RC=%ERRORLEVEL%"

rem The runner must exit with code 1 for a failing suite.
"%JAVA%" -cp "%CP%" org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.testing.selftest.failing.FailingSuite > build\failing-selftest.log 2>&1
set "FRC=%ERRORLEVEL%"
if not "%FRC%"=="1" (
  echo [ERROR] runner self-check failed: expected exit code 1 for failing suite, got %FRC% 1>&2
  exit /b 1
)
echo [OK] runner exit-code self-check passed (failing suite -^> 1)
exit /b %RC%
