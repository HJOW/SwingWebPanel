@echo off
rem Compile and run a Java 6 example. Stored as MS949 with CRLF.
rem Usage: examples\run.bat ExampleClassName [--smoke] [URL]
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
set "EXAMPLE_ROOT=%CD%\"
if exist build\examples-sources.txt del build\examples-sources.txt
for /r examples\src %%f in (*.java) do (
  set "EXAMPLE_SOURCE=%%f"
  set "EXAMPLE_SOURCE=!EXAMPLE_SOURCE:%EXAMPLE_ROOT%=!"
  set "EXAMPLE_SOURCE=!EXAMPLE_SOURCE:\=/!"
  echo !EXAMPLE_SOURCE!>>build\examples-sources.txt
)
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -cp "%SWP_CLASSES%" -d "%EXAMPLE_DIR%" @build\examples-sources.txt || exit /b 1
if exist examples\resources (
  robocopy examples\resources "%EXAMPLE_DIR%" /E /NFL /NDL /NJH /NJS /NP >nul
  if errorlevel 8 exit /b 1
)
"%JAVA%" -cp "%SWP_CLASSES%;%EXAMPLE_DIR%" org.duckdns.hjow.swingwebpanel.examples.%~1 %2 %3 %4 %5 %6 %7 %8 %9
exit /b %ERRORLEVEL%
