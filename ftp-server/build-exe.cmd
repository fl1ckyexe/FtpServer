@echo off
setlocal enabledelayedexpansion

REM Build EXE installer for FTP Server
REM This script builds a Windows installer that will:
REM - Start the FTP server on port 2121
REM - Start the admin UI on port 9090
REM - Automatically open browser window on startup

REM Trap errors
set "EXIT_CODE=0"

REM Prevent window from closing on error
set "ERROR_OCCURRED=0"

set "SCRIPT_DIR=%~dp0"
set "SERVER_DIR=%~dp0"
cd /d "%~dp0.." 2>nul
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Cannot access parent directory
    echo ========================================
    echo Current directory: %CD%
    echo Script location: %~dp0
    echo.
    echo Please make sure you're running the script from the correct location.
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)
set "ROOT_DIR=%CD%"
cd /d "%~dp0"
set "ICON_PATH=%ROOT_DIR%\ftp-admin-ui\src\main\resources\icons\applogo.ico"

echo ========================================
echo Building FTP Server EXE Installer
echo ========================================
echo.
echo Script directory: %SCRIPT_DIR%
echo Root directory: %ROOT_DIR%
echo Server directory: %SERVER_DIR%
echo.

REM Check if Java 17+ is available
where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Java is not in PATH
    echo ========================================
    echo Please install Java 17 or later and add it to PATH.
    echo.
    pause
    exit /b 1
)

REM Check Java version (need 17+)
for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%i
echo Java version: !JAVA_VERSION!
echo.

REM Check if jpackage is available
where jpackage >nul 2>&1
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: jpackage is not available
    echo ========================================
    echo Please install JDK 17 or later (not just JRE).
    echo jpackage is included in JDK 17+.
    echo.
    echo Current Java version:
    java -version 2>&1
    echo.
    pause
    exit /b 1
)

REM Step 1: Build the project
echo [1/3] Building project...
echo.

REM Check if mvnw.cmd exists
if not exist "%ROOT_DIR%\mvnw.cmd" (
    echo.
    echo ========================================
    echo ERROR: mvnw.cmd not found
    echo ========================================
    echo Expected location: %ROOT_DIR%\mvnw.cmd
    echo.
    echo Please make sure you're running the script from the correct location.
    echo.
    pause
    exit /b 1
)

echo Building parent project first...
cd /d "%ROOT_DIR%"
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Cannot change to root directory
    echo ========================================
    echo Root directory: %ROOT_DIR%
    echo.
    pause
    exit /b 1
)

call "%ROOT_DIR%\mvnw.cmd" clean install -DskipTests -pl ftp-common,ftp-server -am
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Parent project build failed!
    echo ========================================
    echo.
    pause
    exit /b 1
)
echo.

echo Building ftp-server module...
cd /d "%SERVER_DIR%"
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Cannot change to server directory
    echo ========================================
    echo Server directory: %SERVER_DIR%
    echo.
    pause
    exit /b 1
)

call "%ROOT_DIR%\mvnw.cmd" clean package -DskipTests
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Build failed!
    echo ========================================
    echo.
    pause
    exit /b 1
)
echo.

REM Step 2: Check if JAR was created
set "JAR_PATH=%SERVER_DIR%\target\ftp-server-1.0-SNAPSHOT.jar"
if not exist "%JAR_PATH%" (
    echo ERROR: JAR file not found at %JAR_PATH%
    pause
    exit /b 1
)
echo [2/3] JAR file created: %JAR_PATH%
echo.

REM Step 3: Create EXE installer with jpackage
echo [3/3] Creating EXE installer with jpackage...
echo This may take a few minutes...
echo.

REM Create output directory
set "OUTPUT_DIR=%SERVER_DIR%\dist-jpackage"
if exist "%OUTPUT_DIR%" (
    echo Cleaning previous build...
    rmdir /s /q "%OUTPUT_DIR%"
)

REM Initialize build type
set "BUILD_TYPE=exe"

