#!/bin/bash
# Download whisper models for Egyptian Arabic ASR
# Usage: ./download_whisper_model.sh [model_size]
# model_size: tiny, base, small, medium, large (default: base)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MODEL_DIR="$PROJECT_ROOT/app/src/main/assets/models"

# Default model size for Egyptian Arabic (balance of speed/accuracy)
MODEL_SIZE="${1:-base}"

echo "=============================================="
echo "Whisper Model Downloader for EgyptianAgent"
echo "=============================================="
echo "Model size: $MODEL_SIZE"
echo "Target directory: $MODEL_DIR"
echo ""

# Create model directory
mkdir -p "$MODEL_DIR"

# Model filenames and URLs
case "$MODEL_SIZE" in
    tiny)
        MODEL_FILE="ggml-tiny.bin"
        MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
        ;;
    base)
        MODEL_FILE="ggml-base.bin"
        MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
        ;;
    small)
        MODEL_FILE="ggml-small.bin"
        MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
        ;;
    medium)
        MODEL_FILE="ggml-medium.bin"
        MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin"
        ;;
    large)
        MODEL_FILE="ggml-large-v3.bin"
        MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3.bin"
        ;;
    *)
        echo "Error: Unknown model size '$MODEL_SIZE'"
        echo "Valid options: tiny, base, small, medium, large"
        exit 1
        ;;
esac

OUTPUT_PATH="$MODEL_DIR/$MODEL_FILE"

# Check if model already exists
if [ -f "$OUTPUT_PATH" ]; then
    echo "Model already exists: $OUTPUT_PATH"
    ls -lh "$OUTPUT_PATH"
    echo ""
    read -p "Download anyway? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Skipping download."
        exit 0
    fi
fi

# Download model
echo "Downloading $MODEL_SIZE model..."
echo "URL: $MODEL_URL"
echo ""

# Try wget first, fall back to curl
if command -v wget &> /dev/null; then
    wget --progress=bar:force -O "$OUTPUT_PATH" "$MODEL_URL"
elif command -v curl &> /dev/null; then
    curl -L -o "$OUTPUT_PATH" "$MODEL_URL"
else
    echo "Error: Neither wget nor curl is available."
    exit 1
fi

# Verify download
if [ -f "$OUTPUT_PATH" ]; then
    echo ""
    echo "=============================================="
    echo "Download Complete!"
    echo "=============================================="
    echo "Model: $MODEL_FILE"
    echo "Location: $OUTPUT_PATH"
    echo "Size:"
    ls -lh "$OUTPUT_PATH"
    echo ""
    
    # Calculate checksum (optional)
    if command -v sha256sum &> /dev/null; then
        echo "SHA256 checksum:"
        sha256sum "$OUTPUT_PATH"
    fi
else
    echo "Error: Download failed!"
    exit 1
fi

echo ""
echo "To use this model in EgyptianAgent:"
echo "1. Copy to device: adb push $OUTPUT_PATH /sdcard/Android/data/com.egyptian.agent/files/models/"
echo "2. Or include in APK assets folder: app/src/main/assets/models/"
