@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
set "NO_PAUSE=0"
if /i "%~1"=="--no-pause" set "NO_PAUSE=1"
set "JAVA_HOME25="

rem Use one validated JDK for every Gradle and Maven invocation.
if defined JAVA_HOME call :tryJdk "%JAVA_HOME%"
if defined JAVA_HOME_25_X64 call :tryJdk "%JAVA_HOME_25_X64%"
for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-25*" "%ProgramFiles%\Microsoft\jdk-25*" "%ProgramFiles%\Java\jdk-25*" "%USERPROFILE%\.jdks\*25*") do call :tryJdk "%%~fJ"
if not defined JAVA_HOME25 (
    echo ERROR: JDK 25 was not found. Set JAVA_HOME to a JDK 25 installation.
    goto failed
)
set "JAVA_HOME=%JAVA_HOME25%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
where mvn >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven was not found on PATH. Install Maven to build the Paper module.
    goto failed
)
if not exist "%ROOT%..\CoroUtil\gradlew.bat" (
    echo ERROR: Expected the CoroUtil repository next to the WATUT folder.
    goto failed
)
echo Using JDK: %JAVA_HOME%

for /f "tokens=2 delims==" %%V in ('findstr /b "coroutil_version=" "%ROOT%gradle.properties"') do set "COROUTIL_VERSION=%%V"
for /f "tokens=2 delims==" %%V in ('findstr /b "mod_version=" "%ROOT%gradle.properties"') do set "WATUT_VERSION=%%V"

echo.
echo [1/6] CoroUtil Fabric
call :gradle "%ROOT%..\CoroUtil" fabric
if errorlevel 1 goto failed
echo.
echo [2/6] CoroUtil NeoForge
call :gradle "%ROOT%..\CoroUtil" neoforge
if errorlevel 1 goto failed
echo.
echo [3/6] CoroUtil Paper (Maven)
if exist "%ROOT%..\CoroUtil-Paper\pom.xml" (
    call mvn -f "%ROOT%..\CoroUtil-Paper\pom.xml" clean install
    if errorlevel 1 goto failed
)
echo.
echo [4/6] WATUT Fabric
call :gradle "%ROOT%" fabric
if errorlevel 1 goto failed
echo.
echo [5/6] WATUT NeoForge
call :gradle "%ROOT%" neoforge
if errorlevel 1 goto failed
echo.
echo [6/6] WATUT Paper and Common (Maven)
call mvn -f "%ROOT%pom.xml" clean package
if errorlevel 1 goto failed

if not exist "%ROOT%jar" mkdir "%ROOT%jar"
copy /y "%ROOT%..\CoroUtil\build\libs\coroutil-fabric-%COROUTIL_VERSION%.jar" "%ROOT%jar\" >nul
if errorlevel 1 goto failed
copy /y "%ROOT%..\CoroUtil\build\libs\coroutil-neoforge-%COROUTIL_VERSION%.jar" "%ROOT%jar\" >nul
if errorlevel 1 goto failed
if exist "%ROOT%..\CoroUtil-Paper\jar\coroutil-server-paper-%COROUTIL_VERSION%.jar" (
    copy /y "%ROOT%..\CoroUtil-Paper\jar\coroutil-server-paper-%COROUTIL_VERSION%.jar" "%ROOT%jar\" >nul
    if errorlevel 1 goto failed
)
copy /y "%ROOT%watut-common\target\watut-common-%WATUT_VERSION%.jar" "%ROOT%jar\" >nul
if errorlevel 1 goto failed

echo.
echo All builds and tests succeeded. All JARs are in:
echo %ROOT%jar
if "%NO_PAUSE%"=="0" pause
exit /b 0

:failed
echo.
echo Build failed. The first failing step is shown above.
if "%NO_PAUSE%"=="0" pause
exit /b 1

:gradle
pushd "%~1"
if errorlevel 1 exit /b 1
call gradlew.bat -Ploader=%~2 clean build --no-daemon --console=plain
set "BUILD_RESULT=%ERRORLEVEL%"
popd
exit /b %BUILD_RESULT%

:tryJdk
if defined JAVA_HOME25 exit /b 0
if not exist "%~1\bin\javac.exe" exit /b 0
for /f "tokens=2" %%V in ('"%~1\bin\javac.exe" -version 2^>^&1') do for /f "tokens=1 delims=.-" %%M in ("%%V") do if "%%M"=="25" set "JAVA_HOME25=%~1"
exit /b 0
