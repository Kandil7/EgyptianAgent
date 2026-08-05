#!/usr/bin/env python3
"""
Merge LoRA adapter with base model and push to Hugging Face Hub
"""
import os
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
from huggingface_hub import create_repo, upload_folder

def main():
    base_model_id = "google/functiongemma-270m-it"
    adapter_id = "Kandil7/functiongemma-270m-egyptian-mobile-action"
    merged_repo_id = "Kandil7/functiongemma-270m-egyptian-mobile-action-merged"
    local_merged_path = "models/functiongemma-270m-egyptian-merged"
    token = os.environ.get("HF_TOKEN")
    
    if not token:
        raise ValueError("HF_TOKEN environment variable not set")
    
    print(f"Loading base model: {base_model_id}")
    tokenizer = AutoTokenizer.from_pretrained(base_model_id)
    base_model = AutoModelForCausalLM.from_pretrained(
        base_model_id,
        torch_dtype=torch.float32,
        device_map="cpu"
    )
    
    print(f"Loading adapter: {adapter_id}")
    model = PeftModel.from_pretrained(base_model, adapter_id)
    
    print("Merging adapter weights into base model...")
    model = model.merge_and_unload()
    
    print(f"Saving merged model to: {local_merged_path}")
    os.makedirs(local_merged_path, exist_ok=True)
    model.save_pretrained(local_merged_path)
    tokenizer.save_pretrained(local_merged_path)
    
    print(f"Creating HF repo: {merged_repo_id}")
    create_repo(merged_repo_id, exist_ok=True, private=False, token=token)
    
    print(f"Uploading merged model to HF...")
    upload_folder(
        repo_id=merged_repo_id,
        folder_path=local_merged_path,
        token=token,
        commit_message="Upload merged FunctionGemma-270M Egyptian model"
    )
    
    print(f"✅ Merged model uploaded to https://huggingface.co/{merged_repo_id}")

if __name__ == "__main__":
    main()