## Intent generation prompt

You will generate synthetic Egyptian Arabic command samples.

Given an intent name, a brief description, and a slot schema, produce N JSON objects matching this structure:

```json
{
  "id": "cmd-XXX",
  "transcript": "...",
  "normalized_text": "...",
  "dialect": "egyptian_arabic",
  "intent": "<intent_name>",
  "slots": { /* slot values */ },
  "execution": {
    "tool": "<tool_name>",
    "args": { /* tool args */ }
  },
  "safety_level": "safe | reversible | user_confirm | high_risk",
  "confirmation_policy": "auto | preview_then_confirm | voice_confirm | never_auto",
  "code_switch": false,
  "negative_example": false
}
```

### Constraints

- Use natural Egyptian Arabic for `transcript`.
- `normalized_text` is a lightly normalized version (e.g., unify spellings, remove elongations).
- `slots` must be consistent with the utterance.
- `execution.tool` and `execution.args` must be executable on Android.
- `safety_level` and `confirmation_policy` must reflect the risk (e.g., messages → `user_confirm`).
- No extra fields beyond the schema.

### Example (open_app – WhatsApp)

- intent: `open_app`
- description: "open a specific app on the phone"
- slots: `app_name`, `package`
- execution: tool `launch_app`, args `{ "package": "com.whatsapp" }`

Example output object:

```json
{
  "id": "cmd-openapp-001",
  "transcript": "افتح واتساب",
  "normalized_text": "افتح واتساب",
  "dialect": "egyptian_arabic",
  "intent": "open_app",
  "slots": {
    "app_name": "واتساب",
    "package": "com.whatsapp"
  },
  "execution": {
    "tool": "launch_app",
    "args": {
      "package": "com.whatsapp"
    }
  },
  "safety_level": "safe",
  "confirmation_policy": "auto",
  "code_switch": false,
  "negative_example": false
}
```
