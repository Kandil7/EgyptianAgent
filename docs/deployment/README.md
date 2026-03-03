# Deployment Documentation

This directory contains deployment guides and instructions for the EgyptianAgent project.

## Documents

### Main Deployment
- [Deployment Guide](DEPLOYMENT_GUIDE.md) - Primary deployment instructions
- [Production Deployment Guide](production_deployment_guide.md) - Production environment setup

### FunctionGemma
- [FunctionGemma Deployment Guide](FUNCTIONGEMMA_DEPLOYMENT_GUIDE.md) - FunctionGemma-specific deployment

## Quick Start

### Development Deployment
```bash
# Initialize submodules
./scripts/deploy/initialize_submodules.sh

# Build debug version
./scripts/build/build.sh

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Production Deployment
```bash
# Build release version
./scripts/build/build_production.sh

# Deploy to device
./scripts/deploy/deploy_production.sh
```

### FunctionGemma Deployment
```bash
# Download FunctionGemma model
./scripts/model/download_functiongemma_model.sh

# Build with FunctionGemma
./scripts/build/build_functiongemma.sh

# Deploy
./scripts/deploy/deploy_functiongemma.sh
```

## Deployment Targets

| Target | Script | Description |
|--------|--------|-------------|
| Honor X6c | `build.sh --target honor-x6c` | Optimized for Honor X6c device |
| Generic Android | `build.sh --target generic` | Standard Android deployment |
| Production | `build_production.sh` | Release build with optimizations |

## Prerequisites

- Android SDK 33+
- NDK 25+
- CMake 3.22+
- Python 3.10+
- Git with LFS

## Related Documentation

- [Architecture](../architecture/ARCHITECTURE.md)
- [Troubleshooting](../guides/TROUBLESHOOTING.md)
- [API Reference](../api/API_REFERENCE.md)
