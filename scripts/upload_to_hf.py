#!/usr/bin/env python3
"""
Upload FunctionGemma Egyptian LoRA adapter to Hugging Face Hub
"""
import os
from pathlib import Path
from huggingface_hub import HfApi, create_repo, upload_file

def main():
    repo_id = "Kandil7/functiongemma-270m-egyptian-mobile-action"
    local_dir = Path("models/functiongemma-270m-egyptian")
    token = os.environ.get("HF_TOKEN")
    
    if not token:
        raise ValueError("HF_TOKEN environment variable not set")
    
    api = HfApi(token=token)
    
    # Create repo (public)
    print(f"Creating repo: {repo_id}")
    create_repo(repo_id, exist_ok=True, private=False, token=token)
    
    # Files to upload (skip large tokenizer.json - use base model's tokenizer)
    files_to_upload = [
        "adapter_model.safetensors",
        "adapter_config.json",
        "tokenizer_config.json",
        "chat_template.jinja",
        "README.md",
    ]
    
    for filename in files_to_upload:
        local_path = local_dir / filename
        if local_path.exists():
            print(f"Uploading {filename}...")
            upload_file(
                path_or_fileobj=str(local_path),
                path_in_repo=filename,
                repo_id=repo_id,
                token=token,
                commit_message=f"Add {filename}"
            )
        else:
            print(f"Warning: {filename} not found, skipping")
    
    print(f"✅ Uploaded to https://huggingface.co/{repo_id}")

if __name__ == "__main__":
    main()