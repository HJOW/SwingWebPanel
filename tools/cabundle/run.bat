@echo off
rem Run the cabundle tool with JDK 6. (ASCII only: this file is stored as MS949.)
rem Arguments: REPORT.csv OUTPUT.txt SOURCE_URL RETRIEVED_DATE
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 1
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\cabundle\build.bat 1>&2
  exit /b 1
)
"%JAVA%" -cp "%DIR%build\classes" cabundle.CaBundle %*
exit /b %ERRORLEVEL%
