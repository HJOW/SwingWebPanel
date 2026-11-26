@echo off
setlocal
set "DIR=%~dp0"
if defined NETSTORM_JAVA_HOME (
  set "JAVA=%NETSTORM_JAVA_HOME%\bin\java.exe"
) else (
  call "%DIR%..\..\scripts\jdk6.bat" || exit /b 2
)
cd /d "%DIR%..\.." || exit /b 2
"%JAVA%" -Djava.awt.headless=true -Dfile.encoding=UTF-8 -cp "%DIR%build\classes;browser\target\swingwebpanel.jar" netstormrun.NetstormRun %*
exit /b %ERRORLEVEL%
