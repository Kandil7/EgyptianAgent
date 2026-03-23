# EgyptianAgent - Hybrid AI Architecture

**Version:** 3.0.0
**Last Updated:** March 14, 2026
**Status:** 🚀 Implementation Ready
**Author:** EgyptianAgent Technical Team

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [System Design](#system-design)
4. [Component Specifications](#component-specifications)
5. [Data Flow Diagrams](#data-flow-diagrams)
6. [Routing Logic](#routing-logic)
7. [Fallback Mechanisms](#fallback-mechanisms)
8. [Performance Targets](#performance-targets)
9. [Integration Strategy](#integration-strategy)
10. [Testing Strategy](#testing-strategy)
11. [Migration Guide](#migration-guide)

---

## Executive Summary

### Vision

The **Hybrid AI Architecture** combines the speed and accuracy of FunctionGemma's intent-based system with DroidClaw-style UI navigation capabilities to create a truly versatile voice assistant for Egyptian seniors and visually impaired users.

### Problem Statement

| Scenario | Current System (Intent-Only) | Limitation |
|----------|------------------------------|------------|
| "اتصل بماما" (Call mom) | ✅ Works perfectly | - |
| "افتح الفيسبوك وشوف الأخبار" (Open Facebook, check news) | ❌ Cannot navigate UI | Requires UI navigation |
| "ابعت رسالة واتساب" (Send WhatsApp message) | ✅ Works if contact clear | ❌ Fails if contact unclear |
| "احجز أوبر" (Book Uber) | ❌ Cannot handle multi-step | Requires workflow |
| "النهاردة كام؟" (What's today's date?) | ✅ Works perfectly | - |

### Solution: Hybrid Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    HYBRID AI ARCHITECTURE                            │
│                                                                     │
│  ┌─────────────────┐         ┌─────────────────┐                   │
│  │  FAST PATH      │         │  SLOW PATH      │                   │
│  │  (Intent-Based) │         │  (UI Navigation)│                   │
│  │                 │         │                 │                   │
│  │  FunctionGemma  │         │  Accessibility  │                   │
│  │  270M           │         │  Tree + LLM     │                   │
│  │                 │         │                 │                   │
│  │  350ms          │         │  2-5s           │                   │
│  │  95.2% accuracy │         │  90%+ success   │                   │
│  │                 │         │                 │                   │
│  │  Common commands│         │  Complex tasks  │                   │
│  └─────────────────┘         └─────────────────┘                   │
│           │                           │                             │
│           └───────────┬───────────────┘                             │
│                       ▼                                             │
│              ┌─────────────────┐                                    │
│              │   HYBRID        │                                    │
│              │   ORCHESTRATOR  │                                    │
│              │   (Decision     │                                    │
│              │    Engine)      │                                    │
│              └─────────────────┘                                    │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Benefits

| Benefit | Impact |
|---------|--------|
| **Expanded Capabilities** | Handle 10x more task types |
| **Maintained Performance** | Fast path unchanged (350ms) |
| **Privacy Preserved** | 100% local processing |
| **Egyptian Dialect** | UI navigation understands dialect |
| **Accessibility Focus** | Enhanced for visually impaired |

---

## Architecture Overview

### High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         EGYPTIANAGENT HYBRID SYSTEM                          │
│                         (FunctionGemma + UI Navigation)                      │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐     ┌──────────────────┐     ┌──────────────────────────┐
│   User Voice     │────▶│  EgyptianWhisper │────▶│   Text Preprocessor      │
│   Input          │     │  ASR Engine      │     │   (Egyptian Dialect)     │
│   (Microphone)   │     │  (Speech→Text)   │     │   - Normalization        │
└──────────────────┘     └──────────────────┘     │   - Tokenization         │
                                                  └───────────┬──────────────┘
                                                              │
                                                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      HYBRID ORCHESTRATOR (NEW)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ROUTING DECISION ENGINE                           │   │
│  │                                                                      │   │
│  │  Input: Normalized text + FunctionGemma intent + confidence          │   │
│  │  Output: FAST_PATH or SLOW_PATH                                      │   │
│  │                                                                      │   │
│  │  Rules:                                                              │   │
│  │  • IF intent_type in [CALL, WHATSAPP, ALARM, OPEN_APP]               │   │
│  │    AND confidence > 0.85 → FAST_PATH                                 │   │
│  │  • IF intent_type = UNKNOWN OR confidence < 0.70 → SLOW_PATH         │   │
│  │  • IF intent requires UI interaction → SLOW_PATH                     │   │
│  │  • IF multi-step workflow needed → SLOW_PATH                         │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                            │                    │
                            │ FAST_PATH          │ SLOW_PATH
                            │ (Intent-Based)     │ (UI Navigation)
                            ▼                    ▼
┌─────────────────────────────────┐   ┌──────────────────────────────────────┐
│     FUNCTIONGEMMA EXECUTOR      │   │        UI NAVIGATION ENGINE          │
│     (Existing System)           │   │        (DroidClaw-Inspired)          │
├─────────────────────────────────┤   ├──────────────────────────────────────┤
│  • CallExecutor                 │   │  • AccessibilityTreeParser           │
│  • WhatsAppExecutor             │   │  • UINavigationEngine (28 actions)   │
│  • AlarmExecutor                │   │  • Perception→Reason→Action Loop     │
│  • AppsExecutor                 │   │  • WorkflowEngine (YAML flows)       │
│  • EmergencyHandler             │   │  • Vision fallback for Flutter/Web   │
│  • SettingsController           │   │                                      │
└────────────────┬────────────────┘   └─────────────────┬────────────────────┘
                 │                                      │
                 └──────────────────┬───────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RESPONSE GENERATION                                  │
│  • TTS (Egyptian Voice)                                                     │
│  • Haptic Feedback                                                          │
│  • Visual Overlay (Senior Mode)                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Architecture Layers

| Layer | Component | Technology | Purpose |
|-------|-----------|------------|---------|
| **Input** | EgyptianWhisper ASR | Whisper.cpp | Egyptian speech-to-text |
| **Preprocessing** | EgyptianNormalizer | Custom NLP | Dialect normalization |
| **Orchestration** | HybridOrchestrator | Custom Kotlin | Fast/slow path routing |
| **Fast Path** | FunctionGemma Engine | Gemma 270M | Intent classification |
| **Slow Path** | UI Navigation Engine | AccessibilityService | UI automation |
| **Execution** | Executors + Actions | Android APIs | Command execution |
| **Output** | TTS + Feedback | PiperTTS | User response |

---

## System Design

### Design Principles

1. **Preserve Existing Functionality**: Fast path unchanged, 95.2% accuracy maintained
2. **Progressive Enhancement**: UI navigation augments, doesn't replace intent system
3. **Privacy First**: All processing remains on-device
4. **Performance Critical**: <500ms overhead for UI navigation mode
5. **Accessibility Focus**: Enhanced for visually impaired users
6. **Egyptian Dialect**: UI navigation understands Egyptian commands

### Component Interaction Patterns

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        COMPONENT INTERACTION FLOW                            │
└─────────────────────────────────────────────────────────────────────────────┘

User Voice Command
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 1. WAKE WORD DETECTION                                                   │
│    • "يا صاحبي" / "يا كبير"                                              │
│    • <200ms latency                                                      │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 2. SPEECH-TO-TEXT (ASR)                                                  │
│    • EgyptianWhisperASR.transcribe()                                     │
│    • Output: "افتح الفيسبوك وشوف الأخبار"                                │
│    • <800ms latency                                                      │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 3. TEXT NORMALIZATION                                                    │
│    • EgyptianNormalizer.normalize()                                      │
│    • Dialect standardization                                             │
│    • <10ms latency                                                       │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 4. HYBRID ORCHESTRATOR DECISION                                          │
│    • FunctionGemma.classifyIntent()                                      │
│    • Routing logic evaluation                                            │
│    • Decision: FAST_PATH or SLOW_PATH                                    │
│    • <100ms latency                                                      │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ├──────────────────────┬──────────────────────────────┐
       │ FAST_PATH            │ SLOW_PATH                    │
       ▼                      ▼                              │
┌──────────────────┐  ┌──────────────────────────┐          │
│ 5a. FUNCTION     │  │ 5b. UI NAVIGATION        │          │
│     GEMMA        │  │     ENGINE               │          │
│     EXECUTOR     │  │                          │          │
│                  │  │ • Perception:            │          │
│ • Direct intent  │  │   Dump accessibility     │          │
│   execution      │  │   tree                   │          │
│ • 350ms total    │  │ • Reason:                │          │
│ • 95.2% accuracy │  │   LLM analyzes UI        │          │
│                  │  │ • Action:                │          │
│                  │  │   Execute tap/swipe/type │          │
│                  │  │ • Loop until done        │          │
│                  │  │ • 2-5s total             │          │
│                  │  │ • 90%+ success           │          │
└──────────────────┘  └──────────────────────────┘          │
       │                      │                              │
       └──────────────────────┴──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 6. RESPONSE GENERATION                                                   │
│    • TTS.speak("تمام، جاري فتح الفيسبوك...")                             │
│    • Haptic feedback                                                     │
│    • <500ms latency                                                      │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Component Specifications

### 1. AccessibilityTreeParser (NEW)

**Location:** `app/src/main/java/com/egyptian/agent/accessibility/ui/AccessibilityTreeParser.kt`

**Purpose:** Parse Android accessibility tree into LLM-readable format

**Key Features:**
- Dump accessibility tree via AccessibilityService
- Parse XML to structured UI elements
- Diff detection for screen changes
- Handle Flutter/webview edge cases
- Vision fallback for empty trees

**Interface:**
```kotlin
interface AccessibilityTreeParser {
    /**
     * Dump and parse current accessibility tree
     * @return List of interactive UI elements
     */
    fun parseCurrentTree(): AccessibilityTree
    
    /**
     * Detect changes between two trees
     * @return List of changed elements
     */
    fun diffTrees(oldTree: AccessibilityTree, newTree: AccessibilityTree): List<UIElement>
    
    /**
     * Convert tree to LLM-readable format (XML/JSON)
     * @return Formatted string for LLM consumption
     */
    fun toLLMFormat(tree: AccessibilityTree): String
    
    /**
     * Check if tree is empty (requires vision fallback)
     */
    fun isEmpty(tree: AccessibilityTree): Boolean
}
```

**Data Structures:**
```kotlin
data class AccessibilityTree(
    val timestamp: Long,
    val packageName: String,
    val elements: List<UIElement>,
    val screenshotPath: String? = null
)

data class UIElement(
    val id: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isEnabled: Boolean,
    val depth: Int,
    val children: List<UIElement> = emptyList()
)
```

---

### 2. UINavigationEngine (NEW)

**Location:** `app/src/main/java/com/egyptian/agent/navigation/UINavigationEngine.kt`

**Purpose:** Execute UI navigation actions via AccessibilityService

**28 Supported Actions:**

| Category | Actions |
|----------|---------|
| **Basic Interactions** | `tap`, `type`, `enter`, `longPress`, `clear`, `paste`, `swipe`, `scroll` |
| **Navigation** | `home`, `back`, `launch`, `switchApp`, `openUrl`, `openSettings`, `notifications` |
| **Clipboard** | `clipboardGet`, `clipboardSet` |
| **Multi-step Skills** | `readScreen`, `submitMessage`, `copyVisibleText`, `waitForContent`, `findAndTap`, `composeEmail` |
| **System** | `screenshot`, `shell`, `keyEvent`, `pullFile`, `pushFile`, `wait`, `done` |

**Interface:**
```kotlin
interface UINavigationEngine {
    /**
     * Execute a single UI action
     * @param action Action to execute
     * @return ActionResult with success/failure
     */
    suspend fun executeAction(action: UIAction): ActionResult
    
    /**
     * Execute perception → reason → action loop
     * @param goal User's goal in Egyptian Arabic
     * @return Final result
     */
    suspend fun navigateToGoal(goal: String): NavigationResult
    
    /**
     * Check if current screen matches expected state
     */
    fun verifyScreenState(expectedElements: List<String>): Boolean
}
```

**Action Data Structure:**
```kotlin
sealed class UIAction {
    data class Tap(val elementId: String, val description: String) : UIAction()
    data class Type(val text: String, val targetId: String? = null) : UIAction()
    data class Swipe(val startX: Int, val startY: Int, val endX: Int, val endY: Int) : UIAction()
    data class Scroll(val direction: ScrollDirection) : UIAction()
    object Home : UIAction()
    object Back : UIAction()
    data class Launch(val packageName: String) : UIAction()
    // ... 22 more actions
}

enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }

data class ActionResult(
    val success: Boolean,
    val message: String,
    val screenChanged: Boolean,
    val error: Throwable? = null
)
```

---

### 3. HybridOrchestrator (ENHANCED)

**Location:** `app/src/main/java/com/egyptian/agent/hybrid/HybridOrchestrator.kt`

**Purpose:** Route commands to fast or slow path

**Key Responsibilities:**
- Receive intent from FunctionGemma
- Evaluate routing rules
- Manage perception → reason → action loop for slow path
- Handle fallback strategies

**Interface:**
```kotlin
interface HybridOrchestrator {
    /**
     * Process user command and route to appropriate path
     * @param command Normalized Egyptian Arabic text
     * @return Execution result
     */
    suspend fun processCommand(command: String): CommandResult
    
    /**
     * Check if orchestrator is ready
     */
    fun isReady(): Boolean
    
    /**
     * Clean up resources
     */
    fun destroy()
}
```

**Routing Logic:**
```kotlin
sealed class RoutingPath {
    object FAST : RoutingPath()  // Intent-based
    object SLOW : RoutingPath()  // UI navigation
}

data class RoutingDecision(
    val path: RoutingPath,
    val reason: String,
    val confidence: Float,
    val intentType: IntentType?,
    val requiresUINavigation: Boolean,
    val uiContext: UIContext?
)

data class UIContext(
    val targetApp: String?,
    val expectedElements: List<String>,
    val isMultiStep: Boolean
)
```

---

### 4. Enhanced FunctionGemma Integration

**Location:** `app/src/main/java/com/egyptian/agent/ai/FunctionGemmaIntentEngine.kt`

**Enhanced Output:**
```kotlin
data class EnhancedIntentResult(
    val intentType: IntentType,
    val confidence: Float,
    val entities: Map<String, String>,
    val rawText: String,
    val processingTimeMs: Long,
    // NEW fields for hybrid routing
    val requiresUINavigation: Boolean,
    val uiContext: UIContext?,
    val reasoning: String?  // LLM's thought process
)
```

**Updated System Prompt:**
```
<|start_header_id|>system<|end_header_id|>

You are an Egyptian Arabic voice assistant with TWO capabilities:

1. FAST PATH: Direct intent classification for common commands
   - Calls, WhatsApp, alarms, apps, settings
   - Output: intent_type + entities

2. SLOW PATH: UI navigation for complex tasks
   - "افتح الفيسبوك وشوف الأخبار" → Navigate Facebook UI
   - "احجز أوبر" → Multi-step Uber booking
   - Output: requires_ui_navigation = true + ui_context

Decision Rules:
- IF clear intent (call, whatsapp, alarm) → FAST PATH
- IF vague/complex (check news, book ride) → SLOW PATH
- IF confidence < 0.70 → SLOW PATH

Output JSON format:
{
  "intent_type": "...",
  "confidence": 0.95,
  "entities": {...},
  "requires_ui_navigation": false,
  "ui_context": null,
  "reasoning": "Clear intent, use fast path"
}

Supported intent types:
- call_contact, send_whatsapp, set_alarm, open_app, ...
- ui_navigation (for complex tasks)

<|eot_id|>
```

---

### 5. WorkflowEngine (NEW)

**Location:** `app/src/main/java/com/egyptian/agent/workflow/WorkflowEngine.kt`

**Purpose:** Execute YAML-based deterministic workflows

**Workflow Format (YAML):**
```yaml
# Morning Routine Workflow
appId: com.google.android.googlequicksearchbox
name: Morning Routine (روتين الصباح)
description: Check weather, news, and send morning message
---
- launchApp: com.google.android.googlequicksearchbox
- wait: 2
- type: "أخبار اليوم"
- tap: "Search"
- wait: 3
- readScreen
- back
- launchApp: com.whatsapp
- wait: 2
- tap: "ماما"
- type: "صباح الخير يا حبيبتي"
- tap: "Send"
- done: "تم روتين الصباح"
```

**Interface:**
```kotlin
interface WorkflowEngine {
    /**
     * Load workflow from YAML
     */
    fun loadWorkflow(yamlContent: String): Workflow
    
    /**
     * Execute workflow
     */
    suspend fun executeWorkflow(workflow: Workflow, variables: Map<String, String> = emptyMap()): WorkflowResult
    
    /**
     * Create workflow from voice command
     */
    suspend fun createWorkflowFromVoice(description: String): Workflow
}
```

**Pre-built Workflows:**
1. **Morning Routine** (روتين الصباح): Weather + news + morning message
2. **Coming Home** (رجوع البيت): Turn on WiFi, AC, lights
3. **Bedtime** (وقت النوم): Alarms, lights off, Do Not Disturb

---

## Data Flow Diagrams

### Fast Path Data Flow (Intent-Based)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FAST PATH DATA FLOW                                  │
│                         (Existing System - Unchanged)                        │
└─────────────────────────────────────────────────────────────────────────────┘

User: "اتصل بماما"
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 1. WAKE WORD: "يا صاحبي"                                                 │
│    Latency: <200ms                                                       │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 2. ASR: EgyptianWhisperASR.transcribe()                                  │
│    Output: "اتصل بماما"                                                  │
│    Latency: <800ms                                                       │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 3. NORMALIZE: EgyptianNormalizer.normalize()                             │
│    Output: "اتصل بماما" (normalized)                                     │
│    Latency: <10ms                                                        │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 4. CLASSIFY: FunctionGemmaIntentEngine.classifyIntent()                  │
│    Output: {intent: CALL_CONTACT, confidence: 0.97, entities: {...}}     │
│    Latency: <280ms                                                       │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 5. ROUTE: HybridOrchestrator.evaluate()                                  │
│    Decision: FAST_PATH (clear intent, high confidence)                   │
│    Latency: <50ms                                                        │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 6. EXECUTE: CallExecutor.execute()                                       │
│    Action: Initiate phone call to contact "ماما"                         │
│    Latency: <200ms                                                       │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 7. RESPOND: TTS.speak()                                                  │
│    Output: "تمام، باتصل بماما دلوقتي"                                    │
│    Latency: <500ms                                                       │
└──────────────────────────────────────────────────────────────────────────┘

TOTAL FAST PATH LATENCY: ~2.0 seconds (unchanged from current system)
```

### Slow Path Data Flow (UI Navigation)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SLOW PATH DATA FLOW                                  │
│                         (DroidClaw-Inspired Loop)                            │
└─────────────────────────────────────────────────────────────────────────────┘

User: "افتح الفيسبوك وشوف الأخبار"
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 1-3. WAKE WORD + ASR + NORMALIZE (same as fast path)                     │
│    Output: "افتح الفيسبوك وشوف الأخبار"                                  │
│    Latency: ~1.0s                                                        │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 4. CLASSIFY: FunctionGemmaIntentEngine.classifyIntent()                  │
│    Output: {intent: UI_NAVIGATION, confidence: 0.65,                     │
│             requires_ui_navigation: true,                                │
│             ui_context: {target_app: "com.facebook.katana"}}             │
│    Latency: <350ms                                                       │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 5. ROUTE: HybridOrchestrator.evaluate()                                  │
│    Decision: SLOW_PATH (complex task, requires UI)                       │
│    Latency: <50ms                                                        │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 6. PERCEPTION → REASON → ACTION LOOP                                     │
│                                                                          │
│    ┌─────────────────────────────────────────────────────────────────┐  │
│    │ ITERATION 1:                                                    │  │
│    │  • PERCEIVE: Dump accessibility tree                            │  │
│    │    Current app: Home screen                                     │  │
│    │    Elements: [Facebook icon, WhatsApp, Gmail, ...]              │  │
│    │  • REASON: LLM analyzes screen                                  │  │
│    │    "I need to open Facebook first. Facebook icon at (234, 567)" │  │
│    │  • ACT: Execute tap(234, 567)                                   │  │
│    │    Result: Facebook launching                                   │  │
│    └─────────────────────────────────────────────────────────────────┘  │
│                           │                                              │
│                           ▼                                              │
│    ┌─────────────────────────────────────────────────────────────────┐  │
│    │ ITERATION 2:                                                    │  │
│    │  • PERCEIVE: Dump accessibility tree                            │  │
│    │    Current app: Facebook                                        │  │
│    │    Elements: [News feed, Search, Friends, ...]                  │  │
│    │  • REASON: LLM analyzes screen                                  │  │
│    │    "I'm on Facebook. Need to check news. Scroll to see posts."  │  │
│    │  • ACT: Execute scroll(DOWN)                                    │  │
│    │    Result: News feed scrolled                                   │  │
│    └─────────────────────────────────────────────────────────────────┘  │
│                           │                                              │
│                           ▼                                              │
│    ┌─────────────────────────────────────────────────────────────────┐  │
│    │ ITERATION 3:                                                    │  │
│    │  • PERCEIVE: Dump accessibility tree                            │  │
│    │  • REASON: "Goal achieved. News visible on screen."             │  │
│    │  • ACT: done("News fetched successfully")                       │  │
│    └─────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│    Loop iterations: 3                                                    │
│    Total loop time: ~3.5s                                               │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 7. RESPOND: TTS.speak()                                                  │
│    Output: "تمام، جاري عرض أخبار الفيسبوك"                               │
│    Latency: <500ms                                                       │
└──────────────────────────────────────────────────────────────────────────┘

TOTAL SLOW PATH LATENCY: ~5.0 seconds (acceptable for complex tasks)
```

---

## Routing Logic

### Decision Tree

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ROUTING DECISION TREE                                │
└─────────────────────────────────────────────────────────────────────────────┘

                              User Command
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │ FunctionGemma Classification │
                    └──────────────┬───────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │  Intent Type + Confidence    │
                    └──────────────┬───────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
     ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
     │ Intent Type     │  │ Confidence      │  │ UI Context      │
     │ in FAST_LIST?   │  │ >= 0.85?        │  │ Required?       │
     └────────┬────────┘  └────────┬────────┘  └────────┬────────┘
              │                    │                    │
         ┌────┴────┐          ┌────┴────┐          ┌────┴────┐
         │         │          │         │          │         │
        YES       NO        YES       NO        NO       YES
         │         │          │         │          │         │
         │         │          │         │          │         │
         ▼         │          ▼         │          │         │
    ┌────────┐     │     ┌────────┐    │          │         │
    │ Check  │     │     │ FAST   │    │          │         │
    │ UI     │     │     │ PATH   │    │          │         │
    │ Needed?│     │     │        │    │          │         │
    └───┬────┘     │     └────────┘    │          │         │
        │          │                   │          │         │
   ┌────┴────┐     │                   │          │         │
   │         │     │                   │          │         │
  NO        YES    │                   │          │         │
   │         │     │                   │          │         │
   │         │     │                   │          │         │
   ▼         │     │                   │          │         │
┌────────┐   │     │                   │          │         │
│ FAST   │   │     │                   │          │         │
│ PATH   │   │     │                   │          │         │
└────────┘   │     │                   │          │         │
             │     │                   │          │         │
             │     ▼                   ▼          ▼         ▼
             │  ┌────────────────────────────────────────────┐
             │  │            SLOW PATH                       │
             │  │  (UI Navigation + Perception Loop)         │
             │  └────────────────────────────────────────────┘
             │
             ▼
        ┌─────────────────┐
        │ Confidence      │
        │ < 0.70?         │
        └────────┬────────┘
                 │
            ┌────┴────┐
            │         │
           YES       NO
            │         │
            │         ▼
            │     ┌─────────────────┐
            │     │ Check intent    │
            │     │ type complexity │
            │     └────────┬────────┘
            │              │
            │         ┌────┴────┐
            │         │         │
            │        COMPLEX  SIMPLE
            │         │         │
            │         │         │
            │         ▼         ▼
            │     ┌────────┐ ┌────────┐
            │     │ SLOW   │ │ FAST   │
            │     │ PATH   │ │ PATH   │
            │     └────────┘ └────────┘
            │
            ▼
        ┌─────────────────┐
        │ SLOW PATH       │
        │ (UI Navigation) │
        └─────────────────┘
```

### Routing Rules Table

| Condition | Rule | Path | Example |
|-----------|------|------|---------|
| Intent in FAST_LIST | AND confidence >= 0.85 | FAST | "اتصل بماما" (0.97) |
| Intent in FAST_LIST | BUT confidence < 0.70 | SLOW | "اتصل بـ..." (0.65, unclear contact) |
| Intent = UI_NAVIGATION | Always | SLOW | "شوف الأخبار" |
| Intent = UNKNOWN | Always | SLOW | "اعمل اللي انت عايزه" |
| Multi-step required | Always | SLOW | "احجز أوبر" |
| App-specific UI | Always | SLOW | "ابعت واتساب" (contact unclear) |

### FAST_LIST (Intent Types for Fast Path)

```kotlin
val FAST_PATH_INTENTS = setOf(
    IntentType.CALL_CONTACT,      // Clear contact name
    IntentType.SEND_WHATSAPP,     // Clear contact + message
    IntentType.SET_ALARM,         // Clear time
    IntentType.OPEN_APP,          // Clear app name
    IntentType.EMERGENCY,         // Always fast
    IntentType.READ_TIME,         // Simple query
    IntentType.TOGGLE_WIFI,       // Device control
    IntentType.TOGGLE_BLUETOOTH,  // Device control
    IntentType.TOGGLE_FLASHLIGHT  // Device control
)
```

---

## Fallback Mechanisms

### Multi-Level Fallback Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FALLBACK HIERARCHY                                   │
└─────────────────────────────────────────────────────────────────────────────┘

Level 1: FunctionGemma Primary (Fast Path)
         │
         │ Confidence < 0.85?
         │ OR UI navigation required?
         ▼
Level 2: UI Navigation (Slow Path)
         │
         │ Accessibility tree empty?
         │ OR stuck for 3 iterations?
         ▼
Level 3: Vision Fallback
         │
         │ Screenshot analysis fails?
         │ OR action execution fails?
         ▼
Level 4: Clarification Request
         │
         │ Ask user: "تقصد إيه بالظبط؟"
         │ (What exactly do you mean?)
         ▼
Level 5: Human Escalation (Future)
         │
         │ Offer: "تحب أكلم حد يساعدك؟"
         │ (Would you like me to call someone?)
```

### Failure Mode Handling

| Failure Mode | Detection | Recovery |
|--------------|-----------|----------|
| **Stuck Loop** | Screen unchanged for 3 steps | Inject recovery hints, try alternative action |
| **Repetition** | Same coordinates tapped 3+ times | Tell LLM to try different approach |
| **Drift** | Navigation spam without interaction | Nudge to take direct action |
| **Empty Tree** | Accessibility tree has 0 elements | Switch to vision fallback |
| **Action Failure** | Action returns error | Retry with modified parameters |
| **Timeout** | Loop exceeds 10 iterations | Abort, ask for clarification |

### Vision Fallback

When accessibility tree is empty (Flutter, WebView, games):

```kotlin
data class VisionFallback(
    val screenshotPath: String,
    val boundingBoxes: List<BoundingBox>,
    val detectedElements: List<VisionElement>
)

data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val label: String,
    val confidence: Float
)
```

**Process:**
1. Capture screenshot
2. Run on-device object detection (TensorFlow Lite)
3. Detect buttons, text fields, icons
4. Generate coordinate-based tap suggestions
5. LLM selects best coordinate to tap

---

## Performance Targets

### Latency Budget

| Stage | Fast Path | Slow Path |
|-------|-----------|-----------|
| Wake Word Detection | <200ms | <200ms |
| ASR (Whisper) | <800ms | <800ms |
| Normalization | <10ms | <10ms |
| Intent Classification | <280ms | <350ms |
| Routing Decision | <50ms | <50ms |
| Execution | <200ms | 2-5s (loop) |
| TTS Response | <500ms | <500ms |
| **TOTAL** | **<2.0s** | **<5.0s** |

### Memory Budget

| Component | Baseline | With UI Navigation |
|-----------|----------|-------------------|
| Wake Word Detector | 20MB | 20MB |
| ASR Engine | 100MB | 100MB |
| FunctionGemma | 200MB | 200MB |
| Accessibility Parser | - | 50MB |
| UI Navigation Engine | - | 80MB |
| TTS Engine | 30MB | 30MB |
| UI Components | 50MB | 50MB |
| System Overhead | 50MB | 70MB |
| **TOTAL** | **450MB** | **600MB** |

**Target:** <800MB (200MB headroom)

### Battery Budget

| Component | Drain/Hour (Baseline) | Drain/Hour (With UI Nav) |
|-----------|----------------------|-------------------------|
| Wake Word Detection | 2-3% | 2-3% |
| Background Service | 1% | 1% |
| ASR Processing | 0.5% per use | 0.5% per use |
| Inference | 0.5% per use | 0.5% per use |
| UI Navigation | - | 1-2% per use |
| **TOTAL (idle)** | **3-4%** | **4-5%** |

### Accuracy Targets

| Metric | Baseline | Target (Hybrid) |
|--------|----------|-----------------|
| Intent Accuracy | 95.2% | Maintain 95.2% |
| UI Navigation Success | N/A | >90% |
| Routing Accuracy | N/A | >95% |
| Overall Task Success | 95.2% | >93% |

---

## Integration Strategy

### Phase 1: Architecture Design (Week 1)
- [x] Study DroidClaw architecture
- [x] Create HYBRID_ARCHITECTURE.md
- [ ] Review with team
- [ ] Finalize component interfaces

### Phase 2: Core Implementation (Weeks 2-4)
- [ ] AccessibilityTreeParser
- [ ] UINavigationEngine (28 actions)
- [ ] Enhanced HybridOrchestrator
- [ ] Enhanced FunctionGemma integration

### Phase 3: Workflow Engine (Week 5)
- [ ] YAML parser
- [ ] Workflow executor
- [ ] Pre-built workflows (3)
- [ ] Voice workflow creation

### Phase 4: Egyptian Optimization (Week 6)
- [ ] Egyptian UI navigation dataset (100+ examples)
- [ ] Dialect-specific prompts
- [ ] Cultural context integration

### Phase 5: Testing & Validation (Week 7)
- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance benchmarks
- [ ] Accuracy testing

### Phase 6: Documentation & Deployment (Week 8)
- [ ] Update README.md
- [ ] User manual (Arabic + English)
- [ ] API reference
- [ ] Migration guide

---

## Testing Strategy

### Unit Tests

| Component | Test Coverage Target |
|-----------|---------------------|
| AccessibilityTreeParser | >90% |
| UINavigationEngine | >85% |
| HybridOrchestrator | >95% |
| WorkflowEngine | >90% |

### Integration Tests

1. **Fast Path Integration**
   - Verify no regression in existing functionality
   - Measure latency (must be <2.0s)
   - Verify accuracy (must be >95%)

2. **Slow Path Integration**
   - Test perception → reason → action loop
   - Verify fallback mechanisms
   - Measure success rate (must be >90%)

3. **Hybrid Routing**
   - Test routing decisions for 100+ commands
   - Verify correct path selection
   - Measure routing latency (must be <100ms)

### Performance Benchmarks

| Benchmark | Target | Measurement |
|-----------|--------|-------------|
| Cold Start | <6s | App launch to ready |
| Warm Start | <2s | Wake word to listening |
| Fast Path End-to-End | <2.0s | Command to action |
| Slow Path End-to-End | <5.0s | Complex task completion |
| Memory Peak | <800MB | During UI navigation |
| Battery Drain | <5%/hour | Idle + active usage |

### Egyptian Dialect Test Suite

**Dataset:** `datasets/egyptian_ui_navigation/`

| Category | Examples | Target Accuracy |
|----------|----------|-----------------|
| Facebook Navigation | 20 | >90% |
| WhatsApp UI | 25 | >92% |
| Uber Booking | 15 | >88% |
| Settings Navigation | 20 | >90% |
| General UI | 20 | >90% |
| **Total** | **100+** | **>90%** |

---

## Migration Guide

### For Existing Users

**No Breaking Changes:**
- Existing intent-based system unchanged
- All current commands work identically
- No configuration changes required

**New Capabilities:**
- Complex UI navigation commands now supported
- Workflow automation available
- Enhanced fallback mechanisms

### For Developers

**API Changes:**

1. **FunctionGemmaIntentEngine** - Enhanced output
```kotlin
// Old (still supported)
val result = engine.classifyIntent(text)
val intentType = result.intentType

// New (enhanced)
val enhancedResult = engine.classifyIntentEnhanced(text)
val requiresUI = enhancedResult.requiresUINavigation
val uiContext = enhancedResult.uiContext
```

2. **HybridOrchestrator** - New routing
```kotlin
// Old
val intent = orchestrator.determineIntent(command)

// New
val result = orchestrator.processCommand(command)
when (result.path) {
    RoutingPath.FAST -> executeFastPath(result)
    RoutingPath.SLOW -> executeSlowPath(result)
}
```

3. **New Components**
```kotlin
// Accessibility Tree Parser
val parser = AccessibilityTreeParser(service)
val tree = parser.parseCurrentTree()

// UI Navigation Engine
val navEngine = UINavigationEngine(service)
navEngine.navigateToGoal("افتح الفيسبوك")

// Workflow Engine
val workflowEngine = WorkflowEngine()
val workflow = workflowEngine.loadWorkflow(yamlContent)
workflowEngine.executeWorkflow(workflow)
```

### Configuration Changes

**New Config Options:**
```kotlin
// HybridOrchestrator config
data class HybridConfig(
    val fastPathConfidenceThreshold: Float = 0.85f,
    val slowPathConfidenceThreshold: Float = 0.70f,
    val maxUINavigationIterations: Int = 10,
    val stuckThreshold: Int = 3,
    val enableVisionFallback: Boolean = true,
    val maxElementsForLLM: Int = 40
)
```

---

## Success Criteria

### Functional Criteria

- [ ] User can say "افتح الفيسبوك وشوف الأخبار" → System navigates Facebook UI
- [ ] User can say "احجز أوبر" → System completes Uber booking flow
- [ ] Hybrid routing works seamlessly (<100ms decision time)
- [ ] No regression in existing intent accuracy (maintain 95.2%)
- [ ] UI navigation achieves >90% success rate on common tasks

### Performance Criteria

- [ ] Memory usage stays under 800MB
- [ ] Battery drain stays under 5%/hour
- [ ] Fast path latency <2.0s
- [ ] Slow path latency <5.0s
- [ ] Cold start <6s

### Quality Criteria

- [ ] All unit tests pass (>90% coverage)
- [ ] All integration tests pass
- [ ] Performance benchmarks meet targets
- [ ] Documentation complete (README, user manual, API reference)
- [ ] Egyptian dialect dataset created (100+ examples)

---

## File Manifest

### New Files Created

| File | Purpose | Status |
|------|---------|--------|
| `docs/architecture/HYBRID_ARCHITECTURE.md` | Architecture document | ✅ Complete |
| `app/src/main/java/.../accessibility/ui/AccessibilityTreeParser.kt` | Tree parser | 🔄 Pending |
| `app/src/main/java/.../accessibility/ui/UIElement.kt` | UI element models | 🔄 Pending |
| `app/src/main/java/.../navigation/UINavigationEngine.kt` | UI navigation | 🔄 Pending |
| `app/src/main/java/.../navigation/UIActions.kt` | Action definitions | 🔄 Pending |
| `app/src/main/java/.../hybrid/HybridOrchestrator.kt` | Enhanced orchestrator | 🔄 Pending |
| `app/src/main/java/.../workflow/WorkflowEngine.kt` | Workflow executor | 🔄 Pending |
| `app/src/main/java/.../workflow/WorkflowParser.kt` | YAML parser | 🔄 Pending |
| `datasets/egyptian_ui_navigation/train.jsonl` | Training data | 🔄 Pending |
| `datasets/egyptian_ui_navigation/test.jsonl` | Test data | 🔄 Pending |
| `app/src/test/java/.../AccessibilityTreeParserTest.kt` | Parser tests | 🔄 Pending |
| `app/src/test/java/.../UINavigationEngineTest.kt` | Navigation tests | 🔄 Pending |
| `app/src/test/java/.../HybridOrchestratorTest.kt` | Orchestrator tests | 🔄 Pending |

### Modified Files

| File | Modification | Status |
|------|--------------|--------|
| `app/src/main/java/.../ai/FunctionGemmaIntentEngine.kt` | Enhanced output | 🔄 Pending |
| `app/src/main/java/.../hybrid/HybridOrchestrator.java` | Replace with Kotlin | 🔄 Pending |
| `README.md` | Update with hybrid capabilities | 🔄 Pending |
| `docs/guides/user_manual_ar.md` | Add UI navigation section | 🔄 Pending |

---

## Appendix A: Egyptian Dialect Examples

### UI Navigation Commands

| Egyptian Command | English Translation | Target Action |
|------------------|---------------------|---------------|
| "افتح الفيسبوك وشوف الأخبار" | Open Facebook, check news | Navigate Facebook UI |
| "ابعت رسالة واتساب لـ أحمد" | Send WhatsApp to Ahmed | Navigate WhatsApp UI |
| "احجز أوبر من البيت للشغل" | Book Uber from home to work | Multi-step Uber flow |
| "شوف إيه الجديد على انستجرام" | Check what's new on Instagram | Navigate Instagram |
| "اكتب بوست على الفيسبوك" | Write a Facebook post | Navigate + type |
| "رد على آخر رسالة" | Reply to last message | Navigate + type |
| "ابعت صورة على الواتساب" | Send photo on WhatsApp | Navigate + attach |
| "شوف حالة الواتساب" | Check WhatsApp status | Navigate UI |
| "اعمل ريستارت للموبايل" | Restart the phone | System action |
| "نضف الذاكرة" | Clean memory | System action |

### Workflow Commands

| Egyptian Command | English Translation | Workflow |
|------------------|---------------------|----------|
| "روتين الصباح" | Morning routine | Weather + news + message |
| "رجوع البيت" | Coming home | WiFi + AC + lights |
| "وقت النوم" | Bedtime | Alarms + lights off + DND |
| "وقت الشغل" | Work time | Open apps + silence |
| "الجمعة" | Friday | Family calls + news |

---

## Appendix B: LLM System Prompt

### Full System Prompt for UI Navigation

```
<|start_header_id|>system<|end_header_id|>

You are an Egyptian Arabic voice assistant controlling an Android device.
You have TWO modes of operation:

## MODE 1: FAST PATH (Intent Classification)
For clear, common commands:
- Calls: "اتصل بماما"
- WhatsApp: "ابعت واتساب لأحمد"
- Alarms: "نبهني بكرة الصبح"
- Apps: "افتح الواتساب"
- Settings: "افتح الواي فاي"

Output format:
{
  "mode": "fast",
  "intent_type": "call_contact",
  "confidence": 0.97,
  "entities": {"contact_name": "ماما"},
  "requires_ui_navigation": false
}

## MODE 2: SLOW PATH (UI Navigation)
For complex tasks requiring UI interaction:
- "افتح الفيسبوك وشوف الأخبار"
- "احجز أوبر من البيت للشغل"
- "ابعت رسالة واتساب" (contact unclear)

Output format:
{
  "mode": "slow",
  "intent_type": "ui_navigation",
  "confidence": 0.85,
  "entities": {},
  "requires_ui_navigation": true,
  "ui_context": {
    "target_app": "com.facebook.katana",
    "expected_elements": ["news feed", "posts"],
    "is_multi_step": true
  },
  "reasoning": "Complex task requiring UI navigation"
}

## DECISION RULES
1. IF intent is clear AND confidence >= 0.85 → FAST PATH
2. IF intent is vague OR confidence < 0.70 → SLOW PATH
3. IF task requires UI interaction → SLOW PATH
4. IF multi-step workflow needed → SLOW PATH

## EGYPTIAN DIALECT SUPPORT
Understand Egyptian expressions:
- "ماما/بابا" → Mother/Father
- "إزايك" → How are you
- "النهاردة" → Today
- "بكرة" → Tomorrow
- "دلوقتي" → Now

## OUTPUT REQUIREMENTS
- JSON format only
- No explanations outside JSON
- Egyptian Arabic entity values
- Confidence scores 0.0-1.0

<|eot_id|>
```

---

**Document Version:** 3.0.0
**Last Updated:** March 14, 2026
**Next Review:** April 14, 2026
**Maintained By:** EgyptianAgent Technical Team
**Status:** 🚀 Implementation Ready
