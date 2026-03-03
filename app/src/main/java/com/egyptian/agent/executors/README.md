# Executors Package

## Purpose

The `executors` package contains **worker classes** that perform actual system operations. These are low-level implementation classes that execute specific tasks on the Android system.

## Contents

| File | Description |
|------|-------------|
| `AlarmExecutor.java` | Executes alarm creation/modification |
| `AlarmReceiver.java` | Broadcast receiver for alarm triggers |
| `AppExecutor.java` | Executes app open/close operations |
| `CallExecutor.java` | Executes phone call operations |
| `CallLogExecutor.java` | Reads call log entries |
| `ContactsExecutor.java` | Executes contact operations |
| `EmergencyHandler.java` | Handles emergency situations |
| `EmergencyHandlerInterface.java` | Interface for emergency handling |
| `EmergencyFollowupReceiver.java` | Receiver for emergency follow-ups |
| `EmergencyFollowupWorker.java` | Worker for emergency follow-up tasks |
| `GuardianNotificationSystem.java` | Notifies guardians in emergencies |
| `MediaExecutor.java` | Executes media-related operations |
| `SmsExecutor.java` | Executes SMS operations |
| `SystemSettingsExecutor.java` | Executes system settings changes |
| `TimeExecutor.java` | Executes time-related operations |
| `WhatsAppExecutor.java` | Executes WhatsApp messaging |

## Architecture

Executors are called by controllers in the `../executor/` package:

```
Controller → Executor Worker → Android System API
```

## Key Features

- **Idempotent Operations** - Safe to retry
- **Permission Handling** - Checks required permissions before execution
- **Error Recovery** - Graceful failure handling
- **Background Execution** - Support for WorkManager integration

## Related Packages

- **`../executor/`** - Contains controller classes that coordinate executors
- **`../receivers/`** - Contains broadcast receivers
- **`../service/`** - Contains background services
