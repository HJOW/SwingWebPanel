@echo off
rem Run the Java 6 regression tests through Maven. The legacy --filter option is accepted.
setlocal
cd /d "%~dp0" || exit /b 1
if /i "%~1"=="--filter" goto filter
call mvn -B test %*
exit /b %ERRORLEVEL%
:filter
if "%~2"=="" (
  echo Usage: test.bat [--filter text] [Maven options] 1>&2
  exit /b 2
)
call mvn -B "-Dtest.filter=%~2" test %3 %4 %5 %6 %7 %8 %9
exit /b %ERRORLEVEL%
