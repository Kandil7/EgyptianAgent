# Integration Documentation

This directory contains integration guides for third-party services and components.

## Documents

### Integration Guides
- [FunctionGemma Integration](FUNCTIONGEMMA_INTEGRATION.md) - FunctionGemma integration guide
- [Saiy PS Integration](saiyy_ps_integration.md) - Saiy Power Scheduler integration

## Available Integrations

### FunctionGemma
FunctionGemma provides advanced language understanding and function calling capabilities.

**Integration Steps:**
1. Download FunctionGemma model
2. Configure model path in settings
3. Initialize FunctionGemma engine
4. Register function handlers

### Saiy Power Scheduler
Saiy PS provides power-efficient scheduling for background operations.

**Integration Steps:**
1. Add Saiy PS dependency
2. Configure power profiles
3. Register background tasks
4. Monitor power usage

## Integration Architecture

```
┌─────────────────┐     ┌─────────────────┐
│  EgyptianAgent  │────▶│  FunctionGemma  │
│                 │◀────│                 │
└─────────────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐
│   Saiy PS       │
│ (Power Mgmt)    │
└─────────────────┘
```

## API Integration Points

| Integration | Type | Purpose |
|-------------|------|---------|
| FunctionGemma | LLM | Advanced reasoning |
| Saiy PS | System | Power management |
| Whisper | ASR | Speech recognition |
| Llama.cpp | LLM | Local inference |

## Related Documentation

- [Architecture](../architecture/ARCHITECTURE.md)
- [API Reference](../api/API_REFERENCE.md)
- [Deployment](../deployment/DEPLOYMENT_GUIDE.md)
