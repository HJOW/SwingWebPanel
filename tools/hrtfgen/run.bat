@echo off
rem Run the hrtfgen tool. (ASCII only: this file is stored as MS949.)
setlocal
set "DIR=%~dp0"
if "%JAVA6_HOME%"=="" set "JAVA6_HOME=C:\Program Files\Java\jdk1.6.0_45"
if not exist "%JAVA6_HOME%\bin\java.exe" (
  echo [ERROR] JRE 6 not found: %JAVA6_HOME% 1>&2
  echo Set JAVA6_HOME to a JDK 6 directory. 1>&2
  exit /b 1
)
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\hrtfgen\build.bat 1>&2
  exit /b 1
)
"%JAVA6_HOME%\bin\java.exe" -cp "%DIR%build\classes" hrtfgen.HrtfGen %*
exit /b %ERRORLEVEL%
