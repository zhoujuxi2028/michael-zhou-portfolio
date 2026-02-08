@echo off
chcp 65001 >nul
echo ========================================
echo  AEP Data Exporter - Production Run
echo ========================================

REM Load environment variables
if exist ".env" (
    echo [INFO] Loading environment variables...
    for /f "usebackq tokens=1,2 delims==" %%a in (".env") do set "%%a=%%b"
    echo [SUCCESS] Environment variables loaded
) else (
    echo [ERROR] .env file not found
    pause
    exit /b 1
)

REM Check production JAR file
if exist "target\aep-data-exporter-prod.jar" (
    set JAR_FILE=target\aep-data-exporter-prod.jar
    echo [INFO] Using production JAR: %JAR_FILE%
) else if exist "target\aep-data-exporter.jar" (
    set JAR_FILE=target\aep-data-exporter.jar
    echo [INFO] Using standard JAR: %JAR_FILE%
) else (
    echo [ERROR] No JAR file found
    echo [INFO] Please run build_prod.bat first
    pause
    exit /b 1
)

REM Create output directory
if not exist "output" mkdir output

REM Run the Fat JAR application
echo [INFO] Starting AEP Data Exporter...
echo [INFO] Arguments: %*
java -jar "%JAR_FILE%" %*

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] Export completed successfully!
    echo [INFO] Check output files:
    if exist "output\*.json" dir output\*.json
    if exist "output\*.csv" dir output\*.csv
) else (
    echo.
    echo [ERROR] Export failed with error code: %errorlevel%
)

echo ========================================
pause