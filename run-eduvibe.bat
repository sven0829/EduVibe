@echo off
REM Starts EduVibe from the packaged jar (build it first with build-jar.bat).
REM Keep this file and target\EduVibe.jar where they are; the "uploads" folder
REM and optional eduvibe.properties are read from THIS folder.
cd /d "%~dp0"
if not exist target\EduVibe.jar (
    echo target\EduVibe.jar not found. Run build-jar.bat first.
    pause
    exit /b 1
)
java -jar target\EduVibe.jar
if errorlevel 1 pause