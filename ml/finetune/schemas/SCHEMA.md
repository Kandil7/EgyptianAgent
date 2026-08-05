# Egyptian Commands Synthetic Dataset Pipeline

This folder describes a complete synthetic data pipeline for generating an Egyptian Arabic command dataset suitable for EgyptianAgent / Kandil Agentic Android.

It includes:

- JSON schema for commands.
- Base and extended examples.
- Prompt templates for LLM-based synthetic generation.
- A Python script to generate new samples (using any LLM API you plug in).
- A Python script to prepare and upload the dataset to Hugging Face Datasets.

## Folder structure

```text
egptian_commands_pipeline/
├── README.md
├── SCHEMA.md
├── prompts/
│   ├── system_prompt.txt
│   ├── intent_generation_prompt.md
│   └── negative_examples_prompt.md
├── data/
│   ├── egyptian_commands_schema_v1_extended.json
│   └── egyptian_commands_schema_v1.schema.json
└── scripts/
    ├── generate_synthetic_data.py
    └── upload_to_hf.py
```

You can integrate this pipeline into EgyptianAgent or Kandil Agentic Android projects.
