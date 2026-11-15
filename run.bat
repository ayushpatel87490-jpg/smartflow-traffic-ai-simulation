@echo off
title SmartFlow 2.0 - Traffic AI Simulator
echo ========================================================
echo Launching SmartFlow 2.0 - Autonomous Traffic AI Simulator
echo ========================================================
javac TrafficSimulator.java
if %errorlevel% neq 0 (
    echo Compilation failed! Please ensure JDK is installed and configured in PATH.
    pause
    exit /b %errorlevel%
)
java TrafficSimulator
pause