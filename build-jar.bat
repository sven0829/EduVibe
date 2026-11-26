
@echo off
title EduVibe - Build JAR
color 0B

echo ========================================
echo          BUILDING EDuvIBE
echo ========================================
echo.

cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK 21 not found.
    pause
    exit /b 1
)

if not exist "%MVN%" (
    echo ERROR: Maven not found.
    pause
    exit /b 1
)

echo Building EduVibe...
echo.

call "%MVN%" clean package

if errorlevel 1 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo BUILD SUCCESSFUL!
echo Check the target folder for EduVibe.jar.
pause
