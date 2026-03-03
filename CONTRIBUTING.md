# Contributing to Egyptian Agent

Thank you for your interest in contributing to Egyptian Agent! This document provides guidelines for contributing to the project.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Environment](#development-environment)
4. [Making Changes](#making-changes)
5. [Testing](#testing)
6. [Pull Request Process](#pull-request-process)
7. [Coding Standards](#coding-standards)
8. [Commit Messages](#commit-messages)

---

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment. We are committed to making contributions from people of all backgrounds and identity.

**Expected Behavior:**
- Be respectful and inclusive
- Use welcoming and inclusive language
- Accept constructive criticism gracefully
- Focus on what is best for the community

**Unacceptable Behavior:**
- Harassment of any kind
- Discriminatory language or actions
- Personal or political attacks

---

## Getting Started

### Prerequisites

- Java JDK 17+
- Android SDK 34+
- Gradle 8.13+
- Git
- Android Studio (recommended)

### Clone the Repository

```bash
git clone https://github.com/Kandil7/EgyptianAgent.git
cd EgyptianAgent
git submodule update --init --recursive
```

### Build the Project

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

---

## Development Environment

### Required Tools

1. **Android Studio** - Recommended IDE
2. **Android SDK** - API 34 (Android 14)
3. **NDK** - For native code (C++)
4. **Genymotion** or physical device for testing

### Project Structure

```
EgyptianAgent/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/egyptian/agent/
│       │   │   ├── core/          # Core engines
│       │   │   ├── nlu/            # Intent classification
│       │   │   ├── executor/       # Command execution
│       │   │   ├── service/        # Android services
│       │   │   ├── accessibility/  # Senior features
│       │   │   ├── utils/          # Utilities
│       │   │   └── security/       # Security
│       │   ├── res/                # Resources
│       │   └── cpp/                # Native code
│       ├── test/                   # Unit tests
│       └── androidTest/            # Android tests
├── documentation/                  # Docs
└── scripts/                        # Build scripts
```

---

## Making Changes

### 1. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout - bugfix/issue-description
```

### 2. Make Your Changes

- Follow the coding standards below
- Add tests for new functionality
- Update documentation if needed

### 3. Test Your Changes

```bash
# Run unit tests
./gradlew test

# Run Android tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests="com.egyptian.agent.nlu.*"
```

---

## Testing

### Unit Tests

Create unit tests in `app/src/test/java/`:

```java
@RunWith(JUnit4.class)
public class MyClassTest {
    
    @Test
    public void testMethod() {
        // Arrange
        MyClass instance = new MyClass();
        
        // Act
        Result result = instance.method();
        
        // Assert
        assertNotNull(result);
    }
}
```

### Integration Tests

Create integration tests in `app/src/androidTest/java/`:

```java
@RunWith(AndroidJUnit4.class)
public class MyIntegrationTest {
    
    @Test
    public void testFeature() {
        // Test implementation
    }
}
```

### Test Coverage

We target:
- **Unit tests**: 80%+ coverage
- **Critical paths**: 100% coverage

Run coverage report:
```bash
./gradlew jacocoTestReport
```

---

## Pull Request Process

### Before Submitting

1. **Run all tests:**
   ```bash
   ./gradlew test connectedAndroidTest
   ```

2. **Verify build:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Check code style:**
   ```bash
   ./gradlew lint
   ```

### Submitting

1. Push your branch:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Create Pull Request on GitHub

3. Fill out the PR template:
   - Description of changes
   - Related issues
   - Testing performed

4. Wait for review (typically 24-48 hours)

### Review Process

- Code review by at least one maintainer
- Address feedback promptly
- Tests must pass before merge

---

## Coding Standards

### Java/Kotlin

- **Language**: Java 8+ or Kotlin
- **Line length**: Max 100 characters
- **Indentation**: 4 spaces (no tabs)
- **Naming**: 
  - Classes: `CamelCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`

### Example

```java
public class MyClass {
    
    private static final String TAG = "MyClass";
    
    public void myMethod(String parameter) {
        if (parameter == null) {
            return;
        }
        
        // Implementation
    }
}
```

### Android Specific

- Use AndroidX libraries
- Follow Material Design guidelines
- Support Arabic RTL layout
- Include accessibility content descriptions

### Error Handling

- Always log errors with context
- Return meaningful error messages
- Never crash silently

---

## Commit Messages

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style
- `refactor`: Code refactoring
- `test`: Tests
- `chore`: Build/process

### Example

```
feat(contacts): Add fuzzy matching for Arabic contact names

- Implemented Levenshtein distance for similarity matching
- Added support for Egyptian family term aliases
- Added diacritics normalization

Closes #123
```

---

## Questions?

- Open an issue for bugs or feature requests
- Join our Discord community (link in README)
- Email: contact@egyptianagent.com

---

*Last Updated: 2026-03-03*
*Version: 1.1.0*
