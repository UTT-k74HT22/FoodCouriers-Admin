---
name: create-plans
description: Create hierarchical project plans for solo agentic development. Outputs to .planning/ folder. Use for project briefs, roadmaps, and executable phase plans.
---

# Create Plans Skill

Create hierarchical project plans optimized for solo developer + AI agent.

## Output Structure

All planning documents go in `.planning/` folder:

```
.planning/
├── BRIEF.md              # Project vision and goals
├── ROADMAP.md            # Phase structure + tracking
├── progress/             # Progress tracking per feature
│   └── [feature]/
│       └── all-phases-progress.md
└── phases/               # Phase-specific plans
    └── 01-[phase-name]/
        ├── PLAN.md       # Executable prompt
        ├── SUMMARY.md   # Outcome (exists = done)
        └── RESEARCH.md  # Research if needed
```

## For Android Java/XML + Supabase Projects

Structure adapted for food ordering app:

```
.planning/
├── BRIEF.md              # MVP scope for Client + Admin
├── ROADMAP.md            # 14-week plan breakdown
├── auth/
│   ├── PLAN.md           # Auth implementation
│   └── SUMMARY.md
├── catalog/
│   ├── PLAN.md
│   └── SUMMARY.md
├── orders/
│   ├── PLAN.md
│   └── SUMMARY.md
└── reports/
    ├── PLAN.md
    └── SUMMARY.md
```

## Workflows

### Starting New Project
1. Answer vision/goals questions
2. Create BRIEF.md
3. Create ROADMAP.md with phases
4. Plan first phase

### Planning a Phase
1. Read BRIEF + ROADMAP
2. Check existing docs in `docs/`
3. Create PLAN.md with executable tasks
4. Include verification criteria

### Executing a Phase
1. Read PLAN.md
2. Execute each task with verification
3. Create SUMMARY.md when complete
4. Update ROADMAP.md progress

## Key Principles

- **Plans Are Prompts**: PLAN.md IS the execution prompt
- **Ship Fast, Iterate Fast**: Short sprints
- **Context Awareness**: Monitor token usage
- **User Gates**: Pause at critical decisions
- **Output to .planning/**: Always write to .planning/ folder

## Checkpoint Types

### checkpoint:human-verify
```xml
<task type="checkpoint:human-verify" gate="blocking">
  <what-built>Deployed app at URL</what-built>
  <how-to-verify>Visit URL, confirm homepage loads</how-to-verify>
  <resume-signal>Type "approved"</resume-signal>
</task>
```

### checkpoint:decision
```xml
<task type="checkpoint:decision" gate="blocking">
  <decision>Select auth provider</decision>
  <options>
    <option id="supabase">Supabase Auth - built-in, free tier</option>
    <option id="clerk">Clerk - best DX, paid after 10k</option>
  </options>
  <resume-signal>Select: supabase or clerk</resume-signal>
</task>
```

## Task Format

```xml
<task type="auto">
  <name>Task name</name>
  <files>files/to/modify</files>
  <action>What to do</action>
  <verify>How to verify</verify>
  <done>Completion criteria</done>
</task>
```

## Best Practices

1. Check `docs/` for existing PRDs/designs first
2. Write to `.planning/` folder
3. Use clear task descriptions
4. Include verification steps
5. Break complex tasks into subtasks

## Example

```
Skill("create-plans")
→ "Create brief for food ordering app MVP with Client + Admin apps"
→ "Create roadmap for 14-week development"
→ "Create phase plan for authentication module"
```

## Version
1.0
