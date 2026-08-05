# ASR Package

## Purpose

The `asr` (Automatic Speech Recognition) package contains **speech recognition engines** that convert audio input to text. This package handles the audio-to-text pipeline.

## Contents

| File | Description |
|------|-------------|
| `ASREngineInterface.java` | Interface for ASR engine implementations |
| `ASRManager.java` | Manages ASR engine selection and lifecycle |
| `ASRResult.java` | Container for speech recognition results |
| `AudioRecorder.java` | Audio recording utility |
| `VoskASREngine.java` | Vosk-based ASR engine implementation |
| `WhisperASREngine.java` | Whisper-based ASR engine implementation |

## Architecture

```
Microphone → AudioRecorder → ASR Engine → Text Output
```

## Supported Engines

### Vosk ASR
- Offline speech recognition
- Supports multiple languages including Arabic
- Lower resource usage
- Good for continuous recognition

### Whisper ASR
- OpenAI Whisper model
- Higher accuracy
- Supports Egyptian dialect
- Higher resource usage

## Usage

```java
ASREngine engine = new WhisperASREngine(context);
ASRResult result = engine.recognize(audioData);
String text = result.getText();
```

## Related Packages

- **`../nlu/`** - Receives text for understanding
- **`../stt/`** - Alternative speech-to-text implementation
- **`../ai/`** - AI/ML infrastructure

## Performance

| Engine | Accuracy | Latency | Resource Usage |
|--------|----------|---------|----------------|
| Vosk | ~85% | Low | Low |
| Whisper | ~95% | Medium | High |
