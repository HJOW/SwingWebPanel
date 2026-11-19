@echo off
rem Locate JDK 6 and set JAVA6_HOME, JAVAC, JAVA, JAR, JAVAC_VERSION. (call this file; ASCII only, stored as MS949)
rem - Uses JAVA6_HOME if set, else the default C:\Program Files\Java\jdk1.6.0_45.
rem - Never silently falls back to another JDK. Set SWP_ALLOW_NON_JDK6=1 to use another JDK explicitly
rem   (the result is NOT a JRE 6 verification).
if "%JAVA6_HOME%"=="" set "JAVA6_HOME=C:\Program Files\Java\jdk1.6.0_45"
if not exist "%JAVA6_HOME%\bin\javac.exe" (
  echo [ERROR] JDK 6 not found: %JAVA6_HOME% 1>&2
  echo Set JAVA6_HOME to a JDK 6 directory. 1>&2
  exit /b 1
)
set "JAVAC=%JAVA6_HOME%\bin\javac.exe"
set "JAVA=%JAVA6_HOME%\bin\java.exe"
set "JAR=%JAVA6_HOME%\bin\jar.exe"
set "JAVAC_VERSION="
for /f "delims=" %%v in ('"%JAVAC%" -version 2^>^&1') do if not defined JAVAC_VERSION set "JAVAC_VERSION=%%v"
echo %JAVAC_VERSION% | findstr /c:" 1.6." >nul
if errorlevel 1 (
  if "%SWP_ALLOW_NON_JDK6%"=="1" (
    echo [WARN] NOT JDK 6 ^(%JAVAC_VERSION%^). Results are NOT a JRE 6 verification. 1>&2
  ) else (
    echo [ERROR] %JAVA6_HOME% is not JDK 6 ^(%JAVAC_VERSION%^). Set SWP_ALLOW_NON_JDK6=1 to use it explicitly. 1>&2
    exit /b 1
  )
)
exit /b 0
