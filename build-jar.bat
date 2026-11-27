
@echo off
title EduVibe - Build Executable JAR
color 0B

cd /d "%~dp0"

echo ========================================
echo       BUILDING EDuvIBE JAR
echo ========================================
echo.

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

call "%MVN%" clean package org.apache.maven.plugins:maven-shade-plugin:3.6.0:shade

if errorlevel 1 (
    echo BUILD FAILED!
    pause
    exit /b 1
)

"%JAVA_HOME%\bin\jar.exe" tf target\EduVibeFinal-1.0-SNAPSHOT.jar | findstr /c:"com/mysql/cj/jdbc/Driver.class"

if errorlevel 1 (
    echo ERROR: MySQL JDBC driver missing.
    pause
    exit /b 1
)

echo.
echo EXECUTABLE JAR READY!
echo target\EduVibeFinal-1.0-SNAPSHOT.jar
pause
