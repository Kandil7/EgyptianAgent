# Contributing to Egyptian Agent

**Version:** 2.0.0  
**Last Updated:** 2026-03-03  
**Status:** ✅ Production Ready

Thank you for your interest in contributing to Egyptian Agent! This document provides comprehensive guidelines for contributing to the project.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Environment](#development-environment)
4. [Making Changes](#making-changes)
5. [Testing](#testing)
6. [Pull Request Process](#pull-request-process)
7. [Coding Standards](#coding-standards)
8. [Commit Messages](#commit-messages)
9. [Documentation](#documentation)
10. [Questions](#questions)

---

## Code of Conduct

### Our Pledge

By participating in this project, you agree to maintain a respectful and inclusive environment. We are committed to making contributions from people of all backgrounds and identity welcome.

### Expected Behavior

| Behavior | Description |
|----------|-------------|
| **Respectful** | Be respectful and inclusive in all interactions |
| **Welcoming** | Use welcoming and inclusive language |
| **Constructive** | Accept constructive criticism gracefully |
| **Community-focused** | Focus on what is best for the community |

### Unacceptable Behavior

| Behavior | Description |
|----------|-------------|
| **Harassment** | Harassment of any kind |
| **Discrimination** | Discriminatory language or actions |
| **Personal attacks** | Personal or political attacks |
| **Disruptive conduct** | Behavior that disrupts the project |

### Reporting

Report violations to the project maintainers at [support@egyptianagent.com](mailto:support@egyptianagent.com).

---

## Getting Started

### Prerequisites

| Tool | Version | Installation |
|------|---------|--------------|
| **Java JDK** | 17+ | [Adoptium](https://adoptium.net/) |
| **Android SDK** | 34+ | Android Studio |
| **Gradle** | 8.13+ | Included |
| **Git** | 2.30+ | [git-scm.com](https://git-scm.com/) |
| **Python** | 3.8+ | [python.org](https://www.python.org/) |
| **Android Studio** | Latest | [Download](https://developer.android.com/studio) |

### Clone the Repository

```bash
# Clone the repository
git clone https://github.com/Kandil7/EgyptianAgent.git
cd EgyptianAgent

# Initialize submodules
git submodule update --init --recursive

# Or use our script
./scripts/deploy/initialize_submodules.sh
```

### Build the Project

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# FunctionGemma build (recommended)
./scripts/build/build_functiongemma.sh --release --native
```

### Verify Setup

```bash
# Run verification script
./scripts/utils/verify_implementation.sh

# Run unit tests
./gradlew test

# Run all tests
./scripts/test/run_functiongemma_tests.sh --all
```

---

## Development Environment

### Required Tools

| Tool | Purpose | Installation |
|------|---------|--------------|
| **Android Studio** | Primary IDE | Android Studio |
| **Android SDK** | API 34 (Android 14) | SDK Manager |
| **NDK** | Native code (C++) | SDK Manager → NDK 25.2.9519653 |
| **CMake** | Native build | SDK Manager → CMake 3.18+ |
| **Genymotion / Physical Device** | Testing | Download or use device |

### Recommended IDE Setup

| Setting | Value |
|---------|-------|
| **JDK** | 17 |
| **Gradle JVM** | 17 |
| **Android SDK** | 34 |
| **Build Tools** | 34.0.0 |
| **NDK** | 25.2.9519653 |

### Project Structure

```
EgyptianAgent/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/egyptian/agent/
│       │   │   ├── ai/            # AI engines (FunctionGemma, Llama)
│       │   │   ├── llm/           # LLM integration
│       │   │   ├── nlu/           # Intent classification
│       │   │   ├── executor/      # Command execution
│       │   │   ├── service/       # Android services
│       │   │   ├── accessibility/ # Senior features
│       │   │   └── utils/         # Utilities
│       │   ├── cpp/               # Native C++ code
│       │   └── res/               # Resources
│       ├── test/                  # Unit tests
│       └── androidTest/           # Instrumented tests
├── docs/                          # Documentation
├── scripts/                       # Build & automation scripts
├── datasets/                      # Training datasets
├── configs/                       # Configuration files
└── agents/                        # Agent definitions
```

### Environment Variables

```bash
# Add to ~/.bashrc or ~/.zshrc (Linux/Mac)
# Or set via System Properties (Windows)

export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
export NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653

# Add to PATH
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

---

## Making Changes

### 1. Create a Branch

```bash
# From main branch
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b bugfix/issue-description
```

### Branch Naming Convention

| Type | Format | Example |
|------|--------|---------|
| **Feature** | `feature/description` | `feature/egyptian-dialect-expansion` |
| **Bugfix** | `bugfix/description` | `bugfix/wake-word-detection` |
| **Documentation** | `docs/description` | `docs/update-api-reference` |
| **Refactor** | `refactor/description` | `refactor/intent-engine` |
| **Test** | `test/description` | `test/add-functiongemma-tests` |

### 2. Make Your Changes

- Follow the [coding standards](#coding-standards) below
- Add tests for new functionality
- Update documentation if needed
- Keep commits focused and atomic

### 3. Test Your Changes

```bash
# Run unit tests
./gradlew test

# Run Android tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests="com.egyptian.agent.nlu.*"

# Run FunctionGemma tests
./scripts/test/run_functiongemma_tests.sh --all

# Generate coverage report
./gradlew jacocoTestReport
```

---

## Testing

### Test Categories

| Category | Location | Tools | Coverage Target |
|----------|----------|-------|-----------------|
| **Unit Tests** | `app/src/test/` | JUnit, Mockito | 90%+ |
| **Integration Tests** | `app/src/androidTest/` | Espresso | 95%+ |
| **Egyptian Dialect Tests** | `datasets/` | Custom suite | 100% commands |
| **Performance Tests** | `scripts/test/` | Android Profiler | Response time |
| **Battery Tests** | `scripts/test/` | Battery Historian | Drain rate |

### Writing Unit Tests

```java
@RunWith(JUnit4.class)
public class FunctionGemmaIntentEngineTest {

    private FunctionGemmaIntentEngine engine;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        engine = new FunctionGemmaIntentEngine(context);
    }

    @Test
    public void testCallContact_Mama() {
        // Arrange
        String command = "اتصل بماما";

        // Act
        IntentResult result = engine.classifyIntent(command);

        // Assert
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        assertEquals("ماما", result.getEntity("contact_name"));
        assertTrue("Confidence should be >= 0.85", result.getConfidence() >= 0.85f);
    }

    @Test
    public void testSendWhatsApp() {
        // Arrange
        String command = "ابعت واتساب لأحمد وقوله إنى هتأخر";

        // Act
        IntentResult result = engine.classifyIntent(command);

        // Assert
        assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
        assertEquals("أحمد", result.getEntity("contact_name"));
        assertEquals("إنى هتأخر", result.getEntity("message"));
    }

    @After
    public void tearDown() {
        if (engine != null) {
            engine.destroy();
        }
    }
}
```

### Test Coverage Goals

| Component | Target | Current |
|-----------|--------|---------|
| **Core Services** | 95%+ | ✅ 96% |
| **Intent Engines** | 90%+ | ✅ 92% |
| **Executors** | 85%+ | ✅ 88% |
| **Utilities** | 80%+ | ✅ 85% |
| **Overall** | 90%+ | ✅ 91% |

---

## Pull Request Process

### Before Submitting

```bash
# 1. Run all tests
./gradlew test connectedAndroidTest

# 2. Verify build
./gradlew assembleDebug

# 3. Check code style
./gradlew lint

# 4. Check formatting
./gradlew spotlessCheck

# 5. Generate coverage
./gradlew jacocoTestReport
```

### Pre-Submission Checklist

- [ ] All tests passing
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] Tests added for new features
- [ ] Commit messages are clear
- [ ] Branch is up to date with main

### Submitting

1. **Push your branch:**
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create Pull Request on GitHub:**
   - Go to [EgyptianAgent Pull Requests](https://github.com/Kandil7/EgyptianAgent/pulls)
   - Click "New Pull Request"
   - Select your branch

3. **Fill out the PR template:**
   - Description of changes
   - Related issues (e.g., "Closes #123")
   - Testing performed
   - Screenshots (if UI changes)

4. **Wait for review** (typically 24-48 hours)

### PR Template

```markdown
## Description
Brief description of changes

## Related Issues
Closes #123

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings
```

### Review Process

| Stage | Description | Time |
|-------|-------------|------|
| **Automated Checks** | CI/CD validation | 5-10 min |
| **Code Review** | Maintainer review | 24-48 hours |
| **Address Feedback** | Make requested changes | As needed |
| **Final Approval** | Merge approval | 24 hours |
| **Merge** | Integration to main | After approval |

---

## Coding Standards

### Java/Kotlin

| Standard | Requirement |
|----------|-------------|
| **Language** | Java 8+ or Kotlin |
| **Line Length** | Max 100 characters |
| **Indentation** | 4 spaces (no tabs) |
| **Naming - Classes** | `CamelCase` |
| **Naming - Methods** | `camelCase` |
| **Naming - Constants** | `UPPER_SNAKE_CASE` |
| **Naming - Variables** | `camelCase` |

### Example Java Code

```java
/**
 * FunctionGemma intent engine for Egyptian Arabic.
 * Provides intent classification with entity extraction.
 */
public class FunctionGemmaIntentEngine implements IntentEngine {

    private static final String TAG = "FunctionGemmaEngine";
    private static final float CONFIDENCE_THRESHOLD = 0.85f;

    private final Context context;
    private final FunctionGemmaConfig config;

    /**
     * Creates a new FunctionGemma intent engine.
     *
     * @param context Application context
     */
    public FunctionGemmaIntentEngine(Context context) {
        this.context = context.getApplicationContext();
        this.config = FunctionGemmaConfig.getDefault();
        initialize();
    }

    @Override
    public IntentResult classifyIntent(String text) {
        if (text == null || text.isEmpty()) {
            return IntentResult.unknown("Empty input");
        }

        // Implementation
    }
}
```

### Android Specific

| Standard | Requirement |
|----------|-------------|
| **Libraries** | Use AndroidX libraries |
| **Design** | Follow Material Design guidelines |
| **RTL** | Support Arabic RTL layout |
| **Accessibility** | Include content descriptions |
| **Permissions** | Request minimal permissions |

### Error Handling

```java
// Always log errors with context
Log.e(TAG, "Failed to load model: " + modelPath, e);

// Return meaningful error messages
return IntentResult.error("Model not loaded", e);

// Never crash silently
try {
    // Risky operation
} catch (Exception e) {
    Log.e(TAG, "Operation failed", e);
    // Handle gracefully
}
```

### Code Review Guidelines

| Aspect | Check |
|--------|-------|
| **Correctness** | Does the code work correctly? |
| **Readability** | Is the code easy to understand? |
| **Maintainability** | Is the code easy to modify? |
| **Performance** | Are there performance concerns? |
| **Security** | Are there security issues? |
| **Testing** | Is the code adequately tested? |

---

## Commit Messages

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(nlu): Add Egyptian dialect support` |
| `fix` | Bug fix | `fix(wake-word): Reduce false positives` |
| `docs` | Documentation | `docs(readme): Update quick start` |
| `style` | Code style | `style(format): Fix indentation` |
| `refactor` | Code refactoring | `refactor(engine): Simplify intent routing` |
| `test` | Tests | `test(nlu): Add unit tests for FunctionGemma` |
| `chore` | Build/process | `chore(deps): Update Gradle version` |

### Examples

```
feat(nlu): Add fuzzy matching for Arabic contact names

- Implemented Levenshtein distance for similarity matching
- Added support for Egyptian family term aliases
- Added diacritics normalization

Closes #123

---

fix(wake-word): Reduce false positive rate

- Adjusted sensitivity threshold from 0.7 to 0.8
- Added noise floor detection
- Implemented debounce logic

Fixes #456

---

docs(readme): Update performance metrics

- Added FunctionGemma benchmarks
- Updated comparison table
- Added performance scorecard

See PR #789
```

### Commit Checklist

- [ ] Subject line < 72 characters
- [ ] Use imperative mood in subject
- [ ] Body explains what and why (not how)
- [ ] Reference issues/PRs
- [ ] No trailing whitespace

---

## Documentation

### Documentation Standards

| Document | Location | Format |
|----------|----------|--------|
| **API Reference** | `docs/api/` | Markdown |
| **User Guides** | `docs/guides/` | Markdown |
| **Architecture** | `docs/architecture/` | Markdown + Diagrams |
| **Deployment** | `docs/deployment/` | Markdown |
| **Testing** | `docs/testing/` | Markdown |

### Writing Guidelines

| Aspect | Requirement |
|--------|-------------|
| **Clarity** | Use clear, concise language |
| **Structure** | Use headers, lists, tables |
| **Examples** | Include code examples |
| **Links** | Add cross-references |
| **Version** | Include version/date |

### Documentation Checklist

- [ ] Table of contents (for long docs)
- [ ] Clear section headers
- [ ] Code examples where applicable
- [ ] Links to related documents
- [ ] Version and date
- [ ] Review status

---

## Questions?

### Getting Help

| Channel | Purpose | Response Time |
|---------|---------|---------------|
| **GitHub Issues** | Bugs, feature requests | 24-48 hours |
| **GitHub Discussions** | Questions, discussions | 24-48 hours |
| **Email** | Private inquiries | 48 hours |
| **Documentation** | Self-help | Immediate |

### Resources

- [Project Documentation](docs/)
- [Architecture Overview](docs/architecture/ARCHITECTURE.md)
- [API Reference](docs/api/API_REFERENCE.md)
- [Troubleshooting](docs/guides/TROUBLESHOOTING.md)

---

**Last Updated:** 2026-03-03  
**Version:** 2.0.0  
**Maintained By:** EgyptianAgent Team
