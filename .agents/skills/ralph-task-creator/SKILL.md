---
name: ralph-task-creator
description: "Creates a new task structure following the swing-gui-builder project pattern. Use when the user asks to create a new task, feature, or increment the project. Triggers: create a task, new task, criar task, nova feature, adicionar feature, incrementar projeto, new feature, add feature."
user-invocable: true
---

# Task Scaffold Generator

Creates the directory and file structure for a new task following the swing-gui-builder project pattern.

---

## The Job

1. Receive the task name/title from the user
2. Create the directory structure in the format `task-DD_MM_YY_HHMMss-[task-name]`
3. Create the required files:
   - `req-[task-name].md` - Requirements and conversation history
   - `plan-[task-name].md` - Implementation plan

**Important:** Do NOT start implementing. Just create the structure.

---

## Naming Convention

- **Task name:** in English, kebab-case (e.g., `components-preview`, `expand-swing-components`)
- **Date:** format `DD_MM_YY_HHMMSS` (e.g., `16_03_26_183407`)
- **Full example:** `task-16_03_26_183407-components-preview`

---

## Step 1: Collect Information

Ask the user:

1. Task name/title (in English, kebab-case)
2. Brief description of what needs to be implemented

---

## Step 2: Generate Current Date

Generate current timestamp in `DD_MM_YY_HHMMSS` format:

```javascript
const now = new Date();
const timestamp = `${String(now.getDate()).padStart(2, "0")}_${String(now.getMonth() + 1).padStart(2, "0")}_${String(now.getFullYear()).slice(-2)}${String(now.getHours()).padStart(2, "0")}${String(now.getMinutes()).padStart(2, "0")}${String(now.getSeconds()).padStart(2, "0")}`;
// Example: 16_03_26_183407
```

---

## Step 3: Create File Structure

Create in `tasks/task-{timestamp}-{name}/`:

### req-{name}.md

```markdown
# {Title}

## Description

[Brief description of the task]

## Decided Requirements

- [ ] Requirement 1
- [ ] Requirement 2
```

### plan-{name}.md

```markdown
# Plan: {Title}

## Steps

1. [ ] Step 1
2. [ ] Step 2
```

1. Ask the user for the task name
2. Generate the current date
3. Create the directories and files
4. Show the user the created structure with the file paths
