# Egyptian Agent - OpenCode Integration Guide

This document explains how to use the agent definitions in `agents/` with OpenCode's subagent system.

## Overview

You have 18 specialized Egyptian Agent team members defined in markdown. OpenCode's `task` tool uses predefined subagent types, but you can inject custom system prompts to make them behave as your specialized agents.

## Mapping: Egyptian Agents → OpenCode Subagent Types

| Egyptian Agent | OpenCode Type | When to Use |
|:---|:---|:---|
| **Product_Manager_Agent** | `product-engineer` | Product strategy, roadmaps, user stories |
| **Technical_Lead_Agent** | `full-stack-ai-engineer` | Architecture, code review, technical decisions |
| **ML_Engineer_LLM_Agent** | `full-stack-ai-engineer` | LLM integration, prompt engineering, llama.cpp |
| **ML_Engineer_ASR_Agent** | `full-stack-ai-engineer` | Whisper fine-tuning, speech recognition |
| **ML_Engineer_NLU_Agent** | `full-stack-ai-engineer` | Intent classification, slot filling |
| **Data_Engineer_Agent** | `data-engineer` | Data pipelines, datasets, annotation |
| **Senior_Android_Engineer_System_Agent** | `software-engineer` | Android system integration, VoiceInteractionService, root ops |
| **Mid_Android_Engineer_Features_Agent** | `software-engineer` | Audio pipeline, WhatsApp integration, alarms |
| **Android_Engineer_UI_Agent** | `software-engineer` | UI implementation, accessibility, views |
| **Cpp_Developer_Agent** | `software-engineer` | JNI bridges, NDK, native optimization |
| **Senior_UX_Designer_Agent** | `product-engineer` | Conversational UX, dialog flows |
| **UI_Designer_Agent** | `frontend-engineer` | Visual design, UI components |
| **QA_Engineer_Automation_Agent** | `qa-automation-engineer` | Automated testing, Espresso, CI/CD |
| **QA_Tester_Manual_Agent** | `qa-automation-engineer` | Manual testing, field testing |
| **DevOps_Engineer_Agent** | `dev-ops-platform-engineer` | CI/CD, releases, infrastructure |
| **Security_Specialist_Agent** | `security-compliance-engineer` | Security audits, encryption |
| **Medical_Consultant_Agent** | `ai-evaluation-engineer` | Health protocols, fall detection logic |

## Usage Patterns

### Pattern 1: Direct System Prompt Injection (Recommended)

Use the `task` tool with the closest subagent type, but include the full system prompt in your prompt:

```
task(
  subagent_type: "software-engineer",
  prompt: "You are the Senior Android Engineer specializing in System Integration for Egyptian Agent. [FULL SYSTEM PROMPT FROM agents/Senior_Android_Engineer_System_Agent.md]"
)
```

### Pattern 2: Load as Skill

Load a skill that contains the agent persona (see skills/ directory).

### Pattern 3: Context Inclusion

Include the agent definition as context when working on tasks.

## Example Invocations

### Example 1: Ask Technical Lead for Architecture Review

```
Use the Technical Lead agent to review this Android architecture:
- Subagent: full-stack-ai-engineer
- Inject system prompt from agents/Technical_Lead_Agent.md
- Task: Review the proposed MVVM + Clean Architecture for the voice assistant
```

### Example 2: Ask ML Engineer about LLM Optimization

```
Use the LLM Optimizer agent to analyze model performance:
- Subagent: full-stack-ai-engineer  
- Inject system prompt from agents/ML_Engineer_LLM_Agent.md
- Task: Evaluate Llama 3.2 3B quantization options for Honor X6c
```

### Example 3: Ask Product Manager for Priority Decision

```
Use the Product Manager agent to prioritize features:
- Subagent: product-engineer
- Inject system prompt from agents/Product_Manager_Agent.md
- Task: Should we prioritize WhatsApp integration or fall detection first?
```

## Quick Reference Card

| Need | Use Agent | OpenCode Type |
|:---|:---|:---|
| Product strategy | Product_Manager | product-engineer |
| Architecture decisions | Technical_Lead | full-stack-ai-engineer |
| LLM/inference issues | ML_Engineer_LLM | full-stack-ai-engineer |
| Speech recognition | ML_Engineer_ASR | full-stack-ai-engineer |
| Android system integration | Senior_Android_System | software-engineer |
| Android UI/UX | Android_UI / Mid_Android | software-engineer |
| C++/NDK optimization | Cpp_Developer | software-engineer |
| Data pipelines | Data_Engineer | data-engineer |
| CI/CD/DevOps | DevOps_Engineer | dev-ops-platform-engineer |
| Security review | Security_Specialist | security-compliance-engineer |
| Testing strategy | QA_Automation / QA_Manual | qa-automation-engineer |
| Conversational design | Senior_UX_Designer | product-engineer |
| Visual design | UI_Designer | frontend-engineer |
| Health/fall detection | Medical_Consultant | ai-evaluation-engineer |
| Intent/NLU design | ML_Engineer_NLU | full-stack-ai-engineer |

## Automation

To streamline agent usage, you can:
1. Create skills for frequently-used agents (see .claude/skills/)
2. Use the agent router pattern for natural language routing
3. Template the system prompt injection for common agents

---

## Available Skills (Project-Level)

I've created project-level skills in `.claude/skills/` that you can load directly:

| Skill | Description |
|:---|:---|
| `egyptian-product-manager` | Product strategy, roadmaps, prioritization |
| `egyptian-tech-lead` | Architecture, code quality, technical decisions |
| `egyptian-llm-engineer` | Llama 3.2 integration, prompt engineering |
| `egyptian-android-system` | Android system integration, root ops |
| `egyptian-qa-automation` | Automated testing, CI/CD |

### Loading a Skill
```
Use the skill tool to load: egyptian-product-manager
Then ask your question directly
```

---

## Usage Examples

### Example 1: Product Decision
```
Load skill: egyptian-product-manager
Question: "Should we prioritize WhatsApp integration or fall detection first?"
```

### Example 2: Technical Architecture Review
```
Load skill: egyptian-tech-lead
Question: "Review our proposed Clean Architecture for the voice assistant"
```

### Example 3: LLM Prompt Engineering
```
Load skill: egyptian-llm-engineer
Question: "The model keeps answering in Modern Standard Arabic instead of Egyptian dialect"
```

### Example 4: Android System Integration
```
Load skill: egyptian-android-system
Question: "How do we toggle WiFi reliably in the background on Honor X6c?"
```

### Example 5: Using Subagent (No Skill Available)
```
Use task tool with:
- subagent_type: "full-stack-ai-engineer"
- prompt: "You are the ASR Specialist for Egyptian Agent. [INJECT FULL PROMPT FROM agents/ML_Engineer_ASR_Agent.md]"
- task: "Optimize Whisper model for Egyptian dialect recognition"
```