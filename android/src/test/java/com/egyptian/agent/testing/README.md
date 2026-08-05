# Testing Package

## Purpose

The `testing` package contains **automated test infrastructure** classes. These provide the foundation for running automated tests and validation within the application.

## Contents

| File | Description |
|------|-------------|
| `AutomatedTestSuite.java` | Automated test suite runner |

## Features

- Automated test execution
- Test result reporting
- Integration with build system
- Continuous validation

## Usage

```java
// Run automated tests
AutomatedTestSuite.run(context);
```

## Related Packages

- **`../test/`** - Contains manual test suite classes
- **`../../../test/`** - Contains JUnit test classes
- **`../../../androidTest/`** - Contains instrumented Android tests
