@echo off
echo ====================================================
echo BlackBerrySmartBridge Build Script for JDE 7.1.x
echo ====================================================

if "%BLACKBERRY_JDE_HOME%"=="" (
    echo [ERROR] BLACKBERRY_JDE_HOME environment variable is not set.
    echo Please set it to your JDE installation path.
    echo Example: set BLACKBERRY_JDE_HOME=C:\Program Files\Research In Motion\BlackBerry JDE 7.1.0
    exit /b 1
)

if not exist "%BLACKBERRY_JDE_HOME%\bin\rapc.exe" (
    echo [ERROR] rapc.exe not found in %BLACKBERRY_JDE_HOME%\bin
    exit /b 1
)

echo [INFO] Cleaning old builds...
del *.cod *.jad *.cso *.debug *.jar 2>nul

echo [INFO] Compiling BBSmartBridge with Ant...
call ant build
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Build Successful!
    echo Generated valid BBSmartBridge.cod, BBSmartBridge.jad, and BBSmartBridge.alx
    exit /b 0
) else (
    echo [ERROR] Ant build failed!
    exit /b %ERRORLEVEL%
)
