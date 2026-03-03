# Testing Documentation

This directory contains testing documentation for the EgyptianAgent project.

## Documents

### Test Documentation
- [Test Suite](TEST_SUITE.md) - Comprehensive test documentation
- [FunctionGemma Test Plan](FUNCTIONGEMMA_TEST_PLAN.md) - FunctionGemma testing strategy

## Test Structure

```
app/src/test/
├── java/com/egyptian/agent/
│   ├── ai/           # AI/ML component tests
│   ├── llm/          # LLM component tests
│   ├── nlu/          # NLU component tests
│   ├── asr/          # ASR component tests
│   ├── tts/          # TTS component tests
│   ├── executor/     # Command executor tests
│   ├── integration/  # Integration tests
│   ├── security/     # Security tests
│   ├── benchmark/    # Performance tests
│   └── system/       # System tests
└── resources/
    └── egyptian_test_commands.json
```

## Running Tests

### All Tests
```bash
./scripts/test/run_functiongemma_tests.sh
```

### Integration Tests
```bash
./scripts/test/test_integration.sh
```

### Specific Test Categories
```bash
# Unit tests
./gradlew testDebugUnitTest

# Integration tests
./gradlew connectedAndroidTest

# Egyptian dialect tests
./scripts/test/run_functiongemma_tests.sh --egyptian
```

## Test Coverage Goals

| Component | Target | Current |
|-----------|--------|---------|
| NLU | 90% | 87% |
| ASR | 85% | 82% |
| LLM | 80% | 78% |
| Executor | 95% | 93% |
| Integration | 75% | 70% |

## Egyptian Dialect Testing

Test data is stored in `app/src/test/resources/egyptian_test_commands.json`

### Test Categories
1. **Basic Commands** - Simple voice commands
2. **Egyptian Expressions** - Culturally specific phrases
3. **Edge Cases** - Ambiguous or complex commands
4. **Senior Voice Patterns** - Elderly voice characteristics

## Related Documentation

- [Architecture](../architecture/ARCHITECTURE.md)
- [API Reference](../api/API_REFERENCE.md)
- [Performance](../performance/FUNCTIONGEMMA_PERFORMANCE_BENCHMARKS.md)
