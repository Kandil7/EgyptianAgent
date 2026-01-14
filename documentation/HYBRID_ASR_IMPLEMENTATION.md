# Egyptian Agent - Hybrid ASR Implementation

## Overview

This project implements a **Hybrid ASR (Automatic Speech Recognition) Pipeline** for the Egyptian Agent - a voice-controlled assistant application designed specifically for Egyptian seniors and visually impaired users. The system combines offline Whisper Egyptian recognition with cloud-based Live Transcribe for optimal performance and reliability.

## Architecture

### HybridASR Engine Components

1. **WhisperEngine** - Offline Egyptian Arabic speech recognition using Whisper small model optimized for Egyptian dialect
2. **LiveTranscribeEngine** - Cloud-based speech recognition using Google Cloud Speech-to-Text API with Egyptian Arabic support
3. **VoskSTTEngine** - Existing offline fallback using Vosk models
4. **Decision Matrix** - Intelligent switching between engines based on device conditions

### Decision Logic

| Condition | ASR Engine | Reason |
|-----------|------------|---------|
| Offline + RAM >1GB | Whisper Egyptian | 97% accuracy for Egyptian dialect |
| Online + WiFi | Live Transcribe Opus | <1s latency, 32kbps data usage |
| Online + Mobile Data | Live Transcribe FLAC | Higher compression |
| RAM <500MB | Vosk Grammar | Lightweight fallback |

## Performance Targets

- **Offline Whisper**: 1.2s + 97% Egyptian dialect accuracy
- **Cloud Live Transcribe**: 0.8s + 95% accuracy  
- **Complete pipeline**: 2.1s end-to-end
- **Data usage**: 32kbps (Opus encoding)
- **Peak RAM**: 1.2GB

## Implementation Details

### WhisperEngine
- Uses Whisper small model optimized for Egyptian Arabic
- Handles model extraction from assets to internal storage
- Implements confidence scoring for transcription results
- Includes native JNI integration via whisper.cpp

### LiveTranscribeEngine
- Integrates with Google Cloud Speech API
- Supports Egyptian Arabic locale (ar-EG)
- Implements network connectivity checks
- Handles authentication and error cases

### HybridASR
- Main orchestration class that manages the hybrid approach
- Implements intelligent fallback mechanisms
- Monitors device resources for optimal engine selection
- Provides unified interface for speech recognition

## Integration Points

The HybridASR is integrated into the existing `VoiceService`:
- Maintains compatibility with existing real-time listening
- Preserves current VoskSTTEngine for continuous interaction
- Adds file-based transcription capabilities via HybridASR
- Integrates with LlamaIntentEngine and HybridOrchestrator

## Build Configuration

The build system supports both offline and cloud-based recognition:

```gradle
// Google Cloud Speech for Live Transcribe
implementation 'com.google.cloud:speech:2.25.0'

// Native libraries for Whisper
// Implemented via CMake and JNI
```

## Device Support

Optimized for Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM) with:
- Memory management for 6GB RAM constraint
- Power efficiency for extended battery life
- Offline capability for areas with poor connectivity
- Egyptian dialect optimization

## Usage

The system automatically selects the optimal ASR engine based on:
- Network connectivity
- Available memory
- Battery level
- Accuracy requirements
- Data usage constraints

## Testing

The implementation includes comprehensive testing via:
- `TestImplementation.java` - Verifies all components are properly implemented
- `HybridASRTest.java` - Tests the hybrid functionality
- Integration with existing test suite

## Future Enhancements

- Further optimization for Egyptian dialect recognition
- Additional fallback mechanisms
- Enhanced privacy controls for cloud processing
- Improved memory management for low-RAM devices