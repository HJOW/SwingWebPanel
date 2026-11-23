@echo off
rem Build the Maven reactor without compiling or running tests.
setlocal
cd /d "%~dp0" || exit /b 1
call mvn -B -Dmaven.test.skip=true package %*
exit /b %ERRORLEVEL%
