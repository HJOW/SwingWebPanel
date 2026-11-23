@echo off
rem Build only the standalone JavaScript engine with Maven.
call "%~dp0..\build.bat" -pl engine -am %*
exit /b %ERRORLEVEL%
