## Negative examples generation prompt

Generate negative examples where the user utterance is a natural Egyptian Arabic request that:
- Is NOT supported by the assistant (e.g., physical-world actions, kitchen tasks).
- Should receive `intent = "none"`.
- Has `negative_example = true`.

Schema:

```json
{
  "id": "neg-XXX",
  "transcript": "...",
  "normalized_text": "...",
  "dialect": "egyptian_arabic",
  "intent": "none",
  "slots": {},
  "execution": {},
  "safety_level": "safe",
  "confirmation_policy": "auto",
  "code_switch": false,
  "negative_example": true
}
```

Examples:
- "افتح الشباك"
- "اعمللي شاي"
- "نضف الأوضة"

Do not generate harmful or explicit content.
