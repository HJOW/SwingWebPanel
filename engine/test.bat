@echo off
rem Run only the JavaScript engine tests with Maven.
call "%~dp0..\test.bat" %* -pl engine
exit /b %ERRORLEVEL%
