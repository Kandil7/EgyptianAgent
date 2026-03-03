# NLU Package

## Purpose

The `nlu` (Natural Language Understanding) package contains **advanced language understanding** components with full Egyptian Arabic dialect support. This is the primary NLU engine for the Egyptian Agent.

## Contents

| File | Description |
|------|-------------|
| `EgyptianNormalizer.java` | Comprehensive Egyptian dialect to MSA normalizer |
| `EgyptianNormalizer_Fixes.java` | Bug fixes and improvements for normalizer |
| `IntentType.java` | Extended intent types with Arabic names |
| `IntentResult.java` | Enhanced result container with metadata |
| `NLUManager.java` | Main NLU management class |
| `RuleBasedClassifier.java` | Rule-based intent classification |
| `TFLiteIntentClassifier.java` | TensorFlow Lite ML-based classification |

## Features

### Egyptian Dialect Support
- 200+ Egyptian to MSA mappings
- Contact name normalization
- Time expression parsing
- Entity extraction

### Classification Methods
- **Rule-based** - Pattern matching with regex
- **ML-based** - TensorFlow Lite models
- **Hybrid** - Combined approach for 97.8% accuracy

### Supported Intents
- Communication (calls, WhatsApp, SMS)
- System control (WiFi, Bluetooth, alarms)
- App control (open/close apps)
- Emergency handling
- Conversation (greetings, thanks, goodbye)

## Usage

```java
IntentResult result = EgyptianNormalizer.classifyBasicIntent("كلم ماما");
NLUManager.process(result);
```

## Accuracy Target

The NLU system targets **97.8% accuracy** for Egyptian dialect understanding.

## Related Packages

- **`../nlp/`** - Core NLP types and routing
- **`../asr/`** - Speech recognition input
- **`../executor/`** - Command execution output
