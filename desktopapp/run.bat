@echo off
rem Build the browser application with Maven and start it with java -jar on Java 6. (ASCII only, stored as MS949)
rem The remaining arguments (for example a start address) are passed to the application.
setlocal
cd /d "%~dp0.." || exit /b 1
call mvn -B -pl desktopapp -am -Dmaven.test.skip=true package || exit /b 1
call scripts\jdk6.bat || exit /b 1
"%JAVA%" -jar desktopapp\target\swingwebpanel-desktopapp.jar %*
exit /b %ERRORLEVEL%
