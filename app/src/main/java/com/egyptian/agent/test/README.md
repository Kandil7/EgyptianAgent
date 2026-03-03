# Test Package

## Purpose

The `test` package contains **test suite classes** for validating the Egyptian Agent functionality. These are in-source test utilities that can be run to verify system behavior.

## Contents

| File | Description |
|------|-------------|
| `EgyptianAgentTester.java` | Main test runner for the agent |
| `EgyptianDialectTestSuite.java` | Test suite for Egyptian dialect processing |

## Usage

These test classes can be invoked to validate:
- Intent classification accuracy
- Egyptian dialect normalization
- Command execution correctness
- End-to-end agent functionality

## Running Tests

```java
// Run the main test suite
EgyptianAgentTester.runAllTests(context);

// Run dialect-specific tests
EgyptianDialectTestSuite.runTests();
```

## Related Packages

- **`../testing/`** - Contains automated test infrastructure
- **`../../../test/`** - Contains JUnit test classes (standard Android test location)

## Test Coverage

- Egyptian dialect recognition
- Intent classification
- Entity extraction
- Command execution
- Error handling
