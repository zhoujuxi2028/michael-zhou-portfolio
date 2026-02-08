@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
echo ===============================================
echo    AEP Data Exporter - Windows Verification
echo ===============================================
echo.

set PASS_COUNT=0
set TOTAL_CHECKS=8

echo [STEP 1/8] Checking Java environment...
java -version >nul 2>&1
if !errorlevel! equ 0 (
    echo [PASS] Java is installed
    java -version 2>&1 | findstr "21\." >nul
    if !errorlevel! equ 0 (
        echo [PASS] Java 21 detected
        set /a PASS_COUNT+=1
    ) else (
        echo [WARN] Java version may not be 21
    )
) else (
    echo [FAIL] Java is not installed or not in PATH
)

echo.
echo [STEP 2/8] Checking project files...
if exist "pom.xml" (
    echo [PASS] pom.xml found
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] pom.xml not found
)

echo.
echo [STEP 3/8] Checking source code...
if exist "src\main\java\com\aep\export\AepDataExporter.java" (
    echo [PASS] Main source file found
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] Main source file not found
    echo [INFO] Expected: src\main\java\com\aep\export\AepDataExporter.java
)

echo.
echo [STEP 4/8] Checking dependencies...
if exist "lib\" (
    echo [PASS] lib directory found
    dir lib\*.jar /b 2>nul | find /c ".jar" >temp_jar_count.txt
    set /p JAR_COUNT=<temp_jar_count.txt
    del temp_jar_count.txt 2>nul
    echo [INFO] Found !JAR_COUNT! JAR files in lib/
    if defined JAR_COUNT (
        if !JAR_COUNT! gtr 5 (
            echo [PASS] Sufficient JAR dependencies
            set /a PASS_COUNT+=1
        ) else (
            echo [WARN] May be missing some dependencies
        )
    ) else (
        echo [WARN] Could not count JAR files
    )
) else (
    echo [FAIL] lib directory not found
)

echo.
echo [STEP 5/8] Checking environment configuration...
if exist ".env" (
    echo [PASS] .env file found
    findstr "AEP_APP_KEY" .env >nul
    if !errorlevel! equ 0 (
        echo [PASS] AEP_APP_KEY configured
        set /a PASS_COUNT+=1
    ) else (
        echo [FAIL] AEP_APP_KEY not found in .env
    )
) else (
    echo [FAIL] .env file not found
    if exist ".env.template" (
        echo [INFO] Template available: .env.template
    )
)

echo.
echo [STEP 6/8] Checking build scripts...
if exist "build_prod.bat" (
    echo [PASS] Production build script found
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] build_prod.bat not found
)

echo.
echo [STEP 7/8] Testing build process...
if exist ".env" (
    echo [INFO] Attempting to build...
    call build_prod.bat >build_test_output.txt 2>&1
    if !errorlevel! equ 0 (
        echo [PASS] Build completed successfully
        set /a PASS_COUNT+=1
        if exist "target\aep-data-exporter-prod.jar" (
            echo [PASS] JAR file generated (Production)
            set /a PASS_COUNT+=1
        ) else if exist "target\aep-data-exporter.jar" (
            echo [PASS] JAR file generated (Standard)
            set /a PASS_COUNT+=1
        ) else if exist "target\aep-data-exporter-1.0.0.jar" (
            echo [PASS] JAR file generated (Maven style)
            set /a PASS_COUNT+=1
        ) else (
            echo [FAIL] JAR file not generated
        )
    ) else (
        echo [FAIL] Build failed
        echo [INFO] Check build_test_output.txt for details
    )
) else (
    echo [SKIP] Cannot test build without .env file
)

echo.
echo [STEP 8/8] Testing basic functionality...
if exist "target\aep-data-exporter-prod.jar" (
    echo [INFO] Testing Production JAR file...
    java -jar target\aep-data-exporter-prod.jar --version >test_output.txt 2>&1
    if !errorlevel! equ 0 (
        echo [PASS] JAR file is executable
    ) else (
        echo [WARN] JAR file may have issues
    )
    del test_output.txt 2>nul
) else if exist "target\aep-data-exporter.jar" (
    echo [INFO] Testing Standard JAR file...
    java -jar target\aep-data-exporter.jar --version >test_output.txt 2>&1
    if !errorlevel! equ 0 (
        echo [PASS] JAR file is executable
    ) else (
        echo [WARN] JAR file may have issues
    )
    del test_output.txt 2>nul
) else if exist "target\aep-data-exporter-1.0.0.jar" (
    echo [INFO] Testing Maven-style JAR file...
    java -jar target\aep-data-exporter-1.0.0.jar --version >test_output.txt 2>&1
    if !errorlevel! equ 0 (
        echo [PASS] JAR file is executable
    ) else (
        echo [WARN] JAR file may have issues
    )
    del test_output.txt 2>nul
) else (
    echo [SKIP] No JAR file to test
)

echo.
echo ===============================================
echo           VERIFICATION SUMMARY
echo ===============================================
echo Passed checks: !PASS_COUNT!/!TOTAL_CHECKS!

if !PASS_COUNT! geq 7 (
    echo [EXCELLENT] System ready for production use
    echo.
    echo Next steps:
    echo   1. Run: .\query_win.bat
    echo   2. Check output: dir output\
) else if !PASS_COUNT! geq 5 (
    echo [GOOD] System mostly ready, minor issues
    echo.
    echo Recommended actions:
    echo   1. Fix any failed checks above
    echo   2. Test with: .\query_win.bat
) else (
    echo [ISSUES] Several problems need to be resolved
    echo.
    echo Required actions:
    echo   1. Fix failed checks above
    echo   2. Ensure all required files are present
    echo   3. Configure .env file properly
)

echo.
echo ===============================================
echo Current directory contents:
dir /b
echo.
if exist "lib\" (
    echo lib/ directory contents:
    dir lib\*.jar /b
    echo.
)
if exist "target\" (
    echo target/ directory contents:
    dir target\*.jar /b 2>nul
    echo.
)

echo Verification completed at %date% %time%
echo ===============================================
pause