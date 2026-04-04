---
name: planning
description: Generate PRDs, Implementation Plans, and Business Analysis documents. Outputs to docs/ folder as .md files. Use when creating feature specs, technical designs, or project documentation.
---

# Planning Skill

Generate PRDs, Implementation Plans, and Business Analysis documents optimized for AI agent consumption.

## Output Structure

All documents are generated in the `docs/` folder:

```
docs/
├── project_plans/
│   ├── PRDs/
│   │   └── features/
│   │       └── [feature-name]-v1.md
│   └── implementation_plans/
│       └── features/
│           └── [feature-name]-v1.md
├── business_analysis/
│   └── [domain]/
│       └── [document-name].md
├── technical_design/
│   └── [module]/
│       └── [design-name].md
└── api_contracts/
    └── [service].md
```

## Key Principles

### Token Efficiency
- Target: ~800 lines max per file
- Use progressive disclosure (summary → detail)
- Break large plans into phase files

### File Naming
- Use kebab-case: `user-authentication-v1.md`
- Version numbers: `-v1`, `-v2`
- Descriptive names

### Document Types

#### 1. PRD (Product Requirements Document)
```markdown
---
title: "[Feature Name] - PRD"
description: "Brief summary"
audience: [ai-agents, developers]
tags: [requirements, product]
created: YYYY-MM-DD
---
# Feature Name

## Overview
## User Stories
## Functional Requirements
## Non-Functional Requirements
## Acceptance Criteria
## Dependencies
```

#### 2. Implementation Plan
```markdown
---
title: "[Feature Name] - Implementation Plan"
description: "Implementation breakdown"
audience: [ai-agents, developers]
tags: [implementation, phases]
---
# Feature Name Implementation

## Phase 1: [Name]
### Tasks
- [ ] Task 1
- [ ] Task 2

## Phase 2: [Name]
### Tasks
- [ ] Task 3
```

#### 3. Technical Design
```markdown
---
title: "[Module] Technical Design"
description: "Technical architecture"
tags: [technical, architecture]
---
# Module Design

## Architecture
## Database Schema
## API Contracts
## Security Considerations
```

## Workflows

### Creating a New Feature

1. **Analyze Requirements**
   - Read existing docs in `docs/`
   - Check for related features

2. **Create PRD**
   - Location: `docs/project_plans/PRDs/features/[feature]-v1.md`
   - Include user stories, acceptance criteria

3. **Create Implementation Plan**
   - Location: `docs/project_plans/implementation_plans/features/[feature]-v1.md`
   - Break into phases/tasks
   - Assign to Client/Admin/Supabase

4. **Create Technical Design** (if needed)
   - Location: `docs/technical_design/[module]/`
   - Include schema, API, security

### Document Templates

#### PRD Template
```markdown
---
title: "{Feature Name} - PRD"
description: "{Brief description}"
audience: [ai-agents, developers]
tags: [{relevant tags}]
created: {YYYY-MM-DD}
updated: {YYYY-MM-DD}
status: draft|in-progress|published
---

# {Feature Name} - PRD

## 1. Overview
## 2. Problem Statement
## 3. User Stories
## 4. Functional Requirements
## 5. Non-Functional Requirements
## 6. Acceptance Criteria
## 7. Dependencies
## 8. Risks & Mitigations
## 9. Out of Scope
```

#### Implementation Plan Template
```markdown
---
title: "{Feature Name} - Implementation Plan"
description: "{Implementation summary}"
audience: [ai-agents, developers]
tags: [implementation, planning]
---

# {Feature Name} - Implementation Plan

## Executive Summary

## Phase 1: {Name}
### Description
### Tasks
| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 1.1 | Task name | Client/Admin/Supabase | feature/bugfix | 1d |

### Subtasks
- [ ] 1.1.1 Subtask

## Phase 2: {Name}
## Phase 3: {Name}

## Dependencies
## Risks
```

## Best Practices

1. **Always check existing docs** before creating new ones
2. **Cross-link documents** (PRD ↔ Plan ↔ Technical Design)
3. **Keep files under 800 lines** - break if needed
4. **Use consistent naming** - kebab-case with version
5. **Include status** - draft/in-progress/published
6. **Add @file references** for code locations

## Example Usage

```bash
# Create PRD for login feature
"Create a PRD for user authentication with email/password and OAuth"

# Create implementation plan
"Create implementation plan for authentication feature, split by Client/Admin/Supabase"

# Create technical design
"Create technical design for auth module including database schema and API contracts"

# Create business analysis
"Analyze the order management flow and document the business rules"
```

## Version
1.0
