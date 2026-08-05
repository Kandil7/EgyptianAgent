#!/usr/bin/env python3
"""
Upload Egyptian Agent datasets to Hugging Face Hub.
Usage: python upload_datasets.py --token YOUR_HF_TOKEN
"""
import argparse
import os
from pathlib import Path
from huggingface_hub import HfApi, create_repo, upload_folder

def upload_dataset(repo_id: str, local_path: Path, token: str, repo_type: str = "dataset"):
    """Upload a dataset folder to HF Hub."""
    api = HfApi(token=token)
    
    # Create repo if it doesn't exist
    try:
        create_repo(repo_id, repo_type=repo_type, private=False, token=token, exist_ok=True)
        print(f"✅ Repo created/verified: {repo_id}")
    except Exception as e:
        print(f"⚠️ Repo creation: {e}")
    
    # Upload the folder
    print(f"📤 Uploading {local_path} to {repo_id}...")
    upload_folder(
        repo_id=repo_id,
        folder_path=str(local_path),
        repo_type=repo_type,
        token=token,
        commit_message=f"Upload {local_path.name} dataset"
    )
    print(f"✅ Uploaded: https://huggingface.co/datasets/{repo_id}")

def main():
    parser = argparse.ArgumentParser(description="Upload Egyptian Agent datasets to HF Hub")
    parser.add_argument("--token", required=True, help="Hugging Face token")
    parser.add_argument("--org", default="Kandil7", help="HF organization/user (default: Kandil7)")
    args = parser.parse_args()
    
    token = args.token
    org = args.org
    base_path = Path("K:/projects/ai-ml/EgyptianAgent/datasets")
    
    # Upload egyptian_voice_commands
    voice_commands_path = base_path / "egyptian_voice_commands"
    if voice_commands_path.exists():
        upload_dataset(f"{org}/egyptian-voice-commands", voice_commands_path, token)
    
    # Upload egyptian_ui_navigation
    ui_nav_path = base_path / "egyptian_ui_navigation"
    if ui_nav_path.exists():
        upload_dataset(f"{org}/egyptian-ui-navigation", ui_nav_path, token)
    
    print("\n🎉 All datasets uploaded!")

if __name__ == "__main__":
    main()