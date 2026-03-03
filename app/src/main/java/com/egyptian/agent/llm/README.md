# LLM Engines Package

## Overview

The `llm` package contains **Large Language Model configuration and engine wrappers**. It provides the integration layer for running LLMs on-device for intent classification and natural language understanding.

## Contents

| File | Description |
|------|-------------|
| `FunctionGemmaEngine.java` | Main FunctionGemma inference engine |
| `FunctionGemmaConfig.java` | Configuration for FunctionGemma |
| `FunctionCallSchema.java` | Schema for function calling |
| `LlamaEngine.java` | Main Llama inference engine |
| `LlamaConfig.java` | Configuration for Llama models |
| `LlamaConfigLoader.java` | Loads Llama configuration from files |
| `LlamaNLUClassifier.java` | Llama-based NLU classifier |
| `EgyptianArabicTestSuite.java` | Test suite for Egyptian Arabic |

## Architecture

```
Configuration → Engine → Model → Inference → Result
```

## Components

### FunctionGemmaEngine
Main engine for running FunctionGemma models.

**Features:**
- Function calling support
- Egyptian Arabic optimization
- Structured intent output
- Low-latency inference

**Usage:**
```java
FunctionGemmaConfig config = FunctionGemmaConfig.defaultConfig();
FunctionGemmaEngine engine = new FunctionGemmaEngine(context, config);
IntentResult result = engine.classify("كلم ماما");
```

### FunctionGemmaConfig
Configuration options for FunctionGemma.

**Properties:**
- Model path
- Context size
- Temperature
- Top-p sampling
- Max tokens

### LlamaEngine
Main engine for running Llama models.

**Features:**
- llama.cpp integration
- CPU-optimized inference
- Quantization support
- Streaming output

**Usage:**
```java
LlamaConfig config = LlamaConfigLoader.load("config.yaml");
LlamaEngine engine = new LlamaEngine(context, config);
String response = engine.generate(prompt);
```

### LlamaConfig
Configuration options for Llama models.

**Properties:**
- Model path (GGUF format)
- Number of threads
- Context length
- Batch size
- Memory mapping

### LlamaNLUClassifier
NLU classifier using Llama models.

**Features:**
- Prompt-based classification
- Egyptian dialect support
- Confidence scoring
- Fallback handling

## Configuration Files

Configuration files are stored in `configs/llm/`:
- `functiongemma.yaml` - FunctionGemma settings
- `llama.yaml` - Llama settings

## Model Formats

| Model | Format | Location |
|-------|--------|----------|
| FunctionGemma | GGUF | `models/functiongemma/` |
| Llama | GGUF | `models/llama/` |

## Performance Optimization

### Memory
- Use quantized models (Q4_K_M, Q5_K_M)
- Enable memory mapping
- Configure appropriate context size

### Speed
- Set optimal thread count
- Use batch processing
- Enable caching

## Related Packages

- **`../ai/`** - AI engine implementations
- **`../nlu/`** - NLU components
- **`../config/`** - Configuration management
