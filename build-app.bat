@echo off
REM Creates a double-clickable app folder  dist\EduVibe\EduVibe.exe  with its own
REM Java runtime included (no Java needed on the other computer).
REM Needs JDK 21 (jpackage) and a built target\EduVibe.jar (run build-jar.bat first).
cd /d "%~dp0"
if not exist target\EduVibe.jar (
    echo target\EduVibe.jar not found. Run build-jar.bat first.
    pause
    exit /b 1
)
if exist dist rmdir /s /q dist
jpackage --type app-image --name EduVibe --input target --main-jar EduVibe.jar --main-class app.app --dest dist --java-options "-Dfile.encoding=UTF-8"
if errorlevel 1 (
    echo jpackage failed. Make sure JAVA_HOME points to JDK 21.
    pause
    exit /b 1
)
echo Done: dist\EduVibe\EduVibe.exe
echo Copy the whole dist\EduVibe folder to move the app. MySQL must still be reachable.
pause