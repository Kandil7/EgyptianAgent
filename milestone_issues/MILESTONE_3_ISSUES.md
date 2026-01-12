# Milestone 3 — Command Router (Week 3)

**Goal:** فصل الذكاء عن التنفيذ

## Issues

### Issue 1: Implement `CommandRouter`
**Status:** Needs verification
**Priority:** High
**Labels:** core, android

**Description:**
Based on the architecture described in the implementation documents, there should be a CommandRouter that separates the AI intelligence layer from the execution layer. The documentation mentions a "Hybrid Orchestrator" that intelligently routes between local and cloud processing.

**Tasks:**
- [ ] Locate existing CommandRouter/Hybrid Orchestrator implementation
- [ ] Review routing logic between AI and executors
- [ ] Verify separation of concerns between intelligence and execution
- [ ] Test routing functionality with various intents
- [ ] Document routing decision criteria

**Acceptance Criteria:**
- Clear separation between AI layer and execution layer
- Deterministic routing logic
- Proper error handling in routing
- Well-documented routing decisions

---

### Issue 2: Inject executors
**Status:** Needs verification
**Priority:** High
**Labels:** core, android

**Description:**
The CommandRouter should properly inject and manage executor instances based on the determined intent from the AI engine.

**Tasks:**
- [ ] Review executor injection mechanism
- [ ] Verify dependency injection pattern implementation
- [ ] Test executor lifecycle management
- [ ] Check proper cleanup of executor resources
- [ ] Validate executor instantiation patterns

**Acceptance Criteria:**
- Executors properly injected into router
- Dependency injection working correctly
- Resource management handled properly
- Executors instantiated as needed

---

### Issue 3: Fallback logic
**Status:** Needs verification
**Priority:** High
**Labels:** core, error-handling

**Description:**
The system should have fallback mechanisms when primary routing fails or when executors encounter errors.

**Tasks:**
- [ ] Review fallback routing implementation
- [ ] Test failure scenarios for executors
- [ ] Verify graceful degradation
- [ ] Check error propagation to user interface
- [ ] Validate recovery mechanisms

**Acceptance Criteria:**
- Fallback logic properly implemented
- System degrades gracefully when routing fails
- Appropriate error handling and recovery
- User feedback provided during failures

---

### Issue 4: Unit test routing logic
**Status:** Needs verification
**Priority:** Medium
**Labels:** testing, core

**Description:**
Comprehensive unit tests should validate the routing logic to ensure deterministic behavior.

**Tasks:**
- [ ] Locate existing routing unit tests
- [ ] Verify test coverage for routing logic
- [ ] Add missing test cases if needed
- [ ] Run existing routing tests
- [ ] Validate test completeness

**Acceptance Criteria:**
- Comprehensive unit test coverage for routing
- All routing scenarios tested
- Tests passing consistently
- Edge cases covered

---

### Issue 5: Remove any direct execution from AI layer
**Status:** Needs verification
**Priority:** High
**Labels:** core, architecture

**Description:**
Ensure that the AI layer only determines intent and does not directly execute commands, maintaining proper separation of concerns.

**Tasks:**
- [ ] Audit AI components for direct execution calls
- [ ] Verify AI layer only returns intents
- [ ] Confirm execution happens in dedicated executors
- [ ] Remove any violations of separation principle
- [ ] Document proper architecture pattern

**Acceptance Criteria:**
- AI layer only responsible for intent determination
- No direct execution from AI components
- Clear separation of intelligence and execution
- Architecture pattern consistently followed

---

## Exit Criteria

> Router نظيف + deterministic

**Verification Steps:**
1. Command routing logic is clean and well-structured
2. Routing decisions are deterministic and predictable
3. Proper separation between AI and execution layers
4. Fallback mechanisms working correctly
5. All routing scenarios properly tested