# AI Components Package

## Overview

The `ai` package contains **AI engine implementations** for speech recognition and intent classification. These components form the core intelligence of the Egyptian Agent.

## Contents

| File | Description |
|------|-------------|
| `EgyptianWhisperASR.java` | Whisper-based ASR optimized for Egyptian Arabic |
| `FunctionGemmaIntentEngine.java` | FunctionGemma model for intent classification |
| `LlamaIntentEngine.java` | Llama-based intent classification engine |

## Architecture

```
Audio Input → ASR Engine → Text → Intent Engine → IntentResult
```

## Components

### EgyptianWhisperASR
Whisper-based Automatic Speech Recognition optimized for Egyptian Arabic dialect.

**Features:**
- Egyptian dialect support
- Offline processing
- Noise robustness
- Real-time transcription

**Usage:**
```java
EgyptianWhisperASR asr = new EgyptianWhisperASR(context);
String text = asr.recognize(audioData);
```

### FunctionGemmaIntentEngine
Intent classification using the FunctionGemma model.

**Features:**
- Function calling support
- Egyptian dialect understanding
- High accuracy (97.8% target)
- Structured output

**Usage:**
```java
FunctionGemmaIntentEngine engine = new FunctionGemmaIntentEngine(context);
IntentResult result = engine.classify(text);
```

### LlamaIntentEngine
Intent classification using Llama models.

**Features:**
- Llama.cpp integration
- Efficient CPU inference
- Configurable model size
- Low latency

**Usage:**
```java
LlamaIntentEngine engine = new LlamaIntentEngine(context);
IntentResult result = engine.classify(text);
```

## Model Files

Models are stored separately and loaded at runtime:
- `models/whisper/` - Whisper models
- `models/functiongemma/` - FunctionGemma models
- `models/llama/` - Llama models

## Performance

| Engine | Accuracy | Latency | Model Size |
|--------|----------|---------|------------|
| Whisper ASR | ~95% | Medium | ~100MB |
| FunctionGemma | ~97.8% | Low | ~500MB |
| Llama | ~95% | Medium | ~500MB-4GB |

## Related Packages

- **`../llm/`** - LLM configuration and engine wrappers
- **`../nlu/`** - Rule-based NLU fallback
- **`../asr/`** - ASR abstraction layer
