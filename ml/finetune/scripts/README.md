# Egyptian Voice Command App 🇪🇬

A complete application for processing Egyptian Arabic voice commands using fine-tuned FunctionGemma model.

## Quick Start

### Option 1: Streamlit UI (Recommended)
```bash
cd /home/think/project/Kandil/EgyptianAgent/scripts/finetune
streamlit run app.py
```

### Option 2: FastAPI Backend Only
```bash
python api_server.py
# API at http://localhost:8000
```

### Option 3: Both UI + API
```bash
python app.py both
```

## Features

- **Streamlit UI**: User-friendly web interface with command history
- **FastAPI Backend**: REST API for programmatic access
- **Egyptian Arabic Support**: Processes commands like:
  - `اتصل بعمتي نادية` → call_contact
  - `ابعته واتساب لحفيدتي مريم` → send_whatsapp
  - `نبهني بكرة على ٧ الصبح` → set_alarm

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Root info |
| `/health` | GET | Health check |
| `/predict` | POST | Process command |

### Example API Call
```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{"command": "اتصل بعمتي نادية"}'
```

## Model

- **Base Model**: google/functiongemma-270m-it
- **Fine-tuned**: 25 epochs on Egyptian voice commands
- **Location**: `/home/think/project/Kandil/EgyptianAgent/models/functiongemma-270m-egyptian`

## Files

- `app.py` - Main app (Streamlit + FastAPI)
- `api_server.py` - Standalone FastAPI server
- `test_model.py` - Quick inference test
- `finetune_functiongemma_egyptian.py` - Training script
- `evaluate_egyptian_accuracy.py` - Evaluation script
