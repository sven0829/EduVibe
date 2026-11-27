
@echo off
title EduVibe - Build Windows Application
color 0B

cd /d "%~dp0"

echo ========================================
echo       BUILDING EDuvIBE APPLICATION
echo ========================================
echo.

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "APP_JAR=target\EduVibeFinal-1.0-SNAPSHOT.jar"
set "INPUT_DIR=target\jpackage-input"

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo ERROR: JDK 21 jpackage not found.
    pause
    exit /b 1
)

echo Building application JAR...
call "%~dp0build-jar.bat"
if errorlevel 1 (
    echo ERROR: JAR build failed.
    pause
    exit /b 1
)

if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"

copy /y "%APP_JAR%" "%INPUT_DIR%\EduVibe.jar"

if errorlevel 1 (
    echo ERROR: Could not copy application JAR.
    pause
    exit /b 1
)

if exist dist rmdir /s /q dist

echo.
echo Creating Windows application...
echo.

"%JAVA_HOME%\bin\jpackage.exe" --type app-image --name EduVibe --input "%INPUT_DIR%" --main-jar EduVibe.jar --main-class app.app --dest dist --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo Copying media files...

if exist uploads (
    xcopy "uploads" "dist\EduVibe\uploads" /E /I /Y
) else (
    echo WARNING: uploads folder not found.
)

if errorlevel 1 (
    echo ERROR: Could not copy media files.
    pause
    exit /b 1
)

echo.
echo ========================================
echo       BUILD SUCCESSFUL!
echo ========================================
echo.
echo Application:
echo dist\EduVibe\EduVibe.exe
echo.
echo Media:
echo dist\EduVibe\uploads
echo.
echo Copy the entire dist\EduVibe folder.
echo MySQL must still be configured and accessible.
echo.
pause
