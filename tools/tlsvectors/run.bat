@echo off
rem Run the tlsvectors tool. (ASCII only: this file is stored as MS949.)
rem Run JRE: TLSVECTORS_JAVA_HOME (Java 11 or newer). JRE 6 lacks the algorithms and is never used instead.
setlocal
set "DIR=%~dp0"
if "%TLSVECTORS_JAVA_HOME%"=="" (
  echo [ERROR] Set TLSVECTORS_JAVA_HOME to a JRE of Java 11 or newer. 1>&2
  exit /b 1
)
if not exist "%TLSVECTORS_JAVA_HOME%\bin\java.exe" (
  echo [ERROR] JRE not found: %TLSVECTORS_JAVA_HOME% 1>&2
  exit /b 1
)
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\tlsvectors\build.bat 1>&2
  exit /b 1
)
"%TLSVECTORS_JAVA_HOME%\bin\java.exe" -cp "%DIR%build\classes" tlsvectors.TlsVectors %*
exit /b %ERRORLEVEL%
