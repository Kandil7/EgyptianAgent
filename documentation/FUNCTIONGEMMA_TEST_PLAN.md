# FunctionGemma Test Plan

**Document Version:** 1.0  
**Created:** 2026-03-03  
**Last Updated:** 2026-03-03  
**Status:** ✅ Approved  

**Project:** EgyptianAgent - FunctionGemma-270M-IT Integration  
**Test Manager:** EgyptianAgent QA Team  
**Review Date:** 2026-03-10  

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Test Objectives](#test-objectives)
3. [Test Scope](#test-scope)
4. [Test Environment](#test-environment)
5. [Test Data Requirements](#test-data-requirements)
6. [Test Execution Schedule](#test-execution-schedule)
7. [Pass/Fail Criteria](#passfail-criteria)
8. [Risk Assessment](#risk-assessment)
9. [Defect Tracking](#defect-tracking)
10. [Test Report Template](#test-report-template)

---

## Executive Summary

This test plan outlines the comprehensive testing strategy for integrating FunctionGemma-270M-IT into the EgyptianAgent voice assistant. The testing focuses on validating accuracy, performance, and reliability for Egyptian Arabic dialect understanding.

### Key Testing Goals

- **Accuracy:** Achieve 95%+ accuracy on Egyptian dialect commands
- **Performance:** Sub-500ms inference latency on Honor X6c
- **Reliability:** 99%+ uptime in production scenarios
- **Coverage:** 90%+ code coverage for all FunctionGemma components

### Test Summary

| Test Category | Test Count | Target Pass Rate |
|---------------|------------|------------------|
| Unit Tests | 100+ | 100% |
| Integration Tests | 30+ | 95% |
| Performance Tests | 20+ | 90% |
| Accuracy Tests | 200+ | 95% |
| **Total** | **350+** | **95%+** |

---

## Test Objectives

### Primary Objectives

1. **Validate Intent Classification Accuracy**
   - Test all 16 supported function types
   - Verify Egyptian dialect understanding
   - Measure confidence score accuracy

2. **Verify Performance Benchmarks**
   - Model load time < 5 seconds
   - Inference latency < 500ms (average)
   - P95 latency < 700ms
   - Memory usage < 600MB

3. **Ensure Reliability**
   - Test fallback mechanisms
   - Validate error handling
   - Verify resource cleanup

4. **Validate Entity Extraction**
   - Contact names (Egyptian variations)
   - Time expressions (colloquial)
   - Message content
   - App names

### Secondary Objectives

1. Test streaming inference functionality
2. Validate concurrent request handling
3. Verify model loading/unloading
4. Test edge cases (empty input, long input, mixed languages)

---

## Test Scope

### In Scope ✅

| Component | Test Types | Priority |
|-----------|------------|----------|
| **FunctionGemmaEngine** | Unit, Integration, Performance | P0 |
| **FunctionGemmaIntentEngine** | Unit, Integration, Accuracy | P0 |
| **FunctionCallSchema** | Unit, Validation | P0 |
| **EgyptianWhisperASR Integration** | Integration | P0 |
| **Fallback Mechanisms** | Unit, Integration | P1 |
| **Streaming Inference** | Unit, Performance | P1 |
| **Memory Management** | Performance, Stress | P1 |
| **Error Handling** | Unit, Integration | P1 |

### Out of Scope ❌

- Whisper ASR model accuracy testing (separate test suite)
- TTSManager functionality (separate test suite)
- Android system integration (system-level tests)
- User acceptance testing (separate UAT phase)
- Load testing beyond device capabilities

---

## Test Environment

### Hardware Requirements

| Device | Quantity | Purpose |
|--------|----------|---------|
| Honor X6c (Helio G81 Ultra, 6GB RAM) | 3 | Primary test device |
| Mid-range Android (4GB RAM) | 2 | Performance testing |
| High-end Android (8GB+ RAM) | 2 | Upper bound testing |

### Software Requirements

| Software | Version | Purpose |
|----------|---------|---------|
| Android SDK | 34 | Build and test |
| Android NDK | 25.2.9519653 | Native build |
| Python | 3.8+ | Fine-tuning scripts |
| Gradle | 8.x | Build automation |
| JUnit | 4.13+ | Unit testing |
| Robolectric | 4.9+ | Android unit testing |
| Mockito | 4.x | Mocking framework |

### Test Environment Setup

```bash
# 1. Install Python dependencies
pip install -r requirements_functiongemma.txt

# 2. Build the project
./build_functiongemma.sh --clean --native --release

# 3. Deploy to device
./scripts/deploy_functiongemma.sh

# 4. Run tests
./scripts/run_functiongemma_tests.sh --all --coverage
```

---

## Test Data Requirements

### Training Data

- **Location:** `datasets/egyptian_voice_commands/`
- **Training Set:** 500+ examples
- **Validation Set:** 50 examples
- **Test Set:** 100 examples (held-out)

### Test Data Categories

| Category | Examples | Source |
|----------|----------|--------|
| CALL_CONTACT | 40 | Native speakers |
| SEND_WHATSAPP | 40 | Native speakers |
| SET_ALARM | 30 | Native speakers |
| EMERGENCY | 20 | Native speakers |
| READ_TIME | 15 | Native speakers |
| OPEN_APP | 25 | Native speakers |
| DEVICE_CONTROL | 25 | Native speakers |
| SEND_VOICE_MESSAGE | 15 | Native speakers |
| READ_MISSED_CALLS | 10 | Native speakers |
| READ_CONTACTS | 15 | Native speakers |

### Data Quality Requirements

- All examples reviewed by native Egyptian Arabic speakers
- Balanced gender representation in voice samples
- Age-appropriate commands for seniors (55+)
- Includes common colloquialisms and variations

---

## Test Execution Schedule

### Phase 1: Unit Testing (Week 1)

| Day | Tasks | Deliverables |
|-----|-------|--------------|
| Day 1 | FunctionGemmaEngine tests | Test results, coverage report |
| Day 2 | FunctionGemmaIntentEngine tests | Test results, coverage report |
| Day 3 | FunctionCallSchema tests | Test results, coverage report |
| Day 4 | Edge case testing | Edge case report |
| Day 5 | Bug fixes, re-testing | Updated test results |

### Phase 2: Integration Testing (Week 2)

| Day | Tasks | Deliverables |
|-----|-------|--------------|
| Day 1 | ASR integration tests | Integration test report |
| Day 2 | End-to-end pipeline tests | E2E test report |
| Day 3 | Performance benchmarks | Performance report |
| Day 4 | Memory/stress testing | Stress test report |
| Day 5 | Bug fixes, re-testing | Updated test results |

### Phase 3: Accuracy Validation (Week 3)

| Day | Tasks | Deliverables |
|-----|-------|--------------|
| Day 1-2 | Egyptian dialect accuracy tests | Accuracy report |
| Day 3-4 | Entity extraction validation | Entity extraction report |
| Day 5 | Final review, sign-off | Test completion report |

---

## Pass/Fail Criteria

### Unit Tests

| Criterion | Target | Status |
|-----------|--------|--------|
| Test Pass Rate | 100% | Required |
| Code Coverage | 90%+ | Required |
| Critical Bugs | 0 | Required |
| Major Bugs | < 5 | Required |

### Integration Tests

| Criterion | Target | Status |
|-----------|--------|--------|
| Test Pass Rate | 95%+ | Required |
| End-to-End Success | 95%+ | Required |
| Integration Bugs | < 10 | Required |

### Performance Tests

| Metric | Target | Measurement |
|--------|--------|-------------|
| Model Load Time | < 5s | Cold start |
| Inference Latency (Avg) | < 500ms | 100 commands |
| Inference Latency (P95) | < 700ms | 100 commands |
| Inference Latency (P99) | < 1000ms | 100 commands |
| Memory Usage | < 600MB | Peak during inference |
| CPU Usage | < 30% | During inference |
| Battery Impact | < 5%/hour | Active use |

### Accuracy Tests

| Metric | Target | Measurement |
|--------|--------|-------------|
| Overall Accuracy | 95%+ | 200 test commands |
| CALL_CONTACT Accuracy | 97%+ | 40 commands |
| SEND_WHATSAPP Accuracy | 94%+ | 40 commands |
| SET_ALARM Accuracy | 93%+ | 30 commands |
| EMERGENCY Accuracy | 98%+ | 20 commands |
| Entity Extraction F1 | 90%+ | All entities |
| Confidence Calibration | 85%+ | Brier score |

---

## Risk Assessment

### High Priority Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Model accuracy < 95% | Medium | High | Fine-tune with more data, adjust prompts |
| Inference latency > 500ms | Low | High | Optimize thread count, use quantization |
| Memory leaks | Low | High | Rigorous stress testing, profiling |
| Fallback failures | Medium | Medium | Comprehensive error handling tests |

### Medium Priority Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Egyptian dialect variations not covered | Medium | Medium | Expand test dataset, collect user feedback |
| Device compatibility issues | Low | Medium | Test on multiple devices, OS versions |
| Model loading failures | Low | Medium | Improve error handling, add retries |

### Low Priority Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Minor UI glitches | High | Low | Standard QA testing |
| Documentation gaps | Medium | Low | Regular documentation reviews |
| Test flakiness | Low | Low | Improve test stability, add retries |

---

## Defect Tracking

### Defect Severity Levels

| Level | Description | Response Time |
|-------|-------------|---------------|
| **Critical** | System crash, data loss, security vulnerability | Immediate |
| **High** | Major feature broken, no workaround | 24 hours |
| **Medium** | Feature impaired, workaround exists | 1 week |
| **Low** | Minor issue, cosmetic, documentation | Next release |

### Defect Categories

1. **Functional Defects**
   - Incorrect intent classification
   - Entity extraction errors
   - Fallback mechanism failures

2. **Performance Defects**
   - Latency exceeds targets
   - Memory leaks
   - Battery drain

3. **Usability Defects**
   - Poor error messages
   - Confusing user feedback
   - Accessibility issues

4. **Compatibility Defects**
   - Device-specific crashes
   - OS version incompatibility
   - Model loading failures

### Defect Tracking Tool

- **GitHub Issues:** All defects tracked with labels
- **Labels:** `bug`, `functiongemma`, `priority/critical`, `priority/high`, `priority/medium`, `priority/low`
- **Template:** Defect report template in `.github/ISSUE_TEMPLATE/`

---

## Test Report Template

### Test Execution Report

```markdown
## Test Execution Report

**Date:** YYYY-MM-DD  
**Version:** FunctionGemma-270M-EGY-v1.0  
**Device:** Honor X6c (Helio G81 Ultra, 6GB RAM)  
**Tester:** [Name]  

### Results Summary

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Total Tests | XXX | - | - |
| Passed | XXX | 95%+ | ✅/❌ |
| Failed | XXX | < 5% | ✅/❌ |
| Skipped | XX | - | - |

### Coverage

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Code Coverage | XX.X% | 90%+ | ✅/❌ |
| Intent Coverage | XX/16 | 16/16 | ✅/❌ |
| Dialect Coverage | XX.X% | 95%+ | ✅/❌ |

### Performance

| Metric | Measured | Target | Status |
|--------|----------|--------|--------|
| Avg Inference | XXXms | <500ms | ✅/❌ |
| P95 Latency | XXXms | <700ms | ✅/❌ |
| Memory Usage | XXXMB | <600MB | ✅/❌ |

### Accuracy by Category

| Category | Accuracy | Target | Status |
|----------|----------|--------|--------|
| CALL_CONTACT | XX.X% | 97%+ | ✅/❌ |
| SEND_WHATSAPP | XX.X% | 94%+ | ✅/❌ |
| SET_ALARM | XX.X% | 93%+ | ✅/❌ |
| EMERGENCY | XX.X% | 98%+ | ✅/❌ |
| Overall | XX.X% | 95%+ | ✅/❌ |

### Issues Found

#### Critical
1. [ID] Description - Status

#### High
1. [ID] Description - Status

#### Medium
1. [ID] Description - Status

### Recommendations

1. [Recommendation 1]
2. [Recommendation 2]

### Sign-off

- [ ] Test Lead Approval
- [ ] QA Manager Approval
- [ ] Product Owner Approval
```

---

## Appendix A: Test Case Examples

### Unit Test Example

```java
@Test
public void testCallContact_Mama() {
    IntentResult result = engine.classifyIntent("اتصل بماما");
    assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
    assertEquals("ماما", result.getEntity("contact_name"));
    assertTrue("Confidence should be >= 0.85", result.getConfidence() >= 0.85f);
}
```

### Integration Test Example

```java
@Test
public void testEndToEnd_SpeechToAction() {
    // 1. Load audio file
    String audioPath = loadTestAudio("call_mama.wav");
    
    // 2. Process speech
    IntentResult result = engine.processEgyptianSpeech(audioPath);
    
    // 3. Validate intent
    assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
    
    // 4. Validate entity
    assertEquals("ماما", result.getEntity("contact_name"));
    
    // 5. Validate performance
    assertTrue("Should complete in <1s", result.processingTimeMs < 1000);
}
```

---

## Appendix B: Performance Testing Script

```bash
#!/bin/bash
# Performance benchmark script

echo "Running FunctionGemma performance benchmarks..."

# Warm up
for i in {1..10}; do
    ./scripts/benchmark_inference.sh --input "اتصل بماما"
done

# Measure
./scripts/benchmark_inference.sh \
    --iterations 100 \
    --output results/performance_report.json \
    --metrics latency,memory,cpu

echo "Performance report: results/performance_report.json"
```

---

## Document Approval

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Test Manager | | | |
| QA Lead | | | |
| Development Lead | | | |
| Product Owner | | | |

---

**Next Review Date:** 2026-03-10  
**Distribution:** Development Team, QA Team, Product Management
