#!/bin/bash
# Initialize git submodules for external dependencies

echo "Initializing git submodules for external dependencies..."

# Initialize and update submodules
git submodule update --init --recursive

echo "Checking external dependencies..."

# Check if llama.cpp is properly initialized
if [ -d "external/llama.cpp" ] && [ -f "external/llama.cpp/README.md" ]; then
    echo "✓ llama.cpp submodule found"
else
    echo "✗ llama.cpp submodule not properly initialized"
    echo "  Run: git submodule add https://github.com/ggerganov/llama.cpp external/llama.cpp"
fi

# Check if faster-whisper is properly initialized
if [ -d "external/faster-whisper" ] && [ -f "external/faster-whisper/README.md" ]; then
    echo "✓ faster-whisper submodule found"
else
    echo "✗ faster-whisper submodule not properly initialized"
    echo "  Run: git submodule add https://github.com/ggerganov/whisper.cpp external/faster-whisper"
fi

echo "Submodule initialization complete."