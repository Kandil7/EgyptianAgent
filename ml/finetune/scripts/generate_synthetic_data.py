"""Generate synthetic Egyptian command samples using an LLM (v2).

- jsonschema validation is mandatory.
- Semantic post-checks: intent/tool coherence, safety/policy coherence, negatives.
- Dedup on (intent, normalized_text).
- Pluggable LLM; --provider local_http targets any OpenAI-compatible endpoint
  (LM Studio / Ollama / vLLM) so generation can stay 100% offline.
- Retries intent batches with a corrective hint on invalid JSON.

Post-reorg layout: data ml/finetune/data/egyptian_commands, prompts ml/prompts.
Usage: python generate_synthetic_data.py --provider local_http --model your-model
"""

import argparse
import json
import logging
import os
import random
import sys
import urllib.request
from pathlib import Path
from typing import Callable, List, Optional

try:
    import jsonschema
    from jsonschema import Draft202012Validator
except ImportError:
    raise RuntimeError("jsonschema is a mandatory dependency. pip install jsonschema")

logger = logging.getLogger("egyptian_commands_pipeline")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-7s %(message)s")

SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_DATA_DIR = Path(os.getenv(
    "PIPE_DATA_DIR",
    str(SCRIPT_DIR.parent / "data" / "egyptian_commands"),
))
DEFAULT_PROMPTS_DIR = Path(os.getenv("PIPE_PROMPTS_DIR")) if os.getenv("PIPE_PROMPTS_DIR") \
    else DEFAULT_DATA_DIR.parents[2] / "prompts"

# ---------------------------------------------------------------------------
# LLM client (pluggable)
# ---------------------------------------------------------------------------


def call_llm_stub(prompt, system_prompt=""):
    raise NotImplementedError(
        "No LLM provider configured. Implement call_llm or pass "
        "--provider local_http (set LLM_URL / LLM_API_KEY if required)."
    )


def call_llm_local_http(prompt, system_prompt="", model=""):
    url = os.getenv("LLM_URL", "http://127.0.0.1:8000/v1/chat/completions")
    api_key = os.getenv("LLM_API_KEY", "not-needed")
    model_name = model or os.getenv("LLM_MODEL", "local-model")
    messages = ([{"role": "system", "content": system_prompt}] if system_prompt else []) + [
        {"role": "user", "content": prompt}
    ]
    payload = {
        "model": model_name,
        "messages": messages,
        "temperature": 0.7,
        "max_tokens": 4096,
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json",
                 "Authorization": "Bearer %s" % api_key},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    return body["choices"][0]["message"]["content"]


def build_llm_client(provider, model):
    if provider == "local_http":
        return lambda p, s="": call_llm_local_http(p, s, model=model)
    if provider == "stub":
        return call_llm_stub
    raise ValueError("Unknown provider %r. Use 'stub' or 'local_http'." % provider)


def call_with_retry(call_llm, prompt, system_prompt, *, max_retries=3,
                    corrective_hint="") -> List[dict]:
    last_error = None
    for _ in range(1, max_retries + 1):
        try:
            response = call_llm(prompt, system_prompt)
            data = json.loads(response)
            if not isinstance(data, list):
                raise ValueError("Expected a JSON array of objects from LLM")
            return data
        except Exception as exc:
            last_error = exc
            logger.warning("LLM attempt failed: %s", exc)
            if corrective_hint:
                prompt = "%s\n\nPrevious error: %s\n\n%s" % (corrective_hint, exc, prompt)
    raise RuntimeError("LLM failed after %d attempts: %s" % (max_retries, last_error))


# ---------------------------------------------------------------------------
# Loaders / validators
# ---------------------------------------------------------------------------


def load_json(path):
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def validate_sample(sample, validator) -> List[str]:
    return [
        "%s: %s" % (".".join(str(p) for p in e.path), e.message)
        for e in sorted(validator.iter_errors(sample), key=lambda e: str(list(e.path)))
    ]


# Intent/tool coherence. NOTE (app alignment): set_timer/create_note/open_gallery/
# open_calendar/open_settings have NO live FunctionGemma function yet (see
# HybridOrchestrator.FAST_PATH_INTENTS + FunctionGemmaConfig). Keep while extending
# the app, or drop these to stay strictly app-aligned.
INTENT_TOOL = {
    "open_app": "launch_app", "open_settings": "open_settings",
    "toggle_wifi": "toggle_wifi", "toggle_bluetooth": "toggle_bluetooth",
    "set_brightness": "adjust_brightness", "volume_up": "adjust_volume",
    "volume_down": "adjust_volume", "set_alarm": "set_alarm",
    "set_timer": "set_timer", "call_contact": "call_contact",
    "send_message_draft": "send_message", "create_note": "create_note",
    "toggle_flashlight": "toggle_flashlight", "open_gallery": "open_gallery",
    "open_calendar": "open_calendar",
}

SAFETY_RULES = {
    "call_contact": {("user_confirm", "voice_confirm")},
    "send_message_draft": {("user_confirm", "preview_then_confirm")},
}


