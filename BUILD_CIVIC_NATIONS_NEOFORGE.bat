@echo off
setlocal
cd /d "%~dp0"

echo ======================================
echo   Civic Nations - NeoForge Build
echo   Minecraft 1.20.1 / NeoForge 47.x
echo ======================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\prepare-and-build.ps1" > ".\build-log.txt" 2>&1
set "BUILD_EXIT=%ERRORLEVEL%"
type ".\build-log.txt"
echo.

if not "%BUILD_EXIT%"=="0" (
    echo ======================================
    echo BUILD FAILED
    echo ======================================
    echo Send build-log.txt for the next fix.
) else (
    echo ======================================
    echo BUILD SUCCEEDED
    echo ======================================
    echo Copy only this JAR into the profile mods folder:
    echo build\libs\civicnations-neoforge-1.20.1.jar
)

echo.
pause
exit /b %BUILD_EXIT%
