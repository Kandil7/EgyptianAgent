# API Reference Documentation

This directory contains API documentation for the EgyptianAgent project.

## Documents

### Core API
- [API Reference](API_REFERENCE.md) - Main API documentation

### FunctionGemma
- [FunctionGemma API Reference](FUNCTIONGEMMA_API_REFERENCE.md) - FunctionGemma-specific API

## API Overview

The EgyptianAgent exposes several internal APIs:

### NLU Manager API
```java
public interface NLUManager {
    IntentResult classifyIntent(String text);
    void trainModel(TrainingData data);
    float getConfidence();
}
```

### ASR Manager API
```java
public interface ASRManager {
    String transcribe(byte[] audioData);
    void setLanguage(String language);
    void setModel(ASRModel model);
}
```

### LLM Engine API
```java
public interface LLMEngine {
    FunctionCallResponse processIntent(Intent intent);
    void loadModel(String modelPath);
    void unloadModel();
}
```

### Command Executor API
```java
public interface CommandExecutor {
    ExecutionResult execute(Command command);
    void registerHandler(String intentType, CommandHandler handler);
}
```

## REST Endpoints (if applicable)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/intent` | POST | Classify user intent |
| `/api/v1/transcribe` | POST | Transcribe audio |
| `/api/v1/execute` | POST | Execute command |

## Related Documentation

- [Architecture](../architecture/ARCHITECTURE.md)
- [Deployment](../deployment/DEPLOYMENT_GUIDE.md)
- [Testing](../testing/TEST_SUITE.md)
