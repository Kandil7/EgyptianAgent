#!/bin/bash
# Script to download, convert, and set up the Llama 3.2 3B model for Egyptian Agent
# This script automates the entire model setup process

set -e  # Exit on any error

echo "=== Egyptian Agent - Llama 3.2 3B Model Setup ==="

# Check if required tools are available
if ! command -v python3 &> /dev/null; then
    echo "Error: python3 is required but not installed."
    exit 1
fi

if ! command -v pip &> /dev/null; then
    echo "Error: pip is required but not installed."
    exit 1
fi

# Create necessary directories
mkdir -p app/src/main/assets/model
mkdir -p scripts

# Install required Python packages
echo "Installing required Python packages..."
pip install huggingface_hub transformers optimum

# Download the Llama 3.2 3B model from Hugging Face
echo "Downloading Llama 3.2 3B model from Hugging Face..."
python3 -c "
from huggingface_hub import snapshot_download
import os

model_id = 'meta-llama/Llama-3.2-3b-Instruct'  # Using the instruct version for better responses
cache_dir = './models/llama-3.2-3b'

print(f'Downloading model: {model_id}')
snapshot_download(
    repo_id=model_id,
    cache_dir=cache_dir,
    local_dir=cache_dir,
    local_dir_use_symlinks=False
)
print('Model downloaded successfully!')
"

# Convert the model to GGUF format using llama.cpp
echo "Setting up llama.cpp for model conversion..."

# Check if llama.cpp exists, if not clone it
if [ ! -d "llama.cpp" ]; then
    echo "Cloning llama.cpp repository..."
    git clone https://github.com/ggerganov/llama.cpp.git
    cd llama.cpp
else
    cd llama.cpp
    echo "Updating existing llama.cpp repository..."
    git pull origin master
fi

# Build llama.cpp
echo "Building llama.cpp..."
make clean
LLAMA_CUBLAS=1 make -j$(nproc)

# Go back to project root
cd ..

# Convert the model to GGUF format
echo "Converting model to GGUF format..."
python3 -c "
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch
import os

# Load the model and tokenizer
model_path = './models/llama-3.2-3b'
tokenizer = AutoTokenizer.from_pretrained(model_path)
model = AutoModelForCausalLM.from_pretrained(model_path)

# Save in GGUF format using llama.cpp's converter
print('Model loaded, preparing for GGUF conversion...')
"

# Use llama.cpp's Python tools to convert the model
echo "Running model conversion with llama.cpp..."
python3 llama.cpp/convert-hf-to-gguf.py ./models/llama-3.2-3b --outfile ./models/llama-3.2-3b-f16.gguf --outtype f16

# Quantize the model to Q4_K_M format
echo "Quantizing model to Q4_K_M format..."
./llama.cpp/llama-quantize ./models/llama-3.2-3b-f16.gguf ./models/llama-3.2-3b-Q4_K_M.gguf Q4_K_M

# Move the quantized model to the assets directory
echo "Moving quantized model to assets..."
mv ./models/llama-3.2-3b-Q4_K_M.gguf app/src/main/assets/model/

echo "=== Model setup completed successfully! ==="
echo "Model location: app/src/main/assets/model/llama-3.2-3b-Q4_K_M.gguf"
echo "Model size: $(ls -lh app/src/main/assets/model/llama-3.2-3b-Q4_K_M.gguf | awk '{print $5}')"
echo ""
echo "Next steps:"
echo "1. Build the native library: cd llama.cpp && make clean && LLAMA_CUBLAS=1 make -j\$(nproc)"
echo "2. Build the Android app: ./gradlew :app:assembleDebug"
echo "3. Install on device: adb install app/build/outputs/apk/debug/app-debug.apk"