def semantic_checks(sample) -> List[str]:
    errors = []
    intent = sample.get("intent")
    negative = bool(sample.get("negative_example"))
    execution = sample.get("execution") or {}
    tool = execution.get("tool")
    slots = sample.get("slots") or {}
    safety = sample.get("safety_level")
    policy = sample.get("confirmation_policy")

    if negative:
        if intent != "none":
            errors.append("negative_example=true but intent != 'none'")
        if slots:
            errors.append("negative_example=true but slots is not empty")
        if tool is not None:
            errors.append("negative_example=true but execution.tool is set")
    else:
        if intent == "none":
            errors.append("negative_example=false but intent == 'none'")
        if not tool:
            errors.append("positive example missing execution.tool")

    if intent in INTENT_TOOL and tool is not None and INTENT_TOOL[intent] != tool:
        errors.append("intent/tool mismatch: %r expects %r, got %r"
                      % (intent, INTENT_TOOL[intent], tool))
    if intent in SAFETY_RULES and (safety, policy) not in SAFETY_RULES[intent]:
        errors.append("safety/policy mismatch for %r: %r %r not in %s"
                      % (intent, safety, policy, sorted(SAFETY_RULES[intent])))
    if safety == "high_risk" and policy != "never_auto":
        errors.append("high_risk commands must use confirmation_policy=never_auto")
    if "duration_minutes" in slots and not isinstance(slots["duration_minutes"], int):
        errors.append("slot duration_minutes must be an integer")
    if "target_state" in slots and slots.get("target_state") not in ("on", "off"):
        errors.append("slot target_state must be 'on' or 'off'")
    return errors


def deduplicate(samples):
    seen = set()
    kept = []
    dropped = 0
    for s in samples:
        norm = (s.get("normalized_text") or "").strip()
        key = "%s\u241f%s" % (s.get("intent") or "", norm)
        if key in seen:
            dropped += 1
            continue
        seen.add(key)
        kept.append(s)
    if dropped:
        logger.info("dedup: dropped %d duplicate sample(s)", dropped)
    return kept


# ---------------------------------------------------------------------------
# Generation
# ---------------------------------------------------------------------------

INTENTS = [
    ("open_app", "Open specific apps like camera, gallery, WhatsApp, Facebook.",
     {"app_name": "string", "package": "string"}),
    ("toggle_wifi", "Turn WiFi on or off.", {"target_state": "on/off"}),
    ("toggle_bluetooth", "Turn Bluetooth on or off.", {"target_state": "on/off"}),
    ("set_brightness", "Increase or decrease screen brightness.", {"direction": "up/down"}),
    ("volume_up", "Increase media/ring volume.", {}),
    ("volume_down", "Decrease media/ring volume.", {}),
    ("set_alarm", "Set alarms for specific times (e.g., tomorrow morning).",
     {"time": "HH:MM", "period": "morning/evening"}),
    ("set_timer", "Set timers for durations in minutes.", {"duration_minutes": "integer"}),
    ("call_contact", "Call a contact using aliases like ماما, بابا, أحمد.",
     {"contact_alias": "string"}),
    ("send_message_draft", "Draft a message to a contact (WhatsApp/SMS) to confirm before sending.",
     {"contact_alias": "string", "message_text": "string"}),
    ("create_note", "Create simple textual notes.", {"note_text": "string"}),
    ("toggle_flashlight", "Turn flashlight on or off.", {"target_state": "on/off"}),
    ("open_gallery", "Open the device gallery.", {}),
    ("open_calendar", "Open the device calendar.", {}),
]

METADATA_HINT = (
    "Optionally add requires_ui_navigation (boolean) and a metadata object "
    "(input_modality: voice|text, mode: standard|senior, device_profile: low|mid|high, "
    "preferred_path: fast|slow|either, source: synthetic|real|edited)."
)


def build_intent_prompt(name, desc, slot_schema, n):
    return f"""You must generate {n} JSON objects for intent `{name}`.

Description:
{desc}

Slot schema (for guidance):
{json.dumps(slot_schema, ensure_ascii=False, indent=2)}

Constraints:
- transcript must be natural Egyptian Arabic (عامية مصرية).
- normalized_text is a lightly normalized variant.
- slots must be consistent with the utterance.
- execution.tool must be `{INTENT_TOOL.get(name, '<tool>')}` and execution.args must be executable on Android.
- safety_level and confirmation_policy must match the risk of the action.
- {METADATA_HINT}
- Return only a JSON array of objects, no commentary.
"""


