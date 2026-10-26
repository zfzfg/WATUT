@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
set "FAILED=0"

rem Select an installed JDK 25 so Gradle and Maven use the same compiler.
set "JAVA_HOME25="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    for /f "tokens=2" %%V in ('"%JAVA_HOME%\bin\javac.exe" -version 2^>^&1') do echo %%V| findstr /b /r "25\." >nul && set "JAVA_HOME25=%JAVA_HOME%"
)
if not defined JAVA_HOME25 for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-25*") do if not defined JAVA_HOME25 set "JAVA_HOME25=%%~fJ"
if not defined JAVA_HOME25 for /d %%J in ("C:\Program Files\Microsoft\jdk-25*") do if not defined JAVA_HOME25 set "JAVA_HOME25=%%~fJ"
if not defined JAVA_HOME25 (
    echo ERROR: JDK 25 was not found. Set JAVA_HOME to a JDK 25 installation and retry.
    exit /b 2
)
set "JAVA_HOME=%JAVA_HOME25%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JDK: %JAVA_HOME%

echo.
echo [1/5] Building CoroUtil Fabric JAR
pushd "%ROOT%..\CoroUtil"
call gradlew.bat -Ploader=fabric build --no-daemon
if errorlevel 1 set "FAILED=1"

echo.
echo [2/5] Building CoroUtil NeoForge JAR
call gradlew.bat -Ploader=neoforge build --no-daemon
if errorlevel 1 set "FAILED=1"
popd

echo.
echo [3/5] Building WATUT Fabric JAR
pushd "%ROOT%"
call gradlew.bat -Ploader=fabric build --no-daemon
if errorlevel 1 set "FAILED=1"

echo.
echo [4/5] Building WATUT NeoForge JAR
call gradlew.bat -Ploader=neoforge build --no-daemon
if errorlevel 1 set "FAILED=1"

echo.
echo [5/5] Building WATUT Paper JARs
call mvn -f "%ROOT%pom.xml" package
if errorlevel 1 set "FAILED=1"
popd

if "%FAILED%"=="0" (
    echo.
    echo All CoroUtil and WATUT JAR builds completed successfully.
    exit /b 0
)
echo.
echo One or more builds failed. See the messages above; later build steps were still attempted.
exit /b 1
