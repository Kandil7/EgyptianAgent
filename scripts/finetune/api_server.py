#!/usr/bin/env python3
"""
Egyptian Voice Command API Server

FastAPI server for serving the fine-tuned Egyptian voice command model.
"""

import json
import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import PeftModel

app = FastAPI(title="Egyptian Voice Command API")

MODEL_PATH = (
    "/home/think/project/Kandil/EgyptianAgent/models/functiongemma-270m-egyptian"
)
BASE_MODEL = "google/functiongemma-270m-it"
SYSTEM_PROMPT = "You are a function calling assistant for Egyptian Arabic voice commands. Call functions based on user requests."

model = None
tokenizer = None


class CommandRequest(BaseModel):
    command: str
    max_tokens: int = 100


class CommandResponse(BaseModel):
    command: str
    function_call: dict
    raw_response: str


def load_model():
    global model, tokenizer
    print("Loading model...")
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

    model = PeftModel.from_pretrained(base_model, MODEL_PATH)
    model = model.merge_and_unload()
    print("Model loaded")


def parse_function_call(response: str):
    try:
        start = response.find("{")
        end = response.rfind("}") + 1
        if start != -1 and end != 0:
            return json.loads(response[start:end])
    except json.JSONDecodeError:
        pass
    return {"raw": response}


@app.on_event("startup")
async def startup():
    load_model()


@app.get("/")
async def root():
    return {"status": "ok", "model": "functiongemma-270m-egyptian"}


@app.get("/health")
async def health():
    return {"status": "healthy", "model_loaded": model is not None}


@app.post("/predict", response_model=CommandResponse)
async def predict(request: CommandRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    prompt = f"<system>\n{SYSTEM_PROMPT}\n</system>\n<user>\n{request.command}\n</user>\n<assistant>\n"

    inputs = tokenizer(prompt, return_tensors="pt")
    inputs = {k: v.to(model.device) for k, v in inputs.items()}

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=request.max_tokens,
            do_sample=False,
            pad_token_id=tokenizer.pad_token_id,
            eos_token_id=tokenizer.eos_token_id,
        )

    response = tokenizer.decode(
        outputs[0][inputs["input_ids"].shape[1] :], skip_special_tokens=True
    )
    function_call = parse_function_call(response.strip())

    return CommandResponse(
        command=request.command,
        function_call=function_call,
        raw_response=response.strip(),
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
