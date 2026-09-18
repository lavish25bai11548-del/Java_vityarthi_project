@echo off
title Banking System Test Runner
echo Running Automated Verification Suite...
if not exist bin mkdir bin
javac -d bin src\com\bank\exception\*.java src\com\bank\model\*.java src\com\bank\repository\*.java src\com\bank\service\*.java src\com\bank\TestRunner.java
if %ERRORLEVEL% EQU 0 (
    java -cp bin com.bank.TestRunner
) else (
    echo Compilation failed!
)
pause
