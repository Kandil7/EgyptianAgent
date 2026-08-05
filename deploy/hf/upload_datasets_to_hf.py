#!/usr/bin/env python3
"""
Upload Egyptian Voice Commands and UI Navigation datasets to Hugging Face Hub
"""
import os
from pathlib import Path
from huggingface_hub import HfApi, create_repo, upload_folder

def main():
    repo_id = "Kandil7/egyptian-voice-commands"
    local_dir = Path("datasets")
    token = os.environ.get("HF_TOKEN")
    
    if not token:
        raise ValueError("HF_TOKEN environment variable not set")
    
    api = HfApi(token=token)
    
    # Create repo (public)
    print(f"Creating repo: {repo_id}")
    create_repo(repo_id, exist_ok=True, private=False, token=token, repo_type="dataset")
    
    # Upload entire datasets folder
    print(f"Uploading from: {local_dir}")
    upload_folder(
        repo_id=repo_id,
        folder_path=str(local_dir),
        token=token,
        commit_message="Upload Egyptian Voice Commands and UI Navigation datasets",
        repo_type="dataset"
    )
    
    print(f"✅ Uploaded to https://huggingface.co/datasets/{repo_id}")

if __name__ == "__main__":
    main()