REM Check if icon exists
set "ICON_ARG="
if exist "%ICON_PATH%" (
    set "ICON_ARG=--icon "%ICON_PATH%""
    echo Using icon: %ICON_PATH%
) else (
    echo WARNING: Icon not found at %ICON_PATH%, building without icon
)

REM Check for WiX Toolset (required for EXE installers)
where candle.exe >nul 2>&1
if errorlevel 1 (
    echo.
    echo WARNING: WiX Toolset not found in PATH.
    echo WiX Toolset is required for creating EXE installers.
    echo.
    echo Options:
    echo 1. Install WiX Toolset from https://wixtoolset.org/
    echo    After installation, add WiX bin directory to PATH
    echo 2. Build app-image instead (portable, no installer)
    echo.
    set /p BUILD_CHOICE="Build app-image instead? (Y/N): "
    if /i "!BUILD_CHOICE!"=="Y" (
        goto :build_app_image
    ) else (
        echo.
        echo ERROR: Cannot build EXE without WiX Toolset.
        echo Please install WiX Toolset or choose app-image option.
        pause
        exit /b 1
    )
)

REM Build EXE installer with jpackage
echo Building EXE installer...
jpackage ^
    --input "%SERVER_DIR%\target" ^
    --main-jar ftp-server-1.0-SNAPSHOT.jar ^
    --main-class org.example.ftp.server.FtpServerMain ^
    --name "FTP Server" ^
    --app-version "1.0" ^
    --description "FTP Server with Admin Web UI" ^
    --vendor "FTP Server" ^
    --type exe ^
    --dest "%OUTPUT_DIR%" ^
    --win-console ^
    --win-shortcut ^
    --win-menu ^
    --win-menu-group "FTP Server" ^
    !ICON_ARG! ^
    --java-options "-Dftp.port=2121" ^
    --java-options "-Dadmin.port=9090"

if errorlevel 1 (
    echo.
    echo ERROR: jpackage failed!
    echo.
    echo Common issues:
    echo - Make sure you have JDK 17+ installed (not just JRE)
    echo - Make sure WiX Toolset is installed and in PATH
    echo   Download from: https://wixtoolset.org/
    echo - Check the error message above for details
    echo.
    echo Trying to build app-image instead...
    goto :build_app_image
)
goto :build_success

:build_app_image
echo.
echo Building portable app-image (no installer required)...
jpackage ^
    --input "%SERVER_DIR%\target" ^
    --main-jar ftp-server-1.0-SNAPSHOT.jar ^
    --main-class org.example.ftp.server.FtpServerMain ^
    --name "FTP Server" ^
    --app-version "1.0" ^
    --description "FTP Server with Admin Web UI" ^
    --vendor "FTP Server" ^
    --type app-image ^
    --dest "%OUTPUT_DIR%" ^
    !ICON_ARG! ^
    --java-options "-Dftp.port=2121" ^
    --java-options "-Dadmin.port=9090"

if errorlevel 1 (
    echo.
    echo ERROR: jpackage failed to build app-image!
    echo.
    echo Please check:
    echo - Make sure you have JDK 17+ installed (not just JRE)
    echo - Check the error message above for details
    echo.
    pause
    exit /b 1
)
set "BUILD_TYPE=app-image"
goto :build_success

:build_success

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.

if "!BUILD_TYPE!"=="app-image" (
    echo Portable app-image location: %OUTPUT_DIR%\FTP Server\
    echo.
    echo To run the application:
    echo   "%OUTPUT_DIR%\FTP Server\FTP Server.exe"
    echo.
    echo The application will:
    echo - Start the FTP server on port 2121
    echo - Start the admin UI on port 9090
    echo - Automatically open browser window on startup
) else (
    echo Installer location: %OUTPUT_DIR%\FTP Server.exe
    echo.
    echo After installation, the application will:
    echo - Start the FTP server on port 2121
    echo - Start the admin UI on port 9090
    echo - Automatically open browser window on startup
    echo.
    echo To test the installer, run:
    echo   "%OUTPUT_DIR%\FTP Server.exe"
)
echo.
echo.
echo ========================================
echo Script finished
echo ========================================
echo.
echo Press any key to close this window...
pause >nul
exit /b %EXIT_CODE%