def main():
    ap = argparse.ArgumentParser(description="Generate Egyptian command samples.")
    ap.add_argument("--provider", default="stub", help="stub | local_http")
    ap.add_argument("--model", default="", help="model name for local_http")
    ap.add_argument("--samples-per-intent", type=int, default=20)
    ap.add_argument("--negative-count", type=int, default=20)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--max-retries", type=int, default=3)
    ap.add_argument("--fail-fast", action="store_true",
                    help="Abort on first invalid sample instead of logging/skipping.")
    ap.add_argument("--data-dir", type=Path, default=DEFAULT_DATA_DIR)
    ap.add_argument("--prompts-dir", type=Path, default=DEFAULT_PROMPTS_DIR)
    ap.add_argument("--out", type=Path,
                    default=DEFAULT_DATA_DIR / "egyptian_commands_synthetic_v2.json")
    args = ap.parse_args()

    random.seed(args.seed)
    call_llm = build_llm_client(args.provider, args.model)

    schema_path = args.data_dir / "egyptian_commands_schema_v1.schema.json"
    base_path = args.data_dir / "egyptian_commands_schema_v1_extended.json"
    if not schema_path.exists():
        raise SystemExit("Schema not found: %s" % schema_path)

    schema = load_json(schema_path)
    validator = Draft202012Validator(schema)
    base = load_json(base_path) if base_path.exists() else []
    logger.info("Re-validating %d base samples against current schema...", len(base))

    stats = {"generated": 0, "structural_fail": 0, "semantic_fail": 0, "skipped": 0}

    def accept(sample, source):
        errs = validate_sample(sample, validator)
        if errs:
            stats["structural_fail"] += 1
            logger.warning("[%s] structural failure (%s): %s",
                           source, sample.get("id"), " | ".join(errs[:3]))
            if args.fail_fast:
                raise SystemExit("Fail-fast invalid sample: %s" % sample.get("id"))
            return False
        sem = semantic_checks(sample)
        if sem:
            stats["semantic_fail"] += 1
            logger.warning("[%s] semantic failure (%s): %s",
                           source, sample.get("id"), "; ".join(sem))
            if args.fail_fast:
                raise SystemExit("Fail-fast semantically invalid sample: %s" % sample.get("id"))
            return False
        stats["generated"] += 1
        return True

    kept_base = [s for s in base if accept(s, "base")]
    all_samples = list(kept_base)

    system_prompt = ""
    sp_path = args.prompts_dir / "system_prompt.txt"
    if sp_path.exists():
        system_prompt = sp_path.read_text(encoding="utf-8")

    neg_template = ""
    for name in ("negative_examples_prompt.md", "negative_examples.md"):
        cand = args.prompts_dir / name
        if cand.exists():
            neg_template = cand.read_text(encoding="utf-8")
            break

    for name, desc, slots in INTENTS:
        logger.info("Generating samples for intent: %s", name)
        prompt = build_intent_prompt(name, desc, slots, args.samples_per_intent)
        corrective = ("Your previous reply was not a valid JSON array matching the schema. "
                      "Return ONLY a JSON array with exact field names/types.")
        try:
            samples = call_with_retry(call_llm, prompt, system_prompt,
                                      max_retries=args.max_retries,
                                      corrective_hint=corrective)
        except RuntimeError as exc:
            logger.error("Skipping intent %s: %s", name, exc)
            stats["skipped"] += 1
            continue
        for s in samples:
            if isinstance(s, dict) and "id" not in s:
                s["id"] = "cmd-%s-%04d" % (name, random.randrange(1000, 9999))
            if accept(s, name):
                all_samples.append(s)

    logger.info("Generating %d negative examples (intent=none)...", args.negative_count)
    if neg_template:
        neg_prompt = (
            "%s\n\nGenerate %d JSON objects where the utterance is a natural Egyptian Arabic "
            "request NOT supported by the assistant. Return only a JSON array in the "
            "exact shape: {\"id\":\"neg-XXX\",\"transcript\":\"...\",\"normalized_text\":\"...\","
            "\"dialect\":\"egyptian_arabic\",\"intent\":\"none\",\"slots\":{},\"execution\":{},\""
            "safety_level\":\"safe\",\"confirmation_policy\":\"auto\",\"code_switch\":false,\""
            "negative_example\":true}"
            % (neg_template, args.negative_count)
        )
        try:
            neg_samples = call_with_retry(call_llm, neg_prompt, "",
                                          max_retries=args.max_retries,
                                          corrective_hint="Return ONLY a JSON array.")
            for s in neg_samples:
                if isinstance(s, dict) and "id" not in s:
                    s["id"] = "neg-%04d" % random.randrange(1000, 9999)
                if accept(s, "negative"):
                    all_samples.append(s)
        except RuntimeError as exc:
            logger.error("Negative sample generation failed: %s", exc)
    else:
        logger.warning("negative examples prompt not found; skipping negatives")

    all_samples = deduplicate(all_samples)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(all_samples, ensure_ascii=False, indent=2),
                        encoding="utf-8")
    logger.info("Wrote %d samples to %s (base=%d, generated=%d, structural_fail=%d, "
                "semantic_fail=%d, skipped_intents=%d)",
                len(all_samples), args.out, len(kept_base), stats["generated"],
                stats["structural_fail"], stats["semantic_fail"], stats["skipped"])


if __name__ == "__main__":
    sys.exit(main())