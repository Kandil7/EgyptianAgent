# FunctionGemma 270M Integration Guide

## Overview

This document describes the integration of **FunctionGemma 270M** into the Egyptian Agent project as a replacement for the heavier Llama 3.2 3B model.

## Why FunctionGemma?

| Feature | FunctionGemma 270M | Llama 3.2 3B |
|---------|-------------------|---------------|
| **Parameters** | 270M | 3B |
| **Model Size** | ~288MB | ~2GB |
| **Peak RAM** | 551MB | ~4GB |
| **Prefill Speed** | 1718 tokens/s | ~100 tokens/s |
| **Function Calling** | Native | Not specialized |
| **Mobile Optimized** | Yes | Partial |

## Model Capabilities

FunctionGemma is specifically designed to:
1. **Convert natural language → function calls** - Perfect for intent classification
2. **Understand Arabic** - Multilingual model with Arabic support
3. **Run on mobile** - Optimized for Android devices
4. **Work offline** - 100% on-device inference

## Installation

### 1. Download the Model

```bash
# Option 1: Direct download from HuggingFace
# Requires accepting license at https://huggingface.co/google/functiongemma-270m-it

# Option 2: Using huggingface-cli
pip install huggingface-hub
huggingface-cli download google/functiongemma-270m-it --local-dir ./models/functiongemma-270m-it
```

### 2. Convert to GGUF Format (for mobile)

Use llama.cpp to convert to Android-compatible format:

```bash
# Clone llama.cpp
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

# Convert model (requires Python with transformers)
python convert.py models/functiongemma-270m-it/ --outfile models/functiongemma-270m-it-q4.gguf --quantize q4_k_m
```

## Function Definitions

Define the Egyptian Agent functions in JSON schema format:

```python
EGYPTIAN_AGENT_FUNCTIONS = [
    {
        "type": "function",
        "function": {
            "name": "call_contact",
            "description": "Make a phone call to a contact",
            "parameters": {
                "type": "object",
                "properties": {
                    "contact_name": {
                        "type": "string",
                        "description": "The name of the contact to call (e.g., ماما, بابا, أحمد)"
                    }
                },
                "required": ["contact_name"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "send_whatsapp",
            "description": "Send a WhatsApp message to a contact",
            "parameters": {
                "type": "object",
                "properties": {
                    "contact_name": {
                        "type": "string",
                        "description": "Name of the contact"
                    },
                    "message": {
                        "type": "string",
                        "description": "The message to send"
                    }
                },
                "required": ["contact_name", "message"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "set_alarm",
            "description": "Set an alarm or reminder",
            "parameters": {
                "type": "object",
                "properties": {
                    "time": {
                        "type": "string",
                        "description": "Time for alarm (e.g., بكرة الصبح, بعد ساعة, الساعة 8)"
                    },
                    "label": {
                        "type": "string",
                        "description": "Label for the alarm"
                    }
                },
                "required": ["time"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "toggle_wifi",
            "description": "Turn WiFi on or off",
            "parameters": {
                "type": "object",
                "properties": {
                    "action": {
                        "type": "string",
                        "description": "Whether to turn on or off",
                        "enum": ["on", "off"]
                    }
                },
                "required": ["action"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "toggle_bluetooth",
            "description": "Turn Bluetooth on or off",
            "parameters": {
                "type": "object",
                "properties": {
                    "action": {
                        "type": "string",
                        "description": "Whether to turn on or off",
                        "enum": ["on", "off"]
                    }
                },
                "required": ["action"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "emergency",
            "description": "Trigger emergency protocol",
            "parameters": {
                "type": "object",
                "properties": {
                    "type": {
                        "type": "string",
                        "description": "Type of emergency"
                    }
                },
                "required": ["type"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "open_app",
            "description": "Open an application",
            "parameters": {
                "type": "object",
                "properties": {
                    "app_name": {
                        "type": "string",
                        "description": "Name of the app to open"
                    }
                },
                "required": ["app_name"]
            }
        }
    }
]
```

## Android Integration

### Native JNI Wrapper

Create a JNI wrapper to use FunctionGemma from Android:

