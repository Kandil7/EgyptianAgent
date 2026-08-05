# Executor Package

## Purpose

The `executor` package contains **controller classes** that handle high-level command execution logic. These controllers act as intermediaries between the intent classification system and the actual execution layer.

## Contents

| File | Description |
|------|-------------|
| `CommandExecutor.java` | Main command execution coordinator |
| `ExecutorResult.java` | Result container for execution outcomes |
| `AlarmController.java` | Handles alarm-related commands |
| `AppsController.java` | Handles application control commands |
| `CommunicationController.java` | Handles communication commands (calls, messages) |
| `EmergencyController.java` | Handles emergency-related commands |
| `SettingsController.java` | Handles system settings commands |

## Architecture

```
User Intent → NLU → Executor Controllers → Executor Workers → System
```

Controllers in this package:
- Parse intent results
- Validate command parameters
- Coordinate with appropriate executor workers
- Return execution results

## Related Packages

- **`../executors/`** - Contains the actual worker classes that perform system operations
- **`../nlu/`** - Contains intent classification logic
- **`../core/`** - Contains core agent functionality

## Usage Example

```java
CommandExecutor executor = new CommandExecutor(context);
ExecutorResult result = executor.executeCommand(intentResult);
if (result.isSuccess()) {
    // Handle successful execution
}
```
