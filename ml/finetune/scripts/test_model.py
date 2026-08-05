#!/usr/bin/env python3
"""Inference script for Egyptian Voice Command Model."""

import json
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import PeftModel

MODEL_PATH = (
    "/home/think/project/Kandil/EgyptianAgent/models/functiongemma-270m-egyptian"
)
BASE_MODEL = "google/functiongemma-270m-it"

SYSTEM_PROMPT = "You are a function calling assistant for Egyptian Arabic voice commands. Call functions based on user requests."


def load_model():
    print("Loading base model...")
    tokenizer = AutoTokenizer.from_pretrained(
        BASE_MODEL,
        trust_remote_code=True,
        padding_side="right",
    )
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    base_model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL,
        trust_remote_code=True,
        torch_dtype=torch.float32,
    )

    print("Loading LoRA adapter...")
    model = PeftModel.from_pretrained(base_model, MODEL_PATH)
    model = model.merge_and_unload()

    return model, tokenizer


def format_prompt(user_input: str) -> str:
    return f"<system>\n{SYSTEM_PROMPT}\n</system>\n<user>\n{user_input}\n</user>\n<assistant>\n"


def generate(model, tokenizer, user_input: str, max_new_tokens: int = 100):
    prompt = format_prompt(user_input)

    inputs = tokenizer(prompt, return_tensors="pt")
    inputs = {k: v.to(model.device) for k, v in inputs.items()}

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            do_sample=False,
            pad_token_id=tokenizer.pad_token_id,
            eos_token_id=tokenizer.eos_token_id,
        )

    response = tokenizer.decode(
        outputs[0][inputs["input_ids"].shape[1] :], skip_special_tokens=True
    )
    return response.strip()


def parse_function_call(response: str):
    try:
        start = response.find("{")
        end = response.rfind("}") + 1
        if start != -1 and end != 0:
            return json.loads(response[start:end])
    except json.JSONDecodeError:
        pass
    return response


def test_model():
    model, tokenizer = load_model()
    print("\n" + "=" * 60)
    print("Testing Egyptian Voice Command Model")
    print("=" * 60)

    test_commands = [
        "اتصل بعمتي نادية",
        "كلم حفيدي يوسف",
        "رن على ابن خالتي",
        "ابعته واتساب لحفيدتي مريم",
        "قول لخالتي منى إنى هزور",
        "نبهني بكرة على ٧ الصبح",
        "اضبط منبه بعد ٣ ساعات",
    ]

    for cmd in test_commands:
        print(f"\nUser: {cmd}")
        response = generate(model, tokenizer, cmd)
        parsed = parse_function_call(response)
        print(f"Response: {parsed}")


if __name__ == "__main__":
    test_model()
