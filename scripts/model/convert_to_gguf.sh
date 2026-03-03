#!/bin/bash
# FunctionGemma Egyptian Dialect Model Conversion Script
# Converts fine-tuned Hugging Face model to GGUF format for mobile deployment
#
# Author: EgyptianAgent Team
# Date: 2026

set -e  # Exit on error

# Configuration
MODEL_DIR="${1:-models/functiongemma-270m-egyptian}"
OUTPUT_DIR="${2:-models}"
QUANTIZATION="${3:-Q4_K_M}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running on Windows (Git Bash)
if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
    IS_WINDOWS=true
    log_info "Running on Windows (Git Bash)"
else
    IS_WINDOWS=false
fi

# Function to check Python version
check_python() {
    log_info "Checking Python version..."
    if command -v python3 &> /dev/null; then
        PYTHON_CMD="python3"
    elif command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        log_error "Python not found. Please install Python 3.8+"
        exit 1
    fi
    
    PYTHON_VERSION=$($PYTHON_CMD --version 2>&1 | cut -d' ' -f2)
    log_info "Found Python $PYTHON_VERSION"
}

# Function to check if llama.cpp exists
check_llama_cpp() {
    if [ -d "llama.cpp" ]; then
        log_info "llama.cpp directory found"
        return 0
    else
        log_warning "llama.cpp not found, will clone it"
        return 1
    fi
}

# Function to clone llama.cpp
clone_llama_cpp() {
    log_info "Cloning llama.cpp repository..."
    git clone --depth 1 https://github.com/ggerganov/llama.cpp.git
    log_success "llama.cpp cloned successfully"
}

# Function to install llama.cpp dependencies
install_llama_cpp_deps() {
    log_info "Installing llama.cpp Python dependencies..."
    cd llama.cpp
    
    if [ "$IS_WINDOWS" = true ]; then
        $PYTHON_CMD -m pip install --upgrade pip
        $PYTHON_CMD -m pip install -r requirements.txt
    else
        pip3 install -r requirements.txt
    fi
    
    cd ..
    log_success "Dependencies installed"
}

# Function to build llama.cpp (for quantization tool)
build_llama_cpp() {
    log_info "Building llama.cpp..."
    cd llama.cpp
    
    if [ "$IS_WINDOWS" = true ]; then
        # Windows build using CMake
        mkdir -p build
        cd build
        cmake .. -DCMAKE_BUILD_TYPE=Release
        cmake --build . --config Release
        cd ..
    else
        # Linux/Mac build using make
        make clean
        make -j$(nproc)
    fi
    
    cd ..
    log_success "llama.cpp built successfully"
}

# Function to convert model to GGUF (F16)
convert_to_gguf_f16() {
    log_info "Converting model to GGUF (F16 format)..."
    
    MODEL_NAME=$(basename "$MODEL_DIR")
    F16_OUTPUT="$OUTPUT_DIR/${MODEL_NAME}-f16.gguf"
    
    cd llama.cpp
    
    $PYTHON_CMD convert-hf-to-gguf.py \
        "../$MODEL_DIR" \
        --outfile "../$F16_OUTPUT" \
        --vocab-type bpe
    
    cd ..
    
    if [ -f "$F16_OUTPUT" ]; then
        F16_SIZE=$(du -h "$F16_OUTPUT" | cut -f1)
        log_success "F16 model created: $F16_OUTPUT ($F16_SIZE)"
    else
        log_error "Failed to create F16 model"
        exit 1
    fi
}

# Function to quantize model
quantize_model() {
    log_info "Quantizing model to $QUANTIZATION..."
    
    MODEL_NAME=$(basename "$MODEL_DIR")
    F16_INPUT="$OUTPUT_DIR/${MODEL_NAME}-f16.gguf"
    QUANT_OUTPUT="$OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf"
    
    if [ ! -f "$F16_INPUT" ]; then
        log_error "F16 model not found: $F16_INPUT"
        exit 1
    fi
    
    cd llama.cpp
    
    if [ "$IS_WINDOWS" = true ]; then
        ./build/bin/Release/quantize.exe \
            "../$F16_INPUT" \
            "../$QUANT_OUTPUT" \
            "$QUANTIZATION"
    else
        ./quantize \
            "../$F16_INPUT" \
            "../$QUANT_OUTPUT" \
            "$QUANTIZATION"
    fi
    
    cd ..
    
    if [ -f "$QUANT_OUTPUT" ]; then
        QUANT_SIZE=$(du -h "$QUANT_OUTPUT" | cut -f1)
        log_success "Quantized model created: $QUANT_OUTPUT ($QUANT_SIZE)"
    else
        log_error "Failed to create quantized model"
        exit 1
    fi
}

