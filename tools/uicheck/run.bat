@echo off
rem Run the uicheck tool from the repository root. Arguments are passed through. (ASCII only: stored as MS949)
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 2
if "%SWP_CLASSES%"=="" set "SWP_CLASSES=%DIR%..\..\browser\target\swingwebpanel.jar"
if not exist "%DIR%build\classes" (
  echo [ERROR] Build first: tools\uicheck\build.bat 1>&2
  exit /b 2
)
cd /d "%DIR%..\.."
rem The Maven output holds compiled examples for the --example step.
"%JAVA%" %UICHECK_JVM_OPTS% -cp "tools\uicheck\build\classes;%SWP_CLASSES%;examples\target\classes;examples\src\main\resources" uicheck.UiCheck %*
exit /b %ERRORLEVEL%
