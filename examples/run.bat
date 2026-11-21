@echo off
rem Compile and run a Java 6 example. Stored as MS949 with CRLF.
rem Usage: examples\run.bat ExampleClassName [--smoke] [URL]   (an example in a sub package is written with a dot: script.ScriptEngineExample)
rem Only the requested example and the example classes it uses are compiled (-sourcepath), so the engine examples compile and run with the engine
rem classes alone:  set SWP_CLASSES=build\engine-classes  and then  examples\run.bat EngineExample --smoke
setlocal enabledelayedexpansion
cd /d "%~dp0.."
call scripts\jdk6.bat || exit /b 1
if "%~1"=="" (
  echo Usage: examples\run.bat ExampleClassName [--smoke] [URL] 1>&2
  exit /b 2
)
if "%SWP_CLASSES%"=="" (
  call build.bat || exit /b 1
  set "SWP_CLASSES=build\classes"
)
set "EXAMPLE_DIR=%CD%\build\examples"
if exist "%EXAMPLE_DIR%" rmdir /s /q "%EXAMPLE_DIR%"
mkdir "%EXAMPLE_DIR%"
set "EXAMPLE_NAME=%~1"
set "EXAMPLE_PATH=!EXAMPLE_NAME:.=\!"
set "EXAMPLE_SOURCE=examples\src\org\duckdns\hjow\swingwebpanel\examples\!EXAMPLE_PATH!.java"
if not exist "!EXAMPLE_SOURCE!" (
  echo [ERROR] example not found: !EXAMPLE_SOURCE! 1>&2
  exit /b 2
)
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -cp "%SWP_CLASSES%" -sourcepath examples\src -d "%EXAMPLE_DIR%" "!EXAMPLE_SOURCE!" || exit /b 1
if exist examples\resources (
  robocopy examples\resources "%EXAMPLE_DIR%" /E /NFL /NDL /NJH /NJS /NP >nul
  if errorlevel 8 exit /b 1
)
"%JAVA%" -cp "%SWP_CLASSES%;%EXAMPLE_DIR%" org.duckdns.hjow.swingwebpanel.examples.%~1 %2 %3 %4 %5 %6 %7 %8 %9
exit /b %ERRORLEVEL%