# Function to verify output
verify_output() {
    log_info "Verifying output model..."
    
    MODEL_NAME=$(basename "$MODEL_DIR")
    QUANT_OUTPUT="$OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf"
    
    if [ ! -f "$QUANT_OUTPUT" ]; then
        log_error "Output model not found: $QUANT_OUTPUT"
        exit 1
    fi
    
    # Check file size (should be around 288MB for Q4_K_M)
    FILE_SIZE=$(stat -c%s "$QUANT_OUTPUT" 2>/dev/null || stat -f%z "$QUANT_OUTPUT" 2>/dev/null)
    FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
    
    log_info "Model size: ${FILE_SIZE_MB}MB"
    
    # Verify size is reasonable (200-400MB for 270M model quantized)
    if [ "$FILE_SIZE_MB" -lt 200 ] || [ "$FILE_SIZE_MB" -gt 500 ]; then
        log_warning "Model size seems unusual: ${FILE_SIZE_MB}MB"
    else
        log_success "Model size is within expected range"
    fi
    
    # Test model info using Python
    cd llama.cpp
    $PYTHON_CMD -c "
import sys
sys.path.insert(0, '.')
try:
    from gguf import GGUFReader
    reader = GGUFReader('../$QUANT_OUTPUT')
    print('Model metadata:')
    for key, value in reader.fields.items():
        if not key.startswith('_'):
            print(f'  {key}: {value}')
except Exception as e:
    print(f'Could not read GGUF metadata: {e}')
"
    cd ..
    
    log_success "Model verification complete"
}

# Function to clean up intermediate files
cleanup() {
    log_info "Cleaning up intermediate files..."
    
    MODEL_NAME=$(basename "$MODEL_DIR")
    F16_FILE="$OUTPUT_DIR/${MODEL_NAME}-f16.gguf"
    
    if [ -f "$F16_FILE" ]; then
        rm -f "$F16_FILE"
        log_info "Removed F16 intermediate file"
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [MODEL_DIR] [OUTPUT_DIR] [QUANTIZATION]"
    echo ""
    echo "Arguments:"
    echo "  MODEL_DIR      Path to fine-tuned model directory (default: models/functiongemma-270m-egyptian)"
    echo "  OUTPUT_DIR     Output directory for GGUF files (default: models)"
    echo "  QUANTIZATION   Quantization type (default: Q4_K_M)"
    echo ""
    echo "Quantization options:"
    echo "  Q4_K_M    - Best quality/size balance (recommended)"
    echo "  Q4_K_S    - Smaller size, slightly lower quality"
    echo "  Q5_K_M    - Higher quality, larger size"
    echo "  Q5_K_S    - Higher quality, smaller size"
    echo "  Q6_K      - Very high quality, larger size"
    echo "  Q8_0      - Near-lossless, largest size"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Use all defaults"
    echo "  $0 models/my-model models Q4_K_S      # Custom model and quantization"
}

# Main execution
main() {
    echo "=============================================="
    echo "FunctionGemma Egyptian Model Conversion"
    echo "=============================================="
    echo ""
    
    # Check for help flag
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        show_usage
        exit 0
    fi
    
    # Check if model directory exists
    if [ ! -d "$MODEL_DIR" ]; then
        log_error "Model directory not found: $MODEL_DIR"
        log_info "Please ensure the model has been fine-tuned first"
        exit 1
    fi
    
    # Create output directory if it doesn't exist
    mkdir -p "$OUTPUT_DIR"
    
    # Check Python
    check_python
    
    # Check/install llama.cpp
    if ! check_llama_cpp; then
        clone_llama_cpp
        install_llama_cpp_deps
        build_llama_cpp
    else
        # Just install dependencies if llama.cpp exists
        cd llama.cpp
        if [ "$IS_WINDOWS" = true ]; then
            $PYTHON_CMD -m pip install -r requirements.txt 2>/dev/null || true
        else
            pip3 install -r requirements.txt 2>/dev/null || true
        fi
        cd ..
    fi
    
    # Convert model
    convert_to_gguf_f16
    
    # Quantize model
    quantize_model
    
    # Verify output
    verify_output
    
    # Optional: cleanup intermediate files
    # cleanup
    
    echo ""
    echo "=============================================="
    log_success "Conversion completed successfully!"
    echo "=============================================="
    echo ""
    
    MODEL_NAME=$(basename "$MODEL_DIR")
    echo "Output model: $OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf"
    echo ""
    echo "To use this model with llama.cpp:"
    echo "  ./main -m $OUTPUT_DIR/${MODEL_NAME}-${QUANTIZATION}.gguf -p 'اتصل بماما' -n 128"
    echo ""
}

# Run main function
main "$@"
