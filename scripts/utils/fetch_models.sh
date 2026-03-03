#!/bin/bash
# Egyptian Agent - Model Download Script
# Downloads required AI models for offline operation

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Configuration
MODELS_DIR="${1:-app/src/main/assets/models}"
DOWNLOAD_DIR="/tmp/egyptian_agent_models"

# Model configurations
declare -A MODELS=(
    ["vosk-model-small-ar"]="https://alphacephei.com/vosk/models/vosk-model-small-ar-0.22.tar.gz"
    ["llama-3.2-1b-instruct-q4_k_m"]="https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
    ["whisper-small-ar"]="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
)

# Create directories
mkdir -p "$MODELS_DIR"
mkdir -p "$DOWNLOAD_DIR"

log_step "Egyptian Agent Model Downloader"
log_info "Models directory: $MODELS_DIR"
log_info "Download directory: $DOWNLOAD_DIR"

# Function to download with progress
download_model() {
    local name=$1
    local url=$2
    local output_file="$DOWNLOAD_DIR/$name"
    
    if [ -f "$output_file" ]; then
        log_info "✓ $name already downloaded"
        return 0
    fi
    
    log_info "Downloading $name..."
    
    if command -v wget &> /dev/null; then
        wget --show-progress -q -O "$output_file" "$url"
    elif command -v curl &> /dev/null; then
        curl -L -# -o "$output_file" "$url"
    else
        log_error "Neither wget nor curl found. Please install one."
        return 1
    fi
    
    if [ $? -eq 0 ]; then
        log_info "✓ $name downloaded successfully"
        return 0
    else
        log_error "✗ Failed to download $name"
        return 1
    fi
}

# Download Vosk Arabic model
log_step "Downloading Vosk Arabic STT Model..."
if download_model "vosk-model-small-ar.tar.gz" "${MODELS[vosk-model-small-ar]}"; then
    log_info "Extracting Vosk model..."
    tar -xzf "$DOWNLOAD_DIR/vosk-model-small-ar.tar.gz" -C "$MODELS_DIR" --strip-components=1
    log_info "✓ Vosk model ready"
fi

# Download Llama model (optional, large file)
log_step "Llama Model (Optional - for full LLM capabilities)"
log_warn "Llama model is ~700MB. Skip if using mock implementation."
read -p "Download Llama model? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if download_model "llama-3.2-1b.gguf" "${MODELS[llama-3.2-1b-instruct-q4_k_m]}"; then
        mv "$DOWNLOAD_DIR/llama-3.2-1b.gguf" "$MODELS_DIR/"
        log_info "✓ Llama model ready"
    fi
fi

# Download Whisper model (optional)
log_step "Whisper Model (Optional - for enhanced ASR)"
log_warn "Whisper model is ~250MB. Skip if using Vosk only."
read -p "Download Whisper model? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if download_model "ggml-small.bin" "${MODELS[whisper-small-ar]}"; then
        mv "$DOWNLOAD_DIR/ggml-small.bin" "$MODELS_DIR/whisper-ar.bin"
        log_info "✓ Whisper model ready"
    fi
fi

# List downloaded models
log_step "Downloaded Models:"
ls -lh "$MODELS_DIR"

# Cleanup
rm -rf "$DOWNLOAD_DIR"

log_info "Model download complete!"
log_info "Models are located in: $MODELS_DIR"
