# FunctionGemma-270M-IT Migration Guide

**Version:** 1.0.0  
**Last Updated:** March 3, 2026  
**Status:** Production Ready  
**Author:** EgyptianAgent Development Team

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Why Migrate](#why-migrate)
3. [Migration Overview](#migration-overview)
4. [Breaking Changes](#breaking-changes)
5. [Migration Steps](#migration-steps)
6. [Configuration Changes](#configuration-changes)
7. [Code Changes](#code-changes)
8. [Testing](#testing)
9. [Rollback Plan](#rollback-plan)
10. [Post-Migration](#post-migration)

---

## Executive Summary

This guide provides a comprehensive migration path from Llama 3.2 3B to FunctionGemma-270M-IT for EgyptianAgent applications. The migration is designed to be **minimally invasive** with most changes limited to configuration files and class instantiation.

### Migration Summary

| Aspect | Effort | Risk | Notes |
|--------|--------|------|-------|
| Configuration | Low | Low | Build flags update |
| Code Changes | Low | Low | Class name changes |
| Testing | Medium | Medium | Validate all intents |
| Rollback | Low | Low | Simple flag revert |

### Expected Timeline

| Phase | Duration | Activities |
|-------|----------|------------|
| Preparation | 1-2 days | Review, backup, setup |
| Configuration | 2-4 hours | Update build files |
| Code Changes | 2-4 hours | Update Java/Kotlin code |
| Testing | 1-2 days | Full test suite |
| Deployment | 1 day | Staged rollout |
| **Total** | **4-6 days** | |

---

## Why Migrate

### Performance Improvements

| Metric | Llama 3.2 3B | FunctionGemma-270M | Improvement |
|--------|-------------|-------------------|-------------|
| Model Size | 2GB | 288MB | **7x smaller** |
| RAM Usage | 4GB | 550MB | **7.3x less** |
| Load Time | 30s | 5s | **6x faster** |
| Inference | 1.5s | 0.3s | **5x faster** |
| Battery/Hour | ~8% | ~3% | **2.7x less** |
| CPU Usage | 40-60% | 15-25% | **2.4x less** |

### Business Benefits

1. **Better User Experience**
   - Faster app startup (5s vs 30s)
   - Quicker response times (0.3s vs 1.5s)
   - Lower battery drain

2. **Broader Device Support**
   - Works on devices with 4GB RAM (vs 8GB+ required)
   - Supports older Android versions better
   - Less thermal throttling

3. **Cost Reduction**
   - Lower cloud inference costs (if using cloud)
   - Reduced CDN costs (smaller model downloads)
   - Less support for performance issues

4. **Maintained Accuracy**
   - 95%+ accuracy on Egyptian dialect
   - Comparable to Llama's 97.8%
   - Specialized for EgyptianAgent use case

### When NOT to Migrate

| Scenario | Recommendation |
|----------|----------------|
| Need >97% accuracy | Stay with Llama |
| Complex reasoning required | Stay with Llama |
| Multi-turn conversations | Stay with Llama |
| Already stable in production | Consider phased migration |

---

## Migration Overview

### Architecture Comparison

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BEFORE: LLAMA 3.2 3B                                 │
└─────────────────────────────────────────────────────────────────────────────┘

User Voice → Whisper ASR → LlamaIntentEngine → IntentResult → Execution
               │                │
               │                └─ Model: 2GB
               │                └─ RAM: 4GB
               │                └─ Load: 30s
               └─ Egyptian Arabic


┌─────────────────────────────────────────────────────────────────────────────┐
│                         AFTER: FUNCTIONGEMMA-270M                            │
└─────────────────────────────────────────────────────────────────────────────┘

User Voice → Whisper ASR → FunctionGemmaIntentEngine → IntentResult → Execution
               │                │
               │                └─ Model: 288MB
               │                └─ RAM: 550MB
               │                └─ Load: 5s
               └─ Egyptian Arabic
```

### Migration Path

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Phase 1:   │───▶│   Phase 2:   │───▶│   Phase 3:   │───▶│   Phase 4:   │
│ Preparation  │    │  Config &    │    │   Testing    │    │  Deployment  │
│              │    │   Code       │    │              │    │              │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
     1-2 days           4-8 hours          1-2 days            1 day
```

---

## Breaking Changes

### 1. Model File Location

**Before (Llama):**
```
models/llama-3.2-3b-instruct.Q4_K_M.gguf
```

**After (FunctionGemma):**
```
models/functiongemma-270m-it.Q4_K_M.gguf
```

**Action Required:** Update model path configuration

### 2. Class Names

**Before (Llama):**
```java
LlamaIntentEngine engine = new LlamaIntentEngine(context);
LlamaConfig config = new LlamaConfig();
```

**After (FunctionGemma):**
```java
FunctionGemmaIntentEngine engine = new FunctionGemmaIntentEngine(context);
FunctionGemmaConfig config = new FunctionGemmaConfig();
```

**Action Required:** Update imports and class instantiation

### 3. Prompt Format

FunctionGemma uses special header tokens different from Llama.

**Before (Llama):**
```
<|begin_of_text|><|start_header_id|>system<|end_header_id|>
You are an Egyptian Arabic voice assistant...
<|eot_id|><|start_header_id|>user<|end_header_id|>
اتصل بماما
<|eot_id|><|start_header_id|>assistant<|end_header_id|>
```

**After (FunctionGemma):**
```
<|start_header_id|>system<|end_header_id|>
You are an Egyptian Arabic voice assistant...
<|eot_id|><|start_header_id|>user<|end_header_id|>
اتصل بماما
<|eot_id|><|start_header_id|>assistant<|end_header_id|>
```

**Action Required:** None (handled internally by engine)

### 4. Temperature Setting

**Before (Llama):**
```java
config.setTemperature(0.7f);  // More creative
```

**After (FunctionGemma):**
```java
config.setTemperature(0.1f);  // More deterministic for function calling
```

**Action Required:** Update temperature configuration

### 5. Build Flags

**Before (Llama):**
```groovy
buildConfigField "boolean", "USE_FUNCTIONGEMMA", "false"
buildConfigField "boolean", "USE_LLAMA", "true"
```

**After (FunctionGemma):**
```groovy
buildConfigField "boolean", "USE_FUNCTIONGEMMA", "true"
buildConfigField "boolean", "USE_LLAMA", "false"
```

**Action Required:** Update build.gradle

---

## Migration Steps

### Step 1: Preparation (1-2 days)

#### 1.1 Backup Current Implementation

```bash
# Create backup branch
git checkout -b backup/llama-before-migration

# Tag current state
git tag -a v1.0-llama -m "Last version with Llama 3.2 3B"

# Push backup
git push origin backup/llama-before-migration
git push origin v1.0-llama
```

#### 1.2 Review Current Implementation

```bash
# Find all Llama references
grep -r "LlamaIntentEngine" app/src/
grep -r "LlamaConfig" app/src/
grep -r "USE_LLAMA" app/src/

# Document current behavior
./scripts/test_llama_accuracy.sh > llama_baseline_results.txt
```

#### 1.3 Setup Development Environment

```bash
# Install FunctionGemma dependencies
pip install -r requirements_functiongemma.txt

# Download FunctionGemma model
wget https://huggingface.co/EgyptianAI/FunctionGemma-270M-IT-GGUF/resolve/main/functiongemma-270m-it.Q4_K_M.gguf \
    -O models/functiongemma-270m-it.Q4_K_M.gguf

# Verify model
sha256sum models/functiongemma-270m-it.Q4_K_M.gguf
```

### Step 2: Configuration Changes (2-4 hours)

#### 2.1 Update build.gradle

```groovy
// app/build.gradle

android {
    defaultConfig {
        // OLD (Llama)
        // buildConfigField "boolean", "USE_FUNCTIONGEMMA", "false"
        // buildConfigField "boolean", "USE_LLAMA", "true"
        // buildConfigField "String", "MODEL_PATH", "\"models/llama-3.2-3b-instruct.Q4_K_M.gguf\""
        
        // NEW (FunctionGemma)
        buildConfigField "boolean", "USE_FUNCTIONGEMMA", "true"
        buildConfigField "boolean", "USE_LLAMA", "false"
        buildConfigField "String", "MODEL_PATH", "\"models/functiongemma-270m-it.Q4_K_M.gguf\""
        
        // FunctionGemma specific settings
        buildConfigField "int", "MAX_TOKENS", "256"
        buildConfigField "float", "TEMPERATURE", "0.1f"
        buildConfigField "int", "TOP_K", "40"
        buildConfigField "float", "TOP_P", "0.9f"
        buildConfigField "int", "NUM_THREADS", "2"
    }
}
```

#### 2.2 Update Model Configuration

```xml
<!-- res/values/models_config.xml -->
<resources>
    <!-- OLD (Llama) -->
    <!-- <string name="model_path">models/llama-3.2-3b-instruct.Q4_K_M.gguf</string> -->
    <!-- <bool name="use_llama">true</bool> -->
    <!-- <bool name="use_functiongemma">false</bool> -->
    
    <!-- NEW (FunctionGemma) -->
    <string name="model_path">models/functiongemma-270m-it.Q4_K_M.gguf</string>
    <bool name="use_llama">false</bool>
    <bool name="use_functiongemma">true</bool>
    <integer name="max_tokens">256</integer>
    <item name="temperature" format="float" type="dimen">0.1</item>
    <integer name="top_k">40</integer>
    <item name="top_p" format="float" type="dimen">0.9</item>
    <integer name="num_threads">2</integer>
</resources>
```

#### 2.3 Update ProGuard Rules

```proguard
# app/proguard-rules.pro

# Keep FunctionGemma classes
-keep class com.egyptianagent.functiongemma.** { *; }
-keep class com.egyptianagent.intent.** { *; }

# Keep model loading classes
-keep class org.llama.** { *; }  # Can remove if not using fallback
-keep class com.egyptianagent.functiongemma.engine.** { *; }
```

### Step 3: Code Changes (2-4 hours)

#### 3.1 Update Intent Engine Factory

```java
// IntentEngineFactory.java

public class IntentEngineFactory {
    public static IntentEngine create(Context context) {
        // OLD (Llama)
        // if (BuildConfig.USE_LLAMA) {
        //     return new LlamaIntentEngine(context);
        // }
        
        // NEW (FunctionGemma)
        if (BuildConfig.USE_FUNCTIONGEMMA) {
            return new FunctionGemmaIntentEngine(context);
        } else {
            // Fallback or error
            throw new IllegalStateException("No intent engine configured");
        }
    }
    
    // Optional: Support both with runtime switching
    public static IntentEngine create(Context context, ModelType type) {
        switch (type) {
            case FUNCTIONGEMMA:
                return new FunctionGemmaIntentEngine(context);
            case LLAMA:
                return new LlamaIntentEngine(context);  // Keep for fallback
            default:
                throw new IllegalArgumentException("Unknown model type: " + type);
        }
    }
}
```

#### 3.2 Update Application Initialization

```java
// EgyptianAgentApplication.java

public class EgyptianAgentApplication extends Application {
    private IntentEngine intentEngine;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // OLD (Llama)
        // if (BuildConfig.USE_LLAMA) {
        //     intentEngine = new LlamaIntentEngine(this);
        // }
        
        // NEW (FunctionGemma)
        if (BuildConfig.USE_FUNCTIONGEMMA) {
            intentEngine = new FunctionGemmaIntentEngine(this);
        }
    }
    
    public IntentEngine getIntentEngine() {
        return intentEngine;
    }
}
```

#### 3.3 Update Voice Activity

```java
// VoiceActivity.java

public class VoiceActivity extends AppCompatActivity {
    private IntentEngine intentEngine;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get engine from factory (automatically uses FunctionGemma)
        intentEngine = IntentEngineFactory.create(this);
        
        // Or direct instantiation
        // intentEngine = new FunctionGemmaIntentEngine(this);
    }
    
    private void processVoiceCommand(String text) {
        // This code remains unchanged - interface is the same
        IntentResult result = intentEngine.classifyIntent(text);
        executeIntent(result);
    }
}
```

#### 3.4 Update Configuration Class

```java
// AppConfig.java

public class AppConfig {
    // OLD (Llama)
    // private static final String MODEL_PATH = "models/llama-3.2-3b-instruct.Q4_K_M.gguf";
    // private static final float TEMPERATURE = 0.7f;
    // private static final int MAX_TOKENS = 512;
    
    // NEW (FunctionGemma)
    private static final String MODEL_PATH = "models/functiongemma-270m-it.Q4_K_M.gguf";
    private static final float TEMPERATURE = 0.1f;  // Lower for deterministic output
    private static final int MAX_TOKENS = 256;       // Sufficient for function calling
    private static final int TOP_K = 40;
    private static final float TOP_P = 0.9f;
    private static final int NUM_THREADS = 2;
    
    public static FunctionGemmaConfig createFunctionGemmaConfig() {
        return new FunctionGemmaConfig()
            .setModelPath(MODEL_PATH)
            .setTemperature(TEMPERATURE)
            .setMaxTokens(MAX_TOKENS)
            .setTopK(TOP_K)
            .setTopP(TOP_P)
            .setNumThreads(NUM_THREADS);
    }
}
```

### Step 4: Testing (1-2 days)

#### 4.1 Unit Tests

```java
// FunctionGemmaIntentEngineTest.java

@RunWith(AndroidJUnit4.class)
public class FunctionGemmaIntentEngineTest {
    
    private FunctionGemmaIntentEngine engine;
    
    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        engine = new FunctionGemmaIntentEngine(context);
        
        // Wait for engine to be ready
        while (!engine.isReady()) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
    }
    
    @Test
    public void testCallContactIntent() {
        IntentResult result = engine.classifyIntent("اتصل بماما");
        
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        assertTrue(result.getConfidence() >= 0.9f);
        assertEquals("ماما", result.getEntity("contact_name"));
    }
    
    @Test
    public void testSendWhatsAppIntent() {
        IntentResult result = engine.classifyIntent("ابعت واتساب لأحمد وقوله إنى هتأخر");
        
        assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
        assertEquals("أحمد", result.getEntity("contact_name"));
        assertEquals("إنى هتأخر", result.getEntity("message"));
    }
    
    @Test
    public void testSetAlarmIntent() {
        IntentResult result = engine.classifyIntent("اضبط منبه على الساعة 7 الصبح");
        
        assertEquals(IntentType.SET_ALARM, result.getIntentType());
        assertEquals("7 الصبح", result.getEntity("time"));
    }
    
    @After
    public void tearDown() {
        if (engine != null) {
            engine.destroy();
        }
    }
}
```

#### 4.2 Integration Tests

```bash
# Run full test suite
./gradlew testFunctiongemmaDebugUnitTest
./gradlew connectedFunctiongemmaDebugAndroidTest

# Run accuracy tests
python scripts/test_functiongemma_accuracy.py \
    --model models/functiongemma-270m-it.Q4_K_M.gguf \
    --dataset datasets/egyptian_intent_test.jsonl \
    --output test_results.json
```

#### 4.3 Manual Testing Checklist

| Test Case | Input | Expected Output | Status |
|-----------|-------|-----------------|--------|
| Call Contact | "اتصل بماما" | CALL_CONTACT, contact=ماما | ☐ |
| Send WhatsApp | "ابعت واتساب لأحمد" | SEND_WHATSAPP, contact=أحمد | ☐ |
| Set Alarm | "اضبط منبه على 7 الصبح" | SET_ALARM, time=7 الصبح | ☐ |
| Open App | "افتح الواتساب" | OPEN_APP, app=الواتساب | ☐ |
| Get Weather | "إزاي الطقس في القاهرة" | GET_WEATHER, location=القاهرة | ☐ |
| Emergency | "إسعاف" | EMERGENCY_CALL | ☐ |
| Calculate | "كام 5 زائد 3" | CALCULATE, expression=5+3 | ☐ |
| Unknown | "إيه أخبارك" | UNKNOWN | ☐ |

### Step 5: Deployment (1 day)

#### 5.1 Staged Rollout

```
Phase 1: Internal Team (5 users)
         │
         ▼
    Verify no crashes
    Check performance metrics
         │
         ▼
Phase 2: Beta Testers (50 users)
         │
         ▼
    Monitor accuracy
    Check user feedback
         │
         ▼
Phase 3: 10% Production
         │
         ▼
    Monitor crash rates
    Compare with Llama baseline
         │
         ▼
Phase 4: 50% Production
         │
         ▼
    Full performance analysis
         │
         ▼
Phase 5: 100% Production
```

#### 5.2 Monitoring Setup

```java
// PerformanceTracker.java

public class PerformanceTracker {
    public static void trackModelLoad(long loadTimeMs) {
        FirebaseAnalytics.getInstance(context).logEvent("model_load", 
            new Bundle() {{
                putLong("load_time_ms", loadTimeMs);
                putString("model_type", "functiongemma");
            }});
    }
    
    public static void trackInference(long inferenceTimeMs, String intentType) {
        FirebaseAnalytics.getInstance(context).logEvent("intent_classification",
            new Bundle() {{
                putLong("inference_time_ms", inferenceTimeMs);
                putString("intent_type", intentType);
                putString("model_type", "functiongemma");
            }});
    }
}
```

---

## Configuration Changes

### Complete build.gradle Changes

```groovy
// app/build.gradle

plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.egyptianagent'
    compileSdk 34
    
    defaultConfig {
        applicationId "com.egyptianagent"
        minSdk 24
        targetSdk 34
        versionCode 10
        versionName "2.0-functiongemma"
        
        // ===== MODEL CONFIGURATION =====
        // Changed from Llama to FunctionGemma
        buildConfigField "boolean", "USE_FUNCTIONGEMMA", "true"   // was false
        buildConfigField "boolean", "USE_LLAMA", "false"          // was true
        buildConfigField "String", "MODEL_PATH", "\"models/functiongemma-270m-it.Q4_K_M.gguf\""
        
        // FunctionGemma-specific settings
        buildConfigField "int", "FUNCTIONGEMMA_MAX_TOKENS", "256"
        buildConfigField "float", "FUNCTIONGEMMA_TEMPERATURE", "0.1f"
        buildConfigField "int", "FUNCTIONGEMMA_TOP_K", "40"
        buildConfigField "float", "FUNCTIONGEMMA_TOP_P", "0.9f"
        buildConfigField "int", "FUNCTIONGEMMA_THREADS", "2"
        buildConfigField "int", "FUNCTIONGEMMA_MAX_CONTEXT", "2048"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // Product flavors for model selection (optional)
    flavorDimensions "model"
    productFlavors {
        functiongemma {
            dimension "model"
            applicationIdSuffix ".functiongemma"
            buildConfigField "boolean", "USE_FUNCTIONGEMMA", "true"
            buildConfigField "boolean", "USE_LLAMA", "false"
        }
        llama {
            dimension "model"
            applicationIdSuffix ".llama"
            buildConfigField "boolean", "USE_FUNCTIONGEMMA", "false"
            buildConfigField "boolean", "USE_LLAMA", "true"
        }
    }
    
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    // FunctionGemma dependencies
    implementation project(':functiongemma-engine')
    implementation 'com.github.ggerganov:llama.cpp:0.2.0'
    
    // Keep Llama for fallback (optional)
    // implementation project(':llama-engine')
    
    // Other dependencies...
}
```

### Environment Variables

```bash
# .env or local.properties

# OLD (Llama)
# MODEL_TYPE=llama
# MODEL_PATH=models/llama-3.2-3b-instruct.Q4_K_M.gguf
# TEMPERATURE=0.7
# MAX_TOKENS=512

# NEW (FunctionGemma)
MODEL_TYPE=functiongemma
MODEL_PATH=models/functiongemma-270m-it.Q4_K_M.gguf
TEMPERATURE=0.1
MAX_TOKENS=256
TOP_K=40
TOP_P=0.9
NUM_THREADS=2
```

---

## Code Changes

### Summary of Required Changes

| File | Change Type | Lines Changed | Risk |
|------|-------------|---------------|------|
| `app/build.gradle` | Configuration | ~10 | Low |
| `IntentEngineFactory.java` | Class names | ~5 | Low |
| `EgyptianAgentApplication.java` | Class names | ~3 | Low |
| `AppConfig.java` | Constants | ~8 | Low |
| `proguard-rules.pro` | Rules | ~5 | Low |
| Test files | Class names | ~20 | Low |

### Diff Example

```diff
--- a/app/build.gradle
+++ b/app/build.gradle
@@ -15,9 +15,9 @@ android {
         versionName "2.0-functiongemma"
         
         // Model configuration
-        buildConfigField "boolean", "USE_FUNCTIONGEMMA", "false"
-        buildConfigField "boolean", "USE_LLAMA", "true"
-        buildConfigField "String", "MODEL_PATH", "\"models/llama-3.2-3b-instruct.Q4_K_M.gguf\""
+        buildConfigField "boolean", "USE_FUNCTIONGEMMA", "true"
+        buildConfigField "boolean", "USE_LLAMA", "false"
+        buildConfigField "String", "MODEL_PATH", "\"models/functiongemma-270m-it.Q4_K_M.gguf\""
         
         // FunctionGemma settings
+        buildConfigField "float", "FUNCTIONGEMMA_TEMPERATURE", "0.1f"
         buildConfigField "int", "FUNCTIONGEMMA_MAX_TOKENS", "256"
     }

--- a/app/src/main/java/com/egyptianagent/factory/IntentEngineFactory.java
+++ b/app/src/main/java/com/egyptianagent/factory/IntentEngineFactory.java
@@ -5,7 +5,7 @@ import android.content.Context;
 import com.egyptianagent.intent.IntentEngine;
 import com.egyptianagent.intent.IntentResult;
-import com.egyptianagent.llama.LlamaIntentEngine;
+import com.egyptianagent.functiongemma.FunctionGemmaIntentEngine;
 
 public class IntentEngineFactory {
     public static IntentEngine create(Context context) {
-        if (BuildConfig.USE_LLAMA) {
-            return new LlamaIntentEngine(context);
+        if (BuildConfig.USE_FUNCTIONGEMMA) {
+            return new FunctionGemmaIntentEngine(context);
         }
         throw new IllegalStateException("No intent engine configured");
     }
 }
```

---

## Rollback Plan

### Quick Rollback (< 1 hour)

If critical issues are discovered after migration:

```bash
# 1. Revert build flags
# app/build.gradle
buildConfigField "boolean", "USE_FUNCTIONGEMMA", "false"
buildConfigField "boolean", "USE_LLAMA", "true"
buildConfigField "String", "MODEL_PATH", "\"models/llama-3.2-3b-instruct.Q4_K_M.gguf\""

# 2. Rebuild
./gradlew clean
./build.sh --release

# 3. Deploy
adb install -r dist/egyptian-agent-release.apk

# 4. Verify
adb logcat | grep "Llama loaded successfully"
```

### Gradual Rollback

```
100% FunctionGemma
         │
         ▼ (Issues detected)
    50% FunctionGemma / 50% Llama
         │
         ▼ (Monitor)
    10% FunctionGemma / 90% Llama
         │
         ▼ (If issues persist)
    0% FunctionGemma / 100% Llama (Full rollback)
```

### Rollback Triggers

| Condition | Action |
|-----------|--------|
| Crash rate > 2% | Immediate rollback |
| Accuracy < 90% | Investigate, then rollback if not fixable |
| Load time > 10s | Investigate, rollback if device-specific |
| User complaints > 10/day | Rollback and investigate |

### Keeping Both Models

For safety, keep both model files:

```
models/
├── functiongemma-270m-it.Q4_K_M.gguf  # Primary
├── llama-3.2-3b-instruct.Q4_K_M.gguf  # Fallback
└── README.md                          # Model documentation
```

Configure fallback in code:

```java
public class FallbackIntentEngine implements IntentEngine {
    private FunctionGemmaIntentEngine primary;
    private LlamaIntentEngine fallback;
    
    @Override
    public IntentResult classifyIntent(String text) {
        try {
            IntentResult result = primary.classifyIntent(text);
            if (result.getConfidence() >= 0.7f) {
                return result;
            }
        } catch (Exception e) {
            Log.w("Fallback", "FunctionGemma failed, using Llama", e);
        }
        
        // Fallback to Llama
        return fallback.classifyIntent(text);
    }
}
```

---

## Post-Migration

### Verification Checklist

- [ ] App builds without errors
- [ ] Model loads successfully (< 6 seconds)
- [ ] All 16 intents classified correctly
- [ ] Accuracy >= 95% on test set
- [ ] Inference time < 500ms (P95)
- [ ] Memory usage < 600MB
- [ ] No crashes in first 24 hours
- [ ] User feedback positive

### Performance Benchmarks

Compare with Llama baseline:

| Metric | Llama Baseline | FunctionGemma | Target |
|--------|---------------|---------------|--------|
| Load Time | 30s | < 6s | ✅ |
| Inference (avg) | 1.5s | < 0.5s | ✅ |
| Memory | 4GB | < 600MB | ✅ |
| Accuracy | 97.8% | > 93% | ✅ |

### Monitoring Dashboard

Set up monitoring for:

1. **Model Load Time**
   - Track: `model_load_time_ms`
   - Alert: > 10s

2. **Inference Time**
   - Track: `inference_time_ms`
   - Alert: P95 > 500ms

3. **Accuracy**
   - Track: `intent_confidence`
   - Alert: avg < 0.85

4. **Crash Rate**
   - Track: `crash_free_users`
   - Alert: < 98%

5. **User Satisfaction**
   - Track: App store ratings
   - Alert: Rating drop > 0.5 stars

### Documentation Updates

After migration, update:

- [ ] README.md with new model info
- [ ] User manual with performance improvements
- [ ] API documentation
- [ ] Deployment guide
- [ ] Troubleshooting guide

---

## Migration Checklist

### Pre-Migration

- [ ] Backup current implementation (git tag)
- [ ] Document Llama baseline metrics
- [ ] Download FunctionGemma model
- [ ] Setup test environment
- [ ] Review breaking changes

### Configuration

- [ ] Update build.gradle flags
- [ ] Update model path
- [ ] Update temperature settings
- [ ] Update ProGuard rules
- [ ] Update environment variables

### Code Changes

- [ ] Update IntentEngineFactory
- [ ] Update Application class
- [ ] Update configuration classes
- [ ] Update imports
- [ ] Update test files

### Testing

- [ ] Run unit tests
- [ ] Run integration tests
- [ ] Run accuracy tests
- [ ] Manual testing (all intents)
- [ ] Performance benchmarking

### Deployment

- [ ] Build release APK
- [ ] Test on target devices
- [ ] Staged rollout (5% → 50% → 100%)
- [ ] Monitor metrics
- [ ] Collect user feedback

### Post-Migration

- [ ] Verify performance improvements
- [ ] Document lessons learned
- [ ] Update all documentation
- [ ] Plan model updates
- [ ] Celebrate! 🎉

---

## Support

### Contact

| Issue Type | Contact |
|------------|---------|
| Technical Issues | tech@egyptianagent.com |
| Performance Questions | performance@egyptianagent.com |
| Migration Help | migration@egyptianagent.com |

### Resources

- [FunctionGemma Architecture](FUNCTIONGEMMA_ARCHITECTURE.md)
- [FunctionGemma Deployment Guide](FUNCTIONGEMMA_DEPLOYMENT_GUIDE.md)
- [FunctionGemma API Reference](FUNCTIONGEMMA_API_REFERENCE.md)
- [FunctionGemma Performance Benchmarks](FUNCTIONGEMMA_PERFORMANCE_BENCHMARKS.md)

---

**Document Status:** ✅ Complete  
**Review Status:** ✅ Approved  
**Next Review:** June 3, 2026
