#!/bin/bash
# Initialize git submodules for external dependencies
# Egyptian Agent - Native Library Setup

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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXTERNAL_DIR="$SCRIPT_DIR/external"

log_step "Egyptian Agent - Submodule Initialization"
log_info "External directory: $EXTERNAL_DIR"

# Create external directory if it doesn't exist
mkdir -p "$EXTERNAL_DIR"

# Check if we're in a git repository
if [ ! -d "$SCRIPT_DIR/.git" ]; then
    log_error "Not a git repository. Please run this from the project root."
    exit 1
fi

# Initialize git submodules
log_step "Initializing git submodules..."

# Check if .gitmodules exists
if [ -f "$SCRIPT_DIR/.gitmodules" ]; then
    log_info "Found .gitmodules, initializing..."
    git submodule init
    git submodule update --recursive
else
    log_warn ".gitmodules not found. Setting up submodules manually..."
fi

# ============================================================================
# llama.cpp Setup
# ============================================================================
LLAMA_DIR="$EXTERNAL_DIR/llama.cpp"
log_step "Setting up llama.cpp..."

if [ -d "$LLAMA_DIR" ] && [ -f "$LLAMA_DIR/CMakeLists.txt" ]; then
    log_info "✓ llama.cpp already initialized"
    cd "$LLAMA_DIR"
    git pull --rebase 2>/dev/null || true
    git submodule update --init --recursive 2>/dev/null || true
    cd "$SCRIPT_DIR"
else
    log_info "Cloning llama.cpp..."
    if [ -d "$LLAMA_DIR" ]; then
        rm -rf "$LLAMA_DIR"
    fi
    git clone --depth 1 https://github.com/ggerganov/llama.cpp.git "$LLAMA_DIR"
    cd "$LLAMA_DIR"
    git submodule update --init --recursive 2>/dev/null || log_warn "Some llama.cpp submodules failed to initialize"
    cd "$SCRIPT_DIR"
    log_info "✓ llama.cpp initialized"
fi

# ============================================================================
# whisper.cpp Setup
# ============================================================================
WHISPER_DIR="$EXTERNAL_DIR/whisper.cpp"
log_step "Setting up whisper.cpp..."

if [ -d "$WHISPER_DIR" ] && [ -f "$WHISPER_DIR/CMakeLists.txt" ]; then
    log_info "✓ whisper.cpp already initialized"
    cd "$WHISPER_DIR"
    git pull --rebase 2>/dev/null || true
    cd "$SCRIPT_DIR"
else
    log_info "Cloning whisper.cpp..."
    if [ -d "$WHISPER_DIR" ]; then
        rm -rf "$WHISPER_DIR"
    fi
    git clone --depth 1 https://github.com/ggerganov/whisper.cpp.git "$WHISPER_DIR"
    cd "$WHISPER_DIR"
    git submodule update --init --recursive 2>/dev/null || log_warn "Some whisper.cpp submodules failed to initialize"
    cd "$SCRIPT_DIR"
    log_info "✓ whisper.cpp initialized"
fi

# ============================================================================
# Verification
# ============================================================================
log_step "Verifying submodules..."

ERRORS=0

# Verify llama.cpp
if [ -f "$LLAMA_DIR/CMakeLists.txt" ]; then
    log_info "✓ llama.cpp CMakeLists.txt found"
else
    log_error "✗ llama.cpp CMakeLists.txt not found"
    ERRORS=$((ERRORS + 1))
fi

# Verify whisper.cpp
if [ -f "$WHISPER_DIR/CMakeLists.txt" ]; then
    log_info "✓ whisper.cpp CMakeLists.txt found"
else
    log_error "✗ whisper.cpp CMakeLists.txt not found"
    ERRORS=$((ERRORS + 1))
fi

# Summary
echo ""
if [ $ERRORS -eq 0 ]; then
    log_info "✓ All submodules initialized successfully!"
    echo ""
    echo "Next steps:"
    echo "  1. Run: ./scripts/fetch_models.sh (to download AI models)"
    echo "  2. Run: ./build.sh --release (to build the application)"
else
    log_error "✗ $ERRORS error(s) during initialization"
    echo ""
    echo "Troubleshooting:"
    echo "  - Check your internet connection"
    echo "  - Run: git submodule sync"
    echo "  - Run: git submodule update --init --recursive --force"
    exit 1
fi