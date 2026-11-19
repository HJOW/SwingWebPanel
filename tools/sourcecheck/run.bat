@echo off
rem Run the sourcecheck tool from the repository root. Arguments are passed through. (ASCII only: stored as MS949)
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 2
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\sourcecheck\build.bat 1>&2
  exit /b 2
)
cd /d "%DIR%..\.."
"%JAVA%" -cp "tools\sourcecheck\build\classes" sourcecheck.SourceCheck %*
exit /b %ERRORLEVEL%
