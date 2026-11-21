@echo off
rem Compile the product with JDK 6 and create the jars in build\. (ASCII only, stored as MS949)
rem Usage: build.bat   (set JAVA6_HOME to choose the JDK 6 directory)
rem Layout (P26): the engine module (engine\src, no browser dependency) is built first, then the browser module (src) is compiled
rem against the engine classes only.
rem   build\swingwebpanel-js.jar       engine only (with the javax.script provider)
rem   build\swingwebpanel-browser.jar  browser only (needs the engine jar at run time)
rem   build\swingwebpanel.jar          both in one jar (same usage as before: put only this jar on the class path)
rem   build\classes                    merged class directory used by tests, examples and tools
setlocal enabledelayedexpansion
cd /d "%~dp0"
call "%CD%\scripts\jdk6.bat" || exit /b 1
call "%CD%\engine\build.bat" || exit /b 1

if exist build\classes rmdir /s /q build\classes
if exist build\browser-classes rmdir /s /q build\browser-classes
if exist build\swingwebpanel.jar del build\swingwebpanel.jar
if exist build\swingwebpanel-browser.jar del build\swingwebpanel-browser.jar
mkdir build\classes
mkdir build\browser-classes
if exist build\sources.txt del build\sources.txt
set "ROOTDIR=%CD%\"
rem Write the source list as relative paths with forward slashes.
for /r "src" %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:%ROOTDIR%=!"
  set "p=!p:\=/!"
  echo !p!>>build\sources.txt
)
if not exist build\sources.txt (
  echo [ERROR] no java sources under src 1>&2
  exit /b 1
)
rem The browser sources may refer to the compiled engine classes only.
"%JAVAC%" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp build\engine-classes -d build\browser-classes @build\sources.txt || exit /b 1

rem Copy non-java resources into the class path root.
robocopy src build\browser-classes /E /XF *.java /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
  echo [ERROR] resource copy failed 1>&2
  exit /b 1
)

rem Merged class directory: browser classes on top of engine classes (the packages of the two modules do not overlap).
robocopy build\engine-classes build\classes /E /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
  echo [ERROR] merge of engine classes failed 1>&2
  exit /b 1
)
robocopy build\browser-classes build\classes /E /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
  echo [ERROR] merge of browser classes failed 1>&2
  exit /b 1
)

> build\MANIFEST.MF echo Manifest-Version: 1.0
>> build\MANIFEST.MF echo Implementation-Title: SwingWebPanel
>> build\MANIFEST.MF echo Implementation-Version: 0.1.0
>> build\MANIFEST.MF echo Created-By: build.bat (JDK 6)
> build\browser-MANIFEST.MF echo Manifest-Version: 1.0
>> build\browser-MANIFEST.MF echo Implementation-Title: SwingWebPanel browser component (requires swingwebpanel-js.jar)
>> build\browser-MANIFEST.MF echo Implementation-Version: 0.1.0
>> build\browser-MANIFEST.MF echo Created-By: build.bat (JDK 6)
"%JAR%" cfm build\swingwebpanel-browser.jar build\browser-MANIFEST.MF -C build\browser-classes . || exit /b 1
"%JAR%" cfm build\swingwebpanel.jar build\MANIFEST.MF -C build\classes . || exit /b 1
echo [OK] build\swingwebpanel.jar (engine + browser), build\swingwebpanel-browser.jar, build\swingwebpanel-js.jar created
exit /b 0
