# Egyptian Agent - Main Package

## Overview

The `com.egyptian.agent` package is the core package for the Egyptian Agent Android application. It contains all the main components that power the voice-activated assistant with Egyptian Arabic dialect support.

## Architecture

```
com.egyptian.agent/
│
├── ai/           → AI engines (ASR, intent classification)
├── llm/          → LLM integration (FunctionGemma, Llama)
├── nlu/          → Natural Language Understanding
├── nlp/          → Core NLP types and routing
├── asr/          → Automatic Speech Recognition
├── stt/          → Speech-to-Text processing
├── executor/     → Command controllers
├── executors/    → Command workers
├── core/         → Core agent functionality
├── service/      → Background services
├── receivers/    → Broadcast receivers
├── ui/           → User interface components
├── security/     → Security features
├── accessibility/→ Accessibility support
├── emergency/    → Emergency handling
├── analytics/    → Usage analytics
├── performance/  → Performance monitoring
├── feedback/     → User feedback
├── updates/      → Update management
├── backup/       → Backup functionality
├── contacts/     → Contact management
├── hybrid/       → Hybrid processing
├── wakeword/     → Wake word detection
├── system/       → System utilities
├── utils/        → Utility classes
├── test/         → In-source test suites
└── testing/      → Automated test infrastructure
```

## Key Components

### AI/ML Layer
| Package | Purpose |
|---------|---------|
| `ai/` | AI engines for ASR and intent classification |
| `llm/` | Large Language Model integration |
| `nlu/` | Egyptian dialect understanding |
| `nlp/` | Core NLP types |
| `asr/` | Speech recognition |
| `stt/` | Speech-to-text |
| `wakeword/` | Voice activation |

### Execution Layer
| Package | Purpose |
|---------|---------|
| `executor/` | Command controllers |
| `executors/` | Command workers |
| `service/` | Background services |
| `receivers/` | Broadcast receivers |

### Feature Layer
| Package | Purpose |
|---------|---------|
| `emergency/` | Emergency handling |
| `contacts/` | Contact management |
| `accessibility/` | Accessibility features |
| `security/` | Security features |

### Support Layer
| Package | Purpose |
|---------|---------|
| `analytics/` | Usage analytics |
| `performance/` | Performance monitoring |
| `feedback/` | User feedback |
| `updates/` | Update management |
| `backup/` | Backup functionality |

## Usage

### Initialize Agent
```java
EgyptianAgent agent = new EgyptianAgent(context);
agent.initialize();
```

### Process Voice Command
```java
String audioPath = recordAudio();
IntentResult result = agent.processVoiceCommand(audioPath);
agent.executeIntent(result);
```

## Testing

Tests are located in `app/src/test/java/com/egyptian/agent/` with parallel package structure.

## Related Documentation

- [AI Components](ai/README.md)
- [LLM Engines](llm/README.md)
- [NLU Package](nlu/README.md)
- [Test Overview](../../../test/java/com/egyptian/agent/README.md)
