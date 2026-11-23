@echo off
rem Build the example module with Maven and launch the selected example on Java 6.
setlocal
cd /d "%~dp0.." || exit /b 1
if "%~1"=="" (
  echo Usage: examples\run.bat ExampleClassName [example arguments] 1>&2
  exit /b 2
)
call mvn -B -pl examples -am -Dmaven.test.skip=true package || exit /b 1
call scripts\jdk6.bat || exit /b 1
set "PRODUCT=browser\target\swingwebpanel.jar"
if "%~1"=="EngineExample" set "PRODUCT=engine\target\swingwebpanel-js.jar"
if "%~1"=="script.ScriptEngineExample" set "PRODUCT=engine\target\swingwebpanel-js.jar"
if not "%SWP_CLASSES%"=="" set "PRODUCT=%SWP_CLASSES%"
"%JAVA%" -cp "examples\target\classes;%PRODUCT%" org.duckdns.hjow.swingwebpanel.examples.%~1 %2 %3 %4 %5 %6 %7 %8 %9
exit /b %ERRORLEVEL%
