#!/usr/bin/env python3
"""
Egyptian Voice Command - Complete Application

A full-stack application for Egyptian Arabic voice command processing.
Includes: FastAPI backend + Streamlit frontend + model inference
"""

import os
import json
import torch
import tempfile
import subprocess
from pathlib import Path
from typing import Optional

import streamlit as st
from fastapi import FastAPI, HTTPException, File, UploadFile
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn

from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import PeftModel

# ============== Configuration ==============
MODEL_PATH = os.getenv(
    "MODEL_PATH",
    "/home/think/project/Kandil/EgyptianAgent/models/functiongemma-270m-egyptian",
)
BASE_MODEL = "google/functiongemma-270m-it"
SYSTEM_PROMPT = "You are a function calling assistant for Egyptian Arabic voice commands. Call functions based on user requests."


# ============== Model Loading ==============
@st.cache_resource
def load_model():
    st.info("Loading model...")
    tokenizer = AutoTokenizer.from_pretrained(
        BASE_MODEL, trust_remote_code=True, padding_side="right"
    )
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    base_model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL, trust_remote_code=True, torch_dtype=torch.float32
    )

    model = PeftModel.from_pretrained(base_model, MODEL_PATH)
    model = model.merge_and_unload()
    st.success("Model loaded!")
    return model, tokenizer


def parse_function_call(response: str) -> dict:
    try:
        start = response.find("{")
        end = response.rfind("}") + 1
        if start != -1 and end != 0:
            return json.loads(response[start:end])
    except json.JSONDecodeError:
        pass
    return {"raw": response}


def generate_response(tokenizer, model, command: str, max_tokens: int = 100) -> dict:
    prompt = f"<system>\n{SYSTEM_PROMPT}\n</system>\n<user>\n{command}\n</user>\n<assistant>\n"

    inputs = tokenizer(prompt, return_tensors="pt")
    inputs = {k: v.to(model.device) for k, v in inputs.items()}

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_tokens,
            do_sample=False,
            pad_token_id=tokenizer.pad_token_id,
            eos_token_id=tokenizer.eos_token_id,
        )

    response = tokenizer.decode(
        outputs[0][inputs["input_ids"].shape[1] :], skip_special_tokens=True
    )

    function_call = parse_function_call(response.strip())

    return {
        "command": command,
        "function_call": function_call,
        "raw_response": response.strip(),
    }


# ============== Streamlit UI ==============
def run_streamlit():
    st.set_page_config(
        page_title="Egyptian Voice Commands", page_icon="🇪🇬", layout="wide"
    )

    st.title("🇪🇬 Egyptian Voice Command Assistant")
    st.markdown("""
    This app translates Egyptian Arabic voice commands into function calls.
    Try commands like:
    - **اتصل بعمتي نادية** (Call my aunt Nadia)
    - **ابعته واتساب لحفيدتي مريم** (Send WhatsApp to my granddaughter Maryam)
    - **نبهني بكرة على ٧ الصبح** (Wake me up tomorrow at 7 AM)
    """)

    # Load model
    try:
        model, tokenizer = load_model()
    except Exception as e:
        st.error(f"Failed to load model: {e}")
        return

    # Sidebar - Command History
    st.sidebar.title("📋 History")

    if "history" not in st.session_state:
        st.session_state.history = []

    # Main input
    col1, col2 = st.columns([3, 1])

    with col1:
        command = st.text_input(
            "Enter Egyptian Arabic command:",
            placeholder="مثال: اتصل بعمتي نادية",
            label_visibility="collapsed",
        )

    with col2:
        st.write("")  # Spacing
        st.write("")  # Spacing
        if st.button("🎤 Process", type="primary", use_container_width=True):
            if command:
                with st.spinner("Processing..."):
                    result = generate_response(tokenizer, model, command)
                    st.session_state.history.insert(0, result)

    # Display results
    if st.session_state.history:
        st.divider()
        st.subheader("Results")

        for i, item in enumerate(st.session_state.history[:10]):
            with st.container():
                st.markdown(f"**👤 User:** {item['command']}")

                fc = item["function_call"]
                if isinstance(fc, dict) and "function" in fc:
                    st.code(
                        json.dumps(fc, ensure_ascii=False, indent=2), language="json"
                    )
                else:
                    st.markdown(f"**Response:** {item['raw_response']}")
                st.divider()

    # Example commands
    st.sidebar.title("💡 Examples")
    examples = [
        ("اتصل بعمتي نادية", "Call contact"),
        ("كلم حفيدي يوسف", "Call contact"),
        ("ابعته واتساب لحفيدتي مريم", "Send WhatsApp"),
        ("قول لخالتي منى إنى هزور", "Send WhatsApp with message"),
        ("نبهني بكرة على ٧ الصبح", "Set alarm"),
        ("اضبط منبه بعد ٣ ساعات", "Set alarm"),
    ]

    for cmd, desc in examples:
        if st.sidebar.button(f"{cmd}", use_container_width=True):
            result = generate_response(tokenizer, model, cmd)
            st.session_state.history.insert(0, result)
            st.rerun()


# ============== FastAPI Backend ==============
class CommandRequest(BaseModel):
    command: str
    max_tokens: int = 100


class CommandResponse(BaseModel):
    command: str
    function_call: dict
    raw_response: str


def create_api_app():
    app = FastAPI(title="Egyptian Voice Command API")

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    model = None
    tokenizer = None

    @app.on_event("startup")
    async def startup():
        nonlocal model, tokenizer
        tokenizer = AutoTokenizer.from_pretrained(
            BASE_MODEL, trust_remote_code=True, padding_side="right"
        )
        if tokenizer.pad_token is None:
            tokenizer.pad_token = tokenizer.eos_token

        base_model = AutoModelForCausalLM.from_pretrained(
            BASE_MODEL, trust_remote_code=True, torch_dtype=torch.float32
        )
        model = PeftModel.from_pretrained(base_model, MODEL_PATH)
        model = model.merge_and_unload()

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

        result = generate_response(
            tokenizer, model, request.command, request.max_tokens
        )
        return CommandResponse(**result)

    return app


# ============== Main Entry ==============
if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1:
        mode = sys.argv[1]

        if mode == "api":
            app = create_api_app()
            uvicorn.run(app, host="0.0.0.0", port=8000)

        elif mode == "ui":
            import subprocess

            subprocess.run(
                [
                    "streamlit",
                    "run",
                    __file__,
                    "--server.port",
                    "8501",
                    "--server.address",
                    "0.0.0.0",
                ]
            )

        elif mode == "both":
            import threading
            import signal

            # Start API in background thread
            api_app = create_api_app()
            api_thread = threading.Thread(
                target=lambda: uvicorn.run(api_app, host="0.0.0.0", port=8000),
                daemon=True,
            )
            api_thread.start()

            # Start Streamlit
            subprocess.run(
                [
                    "streamlit",
                    "run",
                    __file__,
                    "--server.port",
                    "8501",
                    "--server.address",
                    "0.0.0.0",
                ]
            )
        else:
            print("Usage: python app.py [api|ui|both]")
    else:
        run_streamlit()
