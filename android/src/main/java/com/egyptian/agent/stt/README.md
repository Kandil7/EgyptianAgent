# STT Package

## Purpose

The `stt` (Speech-to-Text) package contains **speech-to-text processing** components. This package provides an alternative STT implementation with Egyptian dialect normalization.

## Contents

| File | Description |
|------|-------------|
| `EgyptianNormalizer.java` | Egyptian dialect normalizer for STT output |
| `VocabularyManager.java` | Manages domain-specific vocabulary |
| `VoskSTTEngine.java` | Vosk-based STT engine |

## Architecture

```
Audio Input → STT Engine → Raw Text → Normalizer → Clean Text → NLU
```

## Difference from ASR Package

| Aspect | ASR Package | STT Package |
|--------|-------------|-------------|
| Focus | Engine abstraction | Text processing |
| Normalization | Post-processing | Integrated |
| Vocabulary | Generic | Domain-specific |

## Features

- Egyptian dialect normalization
- Vocabulary management for domain terms
- Vosk integration
- Post-processing rules

## Usage

```java
String rawText = VoskSTTEngine.recognize(audioData);
String normalized = EgyptianNormalizer.normalize(rawText);
```

## Related Packages

- **`../asr/`** - Primary ASR implementation
- **`../nlu/`** - Receives normalized text
- **`../nlp/`** - Core NLP types
