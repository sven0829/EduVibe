
@echo off
title EduVibe Launcher
color 0B

cd /d "%~dp0"

echo ========================================
echo          STARTING EDuvIBE
echo ========================================
echo.

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java 21 not found.
    pause
    exit /b 1
)

set "EDUVIBE_DB_URL=jdbc:mysql://localhost:3306/eduvibe?useSSL=false"
set "EDUVIBE_DB_URL=%EDUVIBE_DB_URL%&allowPublicKeyRetrieval=true"

set "JAR=target\EduVibeFinal-1.0-SNAPSHOT.jar"

if not exist "%JAR%" (
    echo ERROR: EduVibe JAR not found.
    echo Run build-jar.bat first.
    pause
    exit /b 1
)

echo Database URL configured.
echo Launching EduVibe...
echo.

"%JAVA_HOME%\bin\java.exe" -jar "%JAR%"

if errorlevel 1 (
    echo.
    echo EduVibe encountered an error.
    pause
)
