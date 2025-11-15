#!/bin/bash

# Download OpenTelemetry Java Agent
# This script downloads the latest OpenTelemetry Java Agent JAR file

set -e

OTEL_VERSION="2.10.0"
OTEL_JAR_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_VERSION}/opentelemetry-javaagent.jar"
OTEL_DIR="docker/observability/otel"
OTEL_JAR="${OTEL_DIR}/opentelemetry-javaagent.jar"

echo "Downloading OpenTelemetry Java Agent v${OTEL_VERSION}..."

# Create directory if it doesn't exist
mkdir -p "${OTEL_DIR}"

# Download the JAR file
if command -v curl &> /dev/null; then
    curl -L -o "${OTEL_JAR}" "${OTEL_JAR_URL}"
elif command -v wget &> /dev/null; then
    wget -O "${OTEL_JAR}" "${OTEL_JAR_URL}"
else
    echo "Error: Neither curl nor wget is available. Please install one of them."
    exit 1
fi

# Verify the download
if [ -f "${OTEL_JAR}" ]; then
    FILE_SIZE=$(stat -f%z "${OTEL_JAR}" 2>/dev/null || stat -c%s "${OTEL_JAR}" 2>/dev/null)
    echo "✓ Downloaded successfully: ${OTEL_JAR} (${FILE_SIZE} bytes)"
else
    echo "✗ Download failed"
    exit 1
fi

echo "✓ OpenTelemetry Java Agent is ready"
