# Architecture Documentation

This directory contains all architecture-related documentation for the EgyptianAgent project.

## Documents

### Core Architecture
- [System Architecture](ARCHITECTURE.md) - Overall system design, component relationships, and data flow
- [Production Architecture](PRODUCTION_ARCHITECTURE.md) - Production deployment architecture and infrastructure

### FunctionGemma
- [FunctionGemma Architecture](FUNCTIONGEMMA_ARCHITECTURE.md) - FunctionGemma integration design and implementation

## Overview

The EgyptianAgent architecture follows a layered approach:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│              (UI, Voice Interface, Accessibility)            │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                         │
│         (Intent Routing, Command Execution, Orchestrator)    │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                             │
│           (Business Logic, Use Cases, Entities)              │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                       │
│     (ASR, NLU, LLM, TTS, Database, System Integration)       │
└─────────────────────────────────────────────────────────────┘
```

## Key Components

1. **ASR (Automatic Speech Recognition)** - Converts Egyptian Arabic speech to text
2. **NLU (Natural Language Understanding)** - Interprets user intent
3. **LLM (Large Language Model)** - FunctionGemma for advanced reasoning
4. **TTS (Text-to-Speech)** - Converts responses to audio
5. **Command Executor** - Executes system commands (calls, messages, alarms)

## Related Documentation

- [Deployment Guide](../deployment/DEPLOYMENT_GUIDE.md)
- [API Reference](../api/API_REFERENCE.md)
- [Testing](../testing/TEST_SUITE.md)
