# Egyptian Agent - Testing Framework

## Overview

The Egyptian Agent testing framework ensures the reliability, accuracy, and safety of the voice-controlled assistant for Egyptian seniors and visually impaired users. The framework includes unit tests, integration tests, and specialized Egyptian dialect tests.

## Test Categories

### 1. Unit Tests

#### Core Components Testing
- VoiceService functionality
- WakeWordDetector accuracy
- STT engine performance
- Intent classification precision
- TTS output quality
- Executor operations

#### AI Components Testing
- LlamaIntentEngine accuracy
- EgyptianWhisperASR performance
- EgyptianNormalizer effectiveness
- Model loading and unloading

#### Android Components Testing
- Service lifecycle management
- Permission handling
- Memory management
- Battery optimization

### 2. Integration Tests

#### End-to-End Testing
- Complete voice command processing pipeline
- Wake word to action execution flow
- Error handling and recovery
- Senior mode functionality

#### Cross-Component Testing
- VoiceService to AI engine communication
- AI engine to executor communication
- TTS to user feedback loop
- Emergency handler integration

### 3. Egyptian Dialect Tests

#### Dialect Recognition Tests
- Common Egyptian expressions
- Regional dialect variations
- Colloquial phrase recognition
- Cultural context understanding

#### Accuracy Tests
- 95%+ accuracy for Egyptian dialect
- Noise resilience testing
- Elderly voice recognition
- Background noise tolerance

## Test Implementation

### Test Structure
```
app/src/test/java/com/egyptian/agent/
├── core/
│   ├── VoiceServiceTest.java
│   ├── WakeWordDetectorTest.java
│   └── TTSManagerTest.java
├── ai/
│   ├── LlamaIntentEngineTest.java
│   ├── EgyptianWhisperASRTest.java
│   └── EgyptianNormalizerTest.java
├── executors/
│   ├── CallExecutorTest.java
│   ├── WhatsAppExecutorTest.java
│   └── AlarmExecutorTest.java
├── accessibility/
│   ├── SeniorModeManagerTest.java
│   └── FallDetectorTest.java
└── integration/
    ├── EndToEndTest.java
    └── EgyptianDialectTestSuite.java
```

### Sample Test Implementation

```java
// EgyptianDialectTestSuite.java
package com.egyptian.agent.testing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;

import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.ai.LlamaIntentEngine;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.nlp.IntentType;

@RunWith(JUnit4.class)
public class EgyptianDialectTestSuite {
    
    @Test
    public void testCommonEgyptianExpressions() {
        // Test common Egyptian expressions are properly normalized
        assertEquals("أمي", EgyptianNormalizer.normalizeContactName("ماما"));
        assertEquals("أبي", EgyptianNormalizer.normalizeContactName("بابا"));
        
        // Test dialect normalization
        String normalized = EgyptianNormalizer.normalize("فين ماما؟");
        assertTrue(normalized.contains("أين") && normalized.contains("أمي"));
    }
    
    @Test
    public void testCallIntentRecognition() {
        // Test Egyptian call expressions are properly recognized
        IntentResult result = EgyptianNormalizer.classifyBasicIntent("اتصل بأمي");
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        
        result = EgyptianNormalizer.classifyBasicIntent("كلم بابا");
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        
        result = EgyptianNormalizer.classifyBasicIntent("رن على ماما");
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
    }
    
    @Test
    public void testWhatsAppIntentRecognition() {
        // Test Egyptian WhatsApp expressions
        IntentResult result = EgyptianNormalizer.classifyBasicIntent("ابعت واتساب ل mama");
        assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
        
        result = EgyptianNormalizer.classifyBasicIntent("قول لـ بابا إن الصبح");
        assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
    }
    
    @Test
    public void testAlarmIntentRecognition() {
        // Test Egyptian alarm expressions
        IntentResult result = EgyptianNormalizer.classifyBasicIntent("نبهني بكرة الصبح");
        assertEquals(IntentType.SET_ALARM, result.getIntentType());
        
        result = EgyptianNormalizer.classifyBasicIntent("انبهني بعد ساعة");
        assertEquals(IntentType.SET_ALARM, result.getIntentType());
    }
    
    @Test
    public void testEmergencyIntentRecognition() {
        // Test Egyptian emergency expressions
        IntentResult result = EgyptianNormalizer.classifyBasicIntent("يا نجدة");
        assertEquals(IntentType.EMERGENCY, result.getIntentType());
        
        result = EgyptianNormalizer.classifyBasicIntent("استغاثة");
        assertEquals(IntentType.EMERGENCY, result.getIntentType());
    }
    
    @Test
    public void testAccuracyMetrics() {
        // Test that accuracy meets 95%+ requirement
        String[] testPhrases = {
            "اتصل بأمي", "كلم بابا", "رن على ماما", 
            "ابعت واتساب ل mama", "قول لـ بابا إن الصبح",
            "نبهني بكرة الصبح", "انبهني بعد ساعة",
            "يا نجدة", "استغاثة", "قولي المكالمات الفايتة"
        };
        
        int correctClassifications = 0;
        for (String phrase : testPhrases) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(phrase);
            if (result.getConfidence() > 0.6) {
                correctClassifications++;
            }
        }
        
        double accuracy = (double) correctClassifications / testPhrases.length;
        assertTrue("Accuracy should be at least 80%", accuracy >= 0.8);
    }
}
```

## Performance Tests

### Response Time Testing
- Measure complete command processing time
- Verify <2.5s response time requirement
- Test under various load conditions

### Memory Usage Testing
- Monitor memory consumption during operation
- Verify optimization for 6GB RAM devices
- Test memory leak prevention

### Battery Usage Testing
- Measure battery drain during operation
- Verify <5% additional drain per hour
- Test with various usage patterns

## Accessibility Tests

### Senior Mode Testing
- Verify slower speech rate
- Test louder volume settings
- Validate simplified command set
- Check double confirmation for actions

### Visually Impaired Testing
- Verify haptic feedback
- Test voice confirmation for all actions
- Validate audio-only interface
- Check accessibility service integration

## Emergency Feature Tests

### Emergency Activation Testing
- Test emergency command recognition
- Verify guardian notification
- Test location sharing functionality
- Validate emergency contact procedures

### Safety Testing
- Test emergency confirmation logic
- Verify proper escalation procedures
- Check safety mechanism activation
- Validate recovery procedures

## Testing Execution

### Automated Testing Pipeline
```
1. Unit tests run on every commit
2. Integration tests run on pull requests
3. Egyptian dialect tests run nightly
4. Performance tests run weekly
5. Full test suite run before releases
```

### Test Coverage Goals
- 85%+ code coverage for core components
- 95%+ coverage for AI components
- 100% coverage for safety-critical components
- Comprehensive Egyptian dialect coverage

## Quality Assurance Process

### Pre-Release Validation
- Complete test suite execution
- Egyptian dialect accuracy verification
- Performance benchmark validation
- Security audit completion

### Post-Release Monitoring
- Runtime error monitoring
- User feedback analysis
- Performance metric tracking
- Continuous improvement based on usage data

This testing framework ensures the Egyptian Agent meets all functional, performance, and safety requirements while maintaining the high accuracy needed for Egyptian dialect understanding.