@echo off
title Banking and Transaction Simulation System
echo =======================================================================
echo          Compiling Banking and Transaction Simulation System...
echo =======================================================================

if not exist bin mkdir bin

javac -d bin src\com\bank\exception\*.java src\com\bank\model\*.java src\com\bank\repository\*.java src\com\bank\service\*.java src\com\bank\Main.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed! Please check JDK installation.
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation successful!
echo Launching Application...
echo.
java -cp bin com.bank.Main
pause
