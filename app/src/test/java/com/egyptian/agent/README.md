# Test Package - Overview

## Overview

This package contains **unit tests** for the Egyptian Agent application. Tests are organized by component to mirror the main source structure.

## Directory Structure

```
app/src/test/java/com/egyptian/agent/
│
├── ai/           → AI engine tests
├── asr/          → ASR tests
├── executor/     → Executor tests
├── integration/  → Integration tests
├── llm/          → LLM engine tests
├── nlu/          → NLU tests
├── security/     → Security tests
├── system/       → System tests
├── test/         → Test utilities
├── tts/          → TTS tests
├── accessibility/→ Accessibility tests
├── contacts/     → Contact tests
├── benchmark/    → Performance benchmarks
│
├── CallExecutorTest.java
├── EgyptianNormalizerTest.java
├── IntentRouterTest.java
├── SeniorModeTest.java
└── TTSManagerTest.java
```

## Test Categories

### Unit Tests
Test individual components in isolation.

**Examples:**
- `EgyptianNormalizerTest` - Tests dialect normalization
- `IntentRouterTest` - Tests intent routing logic

### Integration Tests
Test component interactions.

**Location:** `integration/`

**Examples:**
- End-to-end voice command processing
- Database operations
- API integrations

### Performance Tests
Test performance characteristics.

**Location:** `benchmark/`

**Examples:**
- Inference latency
- Memory usage
- Battery impact

## Running Tests

### Gradle Commands
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.egyptian.agent.EgyptianNormalizerTest"

# Run with coverage
./gradlew test jacocoTestReport

# Run integration tests
./gradlew testIntegration
```

### Test Scripts
```bash
# Run all tests
./scripts/test/run_functiongemma_tests.sh --all

# Run with coverage
./scripts/test/run_functiongemma_tests.sh --coverage

# Run specific category
./scripts/test/run_functiongemma_tests.sh --unit
./scripts/test/run_functiongemma_tests.sh --integration
```

## Test Coverage Targets

| Component | Target | Current |
|-----------|--------|---------|
| AI/ML | 90% | - |
| NLU | 95% | - |
| Executors | 85% | - |
| Core | 90% | - |
| Overall | 85% | - |

## Writing Tests

### Test Class Template
```java
package com.egyptian.agent.component;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ComponentTest {
    
    private Component component;
    
    @Before
    public void setUp() {
        component = new Component();
    }
    
    @Test
    public void testFeature() {
        // Arrange
        String input = "test input";
        
        // Act
        String result = component.process(input);
        
        // Assert
        assertEquals("expected", result);
    }
}
```

### Test Resources
Place test data files in:
- `src/test/resources/` - General test resources
- `src/test/resources/assets/` - Asset files
- `src/test/resources/models/` - Test models

## CI/CD Integration

Tests are automatically run on:
- Pull requests
- Main branch commits
- Nightly builds

## Related Documentation

- [Testing Strategy](../../../../../docs/testing/TEST_STRATEGY.md)
- [Test Plan](../../../../../docs/testing/TEST_PLAN.md)
- [Main Package README](../../../../main/java/com/egyptian/agent/README.md)
