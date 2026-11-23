@echo off
rem Compile the uicheck tool with JDK 6. Build the product first (build.bat). (ASCII only: stored as MS949)
setlocal
set "DIR=%~dp0"
call "%DIR%..\..\scripts\jdk6.bat" || exit /b 1
if "%SWP_CLASSES%"=="" set "SWP_CLASSES=%DIR%..\..\browser\target\swingwebpanel.jar"
if not exist "%SWP_CLASSES%" (
  echo [ERROR] Build the product first: build.bat 1>&2
  exit /b 2
)
if exist "%DIR%build\classes" rmdir /s /q "%DIR%build\classes"
mkdir "%DIR%build\classes"
cd /d "%DIR%"
if exist build\sources.txt del build\sources.txt
for %%f in (src\uicheck\*.java) do echo src/uicheck/%%~nxf>>build\sources.txt
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -cp "%SWP_CLASSES%" -d build\classes @build\sources.txt
exit /b %ERRORLEVEL%
