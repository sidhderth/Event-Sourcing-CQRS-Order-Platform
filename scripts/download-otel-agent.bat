@echo off
REM Download OpenTelemetry Java Agent
REM This script downloads the latest OpenTelemetry Java Agent JAR file

setlocal

set OTEL_VERSION=2.10.0
set OTEL_JAR_URL=https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v%OTEL_VERSION%/opentelemetry-javaagent.jar
set OTEL_DIR=docker\observability\otel
set OTEL_JAR=%OTEL_DIR%\opentelemetry-javaagent.jar

echo Downloading OpenTelemetry Java Agent v%OTEL_VERSION%...

REM Create directory if it doesn't exist
if not exist "%OTEL_DIR%" mkdir "%OTEL_DIR%"

REM Download using PowerShell
powershell -Command "& {Invoke-WebRequest -Uri '%OTEL_JAR_URL%' -OutFile '%OTEL_JAR%'}"

REM Verify the download
if exist "%OTEL_JAR%" (
    echo [OK] Downloaded successfully: %OTEL_JAR%
    echo [OK] OpenTelemetry Java Agent is ready
) else (
    echo [ERROR] Download failed
    exit /b 1
)

endlocal
