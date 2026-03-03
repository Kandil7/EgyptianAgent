# NLP Package

## Purpose

The `nlp` (Natural Language Processing) package contains **core NLP utilities** for intent classification. This is the foundational NLP layer that provides basic intent types and result containers.

## Contents

| File | Description |
|------|-------------|
| `IntentType.java` | Enum defining all supported intent types |
| `IntentResult.java` | Container for classification results |
| `IntentRouter.java` | Routes intents to appropriate handlers |

## Architecture

```
Input Text → NLP → IntentResult → Router → Executor
```

## Relationship with NLU

- **`nlp/`** - Core types and routing (stable, minimal dependencies)
- **`nlu/`** - Advanced understanding with Egyptian dialect support

The NLP package provides:
- Basic intent type definitions
- Simple result containers
- Routing logic

The NLU package provides:
- Egyptian dialect normalization
- Advanced pattern matching
- Entity extraction
- Confidence scoring

## Usage

```java
IntentResult result = new IntentResult(IntentType.CALL_CONTACT, 0.9f);
result.setEntity("contact", "ماما");
IntentRouter.route(result);
```

## Related Packages

- **`../nlu/`** - Advanced NLU with Egyptian dialect support
- **`../asr/`** - Automatic Speech Recognition
- **`../stt/`** - Speech-to-Text processing
