# External Submodules

This directory contains external library submodules used by the EgyptianAgent project. These are Git submodules that point to third-party repositories.

## Submodules

### 1. llama.cpp
- **Purpose**: C/C++ inference engine for Llama family models (LLaMA, Mistral, etc.)
- **Usage**: Provides efficient CPU-based LLM inference for on-device AI
- **Location**: `external/llama.cpp/`
- **Original Repository**: https://github.com/ggerganov/llama.cpp
- **Build**: See `llama.cpp/README.md` for build instructions

### 2. whisper.cpp
- **Purpose**: C/C++ implementation of OpenAI's Whisper speech-to-text model
- **Usage**: Provides offline speech recognition for voice commands
- **Location**: `external/whisper.cpp/`
- **Original Repository**: https://github.com/ggerganov/whisper.cpp
- **Build**: See `whisper.cpp/README.md` for build instructions

### 3. faster-whisper
- **Purpose**: Optimized Whisper implementation with CTranslate2 backend
- **Usage**: Alternative speech-to-text engine with better performance
- **Location**: `external/faster-whisper/`
- **Original Repository**: https://github.com/SYSTRAN/faster-whisper
- **Build**: See `faster-whisper/README.md` for build instructions

## Managing Submodules

### Initialize Submodules
```bash
git submodule init
git submodule update
```

### Update Submodules
```bash
git submodule update --remote
```

### Clone with Submodules
```bash
git clone --recursive <repository-url>
```

## Build Integration

These submodules are built as part of the project's CMake/NDK build process. See the root `CMakeLists.txt` for integration details.

## License Notes

Each submodule has its own license. Please refer to the individual submodule's LICENSE file:
- **llama.cpp**: MIT License
- **whisper.cpp**: MIT License
- **faster-whisper**: MIT License

## Model Files

Model files (`.bin`, `.gguf`, `.ggml`) are NOT included in this repository due to their large size. They must be downloaded separately:

1. Download models from Hugging Face or official sources
2. Place in appropriate `models/` directory
3. Update model paths in configuration files

## Troubleshooting

### Submodule Not Found
```bash
git submodule sync
git submodule update --init --recursive
```

### Build Errors
Ensure you have:
- Android NDK installed
- CMake configured
- Required system dependencies (for native builds)

## Related Documentation

- Root `README.md` - Project overview
- `docs/` - Project documentation
- `CMakeLists.txt` - Native build configuration
