@echo off
rem Generate each subproject's Javadoc under docs/javadoc.
setlocal DisableDelayedExpansion
pushd "%~dp0" || exit /b 1
call :generateAll
set "JAVADOC_EXIT=%ERRORLEVEL%"
popd
exit /b %JAVADOC_EXIT%

:generateAll
call scripts\jdk6.bat || exit /b 1
set "JAVADOC=%JAVA6_HOME%\bin\javadoc.exe"
if not exist "%JAVADOC%" (
  echo [ERROR] javadoc not found under JAVA6_HOME="%JAVA6_HOME%" 1>&2
  exit /b 1
)

if not exist "build\javadoc\." mkdir "build\javadoc" || exit /b 1
set "ENGINE_SOURCES=engine/src/main/java"
set "BROWSER_SOURCES=browser/src/main/java"
call :generateDocs engine || exit /b 1
call :generateDocs tls13 || exit /b 1
call :generateDocs browser "%ENGINE_SOURCES%" || exit /b 1
call :generateDocs examples "%BROWSER_SOURCES%;%ENGINE_SOURCES%" || exit /b 1
call :generateDocs desktopapp "%BROWSER_SOURCES%;%ENGINE_SOURCES%" || exit /b 1
exit /b 0

:generateDocs
set "SOURCE_DIR=%~1/src/main/java"
set "SOURCE_PATH=%SOURCE_DIR%"
if not "%~2"=="" set "SOURCE_PATH=%SOURCE_PATH%;%~2"
set "SOURCE_LIST=build/javadoc/%~1.args"
set "OUTPUT_DIR=docs/javadoc/%~1"

rem List only the module's own sources, using relative paths in the argument file.
call :listSources "%SOURCE_DIR%" > "%SOURCE_LIST%" || exit /b 1
for %%F in ("%SOURCE_LIST%") do if %%~zF equ 0 (
  echo [ERROR] No Java sources found in %SOURCE_DIR% 1>&2
  exit /b 1
)

echo [INFO] Generating Javadoc for %~1 in %OUTPUT_DIR%
"%JAVADOC%" -quiet -source 1.6 -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 ^
  -notimestamp -d "%OUTPUT_DIR%" -sourcepath "%SOURCE_PATH%" "@%SOURCE_LIST%"
exit /b %ERRORLEVEL%

:listSources
for %%F in ("%~1\*.java") do if exist "%%F" echo "%~1/%%~nxF"
for /d %%D in ("%~1\*") do call :listSources "%~1/%%~nxD"
exit /b 0