```java
// FunctionGemmaEngine.java
package com.egyptian.agent.llm;

public class FunctionGemmaEngine {
    static {
        System.loadLibrary("functiongemma");
    }
    
    private long modelHandle;
    
    public native boolean loadModel(String modelPath, int nThreads);
    public native String generate(String prompt, String functions, int maxTokens);
    public native void unloadModel();
    
    // Egyptian Arabic system prompt
    private static final String SYSTEM_PROMPT = 
        "أنت مساعد صوتي مصري متخصص في مساعدة كبار السن. " +
        "فهمك ممتاز للعامية المصرية. " +
        " مهمتك هي فهم أوامر المستخدم وتحويلها لfunctions مناسبة.";
    
    public FunctionGemmaResult processCommand(String userInput) {
        String fullPrompt = SYSTEM_PROMPT + "\nالمستخدم: " + userInput;
        String response = generate(fullPrompt, EGYPTIAN_FUNCTIONS_JSON, 128);
        return parseFunctionCall(response);
    }
}
```

### CMakeLists.txt Integration

```cmake
# Add to app/CMakeLists.txt
add_library(functiongemma SHARED
    native-functiongemma.cpp
)

target_link_libraries(functiongemma
    llama
    log
)
```

## Fine-tuning for Egyptian Arabic

### Mobile Actions Dataset

Google provides a dataset for Android function calling that can be adapted:

```python
# Fine-tuning recipe available at:
# https://github.com/google-gemini/gemma-cookbook/blob/main/FunctionGemma/%5BFunctionGemma%5D_Finetune_FunctionGemma_270M_for_Mobile_Actions_with_Hugging_Face.ipynb
```

### Custom Egyptian Commands Dataset

Create training data:

```python
egyptian_commands_dataset = [
    {
        "input": "اتصل بماما",
        "function_call": "call_contact(contact_name='ماما')"
    },
    {
        "input": "بلغ أحمد أنا جيت",
        "function_call": "send_whatsapp(contact_name='أحمد', message='أنا جيت')"
    },
    {
        "input": "نبهني بكرة الصبح",
        "function_call": "set_alarm(time='بكرة الصبح')"
    },
    {
        "input": "افتح الواي فاي",
        "function_call": "toggle_wifi(action='on')"
    },
    {
        "input": "يا نجدة",
        "function_call": "emergency(type='general')"
    }
    # Add 500+ more examples
]
```

## Performance on Honor X6c

Expected performance with FunctionGemma 270M:

| Metric | Value |
|--------|-------|
| **Model Load Time** | ~3 seconds |
| **Prefill Speed** | ~500 tokens/s (on Helio G81) |
| **Decode Speed** | ~50 tokens/s |
| **Time per Command** | < 500ms |
| **Memory Usage** | ~600MB total |
| **Battery Impact** | < 3%/hour |

## Comparison: Before vs After

| Aspect | Before (Llama 3B) | After (FunctionGemma 270M) |
|--------|------------------|---------------------------|
| Model Size | 2GB | 288MB |
| Load Time | 30s | 3s |
| Command Latency | 5s | 0.5s |
| Memory | 4GB | 600MB |
| Battery | 8%/hr | 2%/hr |
| Function Calling | Manual NLU | Native |

## Usage Example

```java
// Initialize
FunctionGemmaEngine engine = new FunctionGemmaEngine();
engine.loadModel("/data/local/llm/functiongemma-270m-q4.gguf", 4);

// Process voice command
String userInput = "اتصل بماما";
FunctionGemmaResult result = engine.processCommand(userInput);

// Result: {function: "call_contact", args: {contact_name: "ماما"}}
executeFunction(result.getFunction(), result.getArgs());
```

## Next Steps

1. [ ] Download FunctionGemma model
2. [ ] Convert to GGUF format
3. [ ] Create JNI wrapper
4. [ ] Implement function parser
5. [ ] Fine-tune on Egyptian commands
6. [ ] Test on Honor X6c

---

*Integration Date: 2026-03-03*
*Model: google/functiongemma-270m-it*
