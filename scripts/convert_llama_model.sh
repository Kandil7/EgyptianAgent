#!/bin/bash
# Script to convert Llama 3.2 3B model to GGUF format with Q4_K_M quantization
# Designed for Egyptian Agent project

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Llama 3.2 3B Model Conversion Script ===${NC}"
echo -e "${BLUE}Converting for Egyptian Agent on Honor X6c${NC}"

# Configuration
MODEL_NAME="Llama-3.2-3B-Instruct"
QUANTIZATION_TYPE="Q4_K_M"
OUTPUT_DIR="./models"
HF_REPO="meta-llama/Llama-3.2-3B-Instruct"

# Function to print status
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if required tools are available
check_dependencies() {
    print_status "Checking dependencies..."

    if ! command -v python3 &> /dev/null; then
        print_error "Python3 is not installed"
        exit 1
    fi

    if ! command -v pip &> /dev/null; then
        print_error "Pip is not installed"
        exit 1
    fi

    if ! command -v git &> /dev/null; then
        print_error "Git is not installed"
        exit 1
    fi

    print_status "Dependencies check passed"
}

# Install required Python packages
install_python_packages() {
    print_status "Installing required Python packages..."

    pip install torch==2.4.1 transformers==4.51.3 huggingface-hub==0.25.1 sentencepiece protobuf accelerate

    print_status "Python packages installed successfully"
}

# Clone llama.cpp repository
clone_llama_cpp() {
    print_status "Cloning llama.cpp repository..."

    if [ ! -d "llama.cpp" ]; then
        git clone https://github.com/ggerganov/llama.cpp
        cd llama.cpp
        git checkout b3842f  # Using commit from Jan 11, 2026
        cd ..
    else
        print_warning "llama.cpp directory already exists, skipping clone"
    fi

    print_status "llama.cpp cloned successfully"
}

# Build llama.cpp
build_llama_cpp() {
    print_status "Building llama.cpp..."

    cd llama.cpp
    make clean && make -j LLAMA_CUBLAS=0
    cd ..

    print_status "llama.cpp built successfully"
}

# Download Llama 3.2 3B model from Hugging Face
download_model() {
    print_status "Downloading $MODEL_NAME from Hugging Face..."

    if [ ! -d "llama-3.2-3b" ]; then
        huggingface-cli download $HF_REPO \
          --local-dir llama-3.2-3b --local-dir-use-symlinks False
    else
        print_warning "Model directory already exists, skipping download"
    fi

    print_status "Model downloaded successfully"
}

# Convert model to GGUF format (F16)
convert_to_gguf() {
    print_status "Converting model to GGUF F16 format..."

    cd llama.cpp
    python convert_hf_to_gguf.py ../llama-3.2-3b \
      --outfile ../llama-3.2-3b-f16.gguf \
      --outtype f16 \
      --vocab-type bpe \
      --model-type instruct
    cd ..

    print_status "Model converted to GGUF F16 format successfully"
}

# Quantize model to Q4_K_M
quantize_model() {
    print_status "Quantizing model to $QUANTIZATION_TYPE format..."

    cd llama.cpp
    ./llama-quantize ../llama-3.2-3b-f16.gguf \
      ../llama-3.2-3b-$QUANTIZATION_TYPE.gguf $QUANTIZATION_TYPE \
      --update-ctx ../llama-3.2-3b-$QUANTIZATION_TYPE.gguf
    cd ..

    print_status "Model quantized to $QUANTIZATION_TYPE format successfully"
}

# Verify model size and properties
verify_model() {
    print_status "Verifying quantized model..."

    ls -lh llama-3.2-3b-$QUANTIZATION_TYPE.gguf

    # Run a quick benchmark to verify the model works
    cd llama.cpp
    echo "Testing model with a simple prompt..."
    echo "Hello, how are you?" | ./llama-cli -m ../llama-3.2-3b-$QUANTIZATION_TYPE.gguf -p "Question: " -n 32 --temp 0.1
    cd ..

    print_status "Model verification completed"
}

# Move model to assets directory
move_model_to_assets() {
    print_status "Moving quantized model to assets directory..."

    # Create model directory if it doesn't exist
    mkdir -p app/src/main/assets/model/llama-3.2-3b-q4_k_m

    # Copy the quantized model to the assets directory
    cp llama-3.2-3b-$QUANTIZATION_TYPE.gguf app/src/main/assets/model/llama-3.2-3b-q4_k_m/

    print_status "Model copied to assets directory successfully"
}

# Create model metadata
create_metadata() {
    print_status "Creating model metadata..."

    cat > app/src/main/assets/model/llama-3.2-3b-q4_k_m/model.properties << EOF
model.name=Llama 3.2 3B Q4_K_M
model.version=3.2
model.size=3B
model.quantization=Q4_K_M
model.type=gguf
model.path=llama-3.2-3b-q4_k_m/llama-3.2-3b-Q4_K_M.gguf
model.description=Quantized Llama 3.2 3B model optimized for Egyptian dialect processing on Honor X6c
model.size_bytes=$(stat -c%s "llama-3.2-3b-$QUANTIZATION_TYPE.gguf")
model.size_mb=$(echo "scale=2; $(stat -c%s "llama-3.2-3b-$QUANTIZATION_TYPE.gguf") / 1024 / 1024" | bc)
EOF

    print_status "Model metadata created successfully"
}

# Main execution flow
main() {
    check_dependencies
    install_python_packages
    clone_llama_cpp
    build_llama_cpp
    download_model
    convert_to_gguf
    quantize_model
    verify_model
    move_model_to_assets
    create_metadata

    print_status "Llama 3.2 3B model conversion completed successfully!"
    print_status "Model location: app/src/main/assets/model/llama-3.2-3b-q4_k_m/llama-3.2-3b-Q4_K_M.gguf"
    print_status "Model size: $(ls -lh llama-3.2-3b-$QUANTIZATION_TYPE.gguf | awk '{print $5}')"
}

# Run main function
main