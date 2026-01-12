# Milestone 0 — Project Setup (Week 0)

**Goal:** Repo منضبط + Scope مقفول

## Issues

### Issue 1: Define MVP scope (lock features)
**Status:** Needs assessment
**Priority:** High
**Labels:** core, planning

**Description:**
Based on the current implementation status, assess what features are already implemented vs. what should be considered MVP. Document the final scope to prevent feature creep.

**Tasks:**
- [ ] Review IMPLEMENTATION_SUMMARY.md and COMPLETE_IMPLEMENTATION.md
- [ ] Compare against original requirements
- [ ] Define hard boundaries for MVP
- [ ] Document out-of-scope features
- [ ] Create scope freeze document

**Acceptance Criteria:**
- Clear list of MVP features
- Clear list of post-MVP features
- Stakeholder sign-off on scope

---

### Issue 2: Clean repo structure
**Status:** In progress
**Priority:** High
**Labels:** core, cleanup

**Description:**
Organize the repository structure to follow standard Android project conventions and improve maintainability.

**Current State Analysis:**
- Project already has good structure with app/, documentation/, scripts/, etc.
- Need to ensure consistent naming and organization

**Tasks:**
- [ ] Review current directory structure
- [ ] Ensure consistent naming conventions
- [ ] Organize documentation files properly
- [ ] Standardize asset organization
- [ ] Clean up any redundant files

**Acceptance Criteria:**
- Consistent directory structure
- Clear separation of concerns
- Easy navigation for new contributors

---

### Issue 3: Remove non-MVP code
**Status:** Needs assessment
**Priority:** Medium
**Labels:** core, cleanup

**Description:**
Identify and remove any experimental, deprecated, or non-MVP code that might complicate the initial release.

**Tasks:**
- [ ] Audit all Java/Kotlin files for non-MVP features
- [ ] Identify experimental implementations
- [ ] Document features to be removed
- [ ] Safely remove non-critical code
- [ ] Update dependencies accordingly

**Acceptance Criteria:**
- Only MVP features remain in codebase
- No dead code or unused dependencies
- All remaining code is documented

---

### Issue 4: Create `/docs` folder
**Status:** Pending
**Priority:** Low
**Labels:** documentation

**Description:**
Establish a standardized documentation structure that follows industry best practices.

**Tasks:**
- [ ] Create docs/ directory
- [ ] Migrate existing documentation
- [ ] Establish documentation standards
- [ ] Create template for new docs
- [ ] Link to main README

**Acceptance Criteria:**
- Centralized documentation location
- Consistent formatting and structure
- Easy to navigate and update

---

### Issue 5: Add temporary README (vision only)
**Status:** Pending
**Priority:** Medium
**Labels:** documentation

**Description:**
Create a simplified README that focuses on the project vision and mission rather than technical implementation details.

**Tasks:**
- [ ] Draft vision-focused README
- [ ] Include project mission and goals
- [ ] Add high-level architecture diagram
- [ ] Include installation prerequisites
- [ ] Link to detailed documentation

**Acceptance Criteria:**
- Clear project vision communicated
- Non-technical stakeholders can understand purpose
- Links to detailed technical docs

---

## Exit Criteria

> Repo نظيف + لا Features خارج النطاق

**Verification Steps:**
1. All non-MVP features are identified and removed or deferred
2. Repository structure is clean and organized
3. Scope is clearly defined and frozen
4. Documentation is centralized and accessible