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

echo [INFO] Compiling with RAPC...
"%BLACKBERRY_JDE_HOME%\bin\rapc.exe" import="%BLACKBERRY_JDE_HOME%\lib\net_rim_api.jar" codename=BBSmartBridge BBSmartBridge.jdp

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Build Successful!
    echo Generated BBSmartBridge.cod and BBSmartBridge.jad
    exit /b 0
) else (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)
