
@echo off
title EduVibe - Build Executable JAR
color 0B

echo ========================================
echo       BUILDING EDuvIBE EXECUTABLE
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

echo Building EduVibe and packaging dependencies...
echo.

call "%MVN%" clean package org.apache.maven.plugins:maven-shade-plugin:3.6.0:shade

if errorlevel 1 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo Checking for MySQL JDBC driver...

"%JAVA_HOME%\bin\jar.exe" tf target\EduVibeFinal-1.0-SNAPSHOT.jar | findstr /c:"com/mysql/cj/jdbc/Driver.class"

if errorlevel 1 (
    echo ERROR: MySQL driver is missing from the JAR!
    pause
    exit /b 1
)

echo.
echo ========================================
echo       EXECUTABLE JAR READY!
echo ========================================
echo.
echo File:
echo target\EduVibeFinal-1.0-SNAPSHOT.jar
echo.
pause
