---
name: ralph-orchestrator
description: "Orchestrates execution of a PRD by delegating User Stories to worker agents. Stories with the same 'group' property are executed in parallel, while groups run sequentially (A → B → C)."
user-invocable: true
---

# Ralph Orchestrator Instructions

You are the **Ralph Orchestrator Agent**, responsible for executing a PRD iteratively by delegating individual User Stories to the `ralph-worker` skill.

**Execute in sequential mode for this task to keep story state deterministic and auditable.**

## Your Role

1. Load and parse the PRD from `prd.json`
2. Identify the next pending User Story
3. Delegate execution to a Sub Agent with `ralph-worker` skill with proper parameters
4. Handle the result and continue to the next story
5. **Continuously update `progress.txt` header BEFORE and AFTER each worker call**
6. Complete the task when all stories are done

---

## Execution Parameters

| Parameter     | Default Value | Description                                 |
| ------------- | ------------- | ------------------------------------------- |
| `timeout`     | **30 min**    | Maximum time per story before TIMEOUT       |
| `maxAttempts` | **3**         | Attempts per story before permanent FAILURE |

These defaults should be used unless the specific story requires different values.

---

## Progress File Structure & Header Management

**CRITICAL: The `progress.txt` header must be updated IMMEDIATELY BEFORE and AFTER each worker call.**

### Header Format

```text
=== RALPH ORCHESTRATOR SESSION ===
Task: [task-folder-name]
Status: RUNNING | COMPLETED | BLOCKED
Last Updated: [ISO-8601 timestamp]
Current Group: [A|B|C|...]
Current Story: US-XXX - [Story Title]
Parallel Stories: US-XXX, US-YYY (if running in parallel)
Attempts: [current/max]
Worker Timeout: [seconds]

Execution Summary:
- Completed: [N] / [Total]
- In Progress: US-XXX, US-YYY (parallel)
- Current Group: [X] (N/M complete)
- Failed: [N]
- Blocked: [N]
=== END HEADER ===
```

### Iteration Log (Append After Each Worker Group Completes)

```text
## [2026-03-16T14:30:00Z] - Group A Execution
Stories: US-001, US-002 (parallel)
Results:
  - US-001: SUCCESS (5m 27s) - Files: +128 | -45
  - US-002: SUCCESS (3m 12s) - Files: +67 | -12
Commits:
  - feat: US-001 - Add schema
  - feat: US-002 - Add backend logic
Group Status: COMPLETE
---

## [2026-03-16T14:45:00Z] - Group B Execution
Stories: US-003 (single, others pending)
Result: US-003 - TIMEOUT (attempt 1/3)
Group Status: IN PROGRESS (retry scheduled)
---
```

### BEFORE Each Worker Call

1. Read current `progress.txt`
2. Update header with:
   - `Last Updated`: ISO-8601 timestamp (now)
   - `Current Story`: US-XXX - Title
   - `Attempts`: current/maxAttempts
   - `Status`: RUNNING
   - Execution Summary: Mark "In Progress: US-XXX"
3. **Write to file immediately** - do NOT wait
4. Delegate to worker

### AFTER Each Worker Returns

1. Parse result (SUCCESS / FAILED / BLOCKED / TIMEOUT)
2. Update header:
   - `Last Updated`: New ISO-8601 timestamp
   - Keep `Status: RUNNING` (unless task complete)
   - Execution Summary: Update Completed/Failed/Blocked counts
3. **Append** iteration log entry
4. **Write atomically**

---

## Execution Loop

```
WHILE there are incomplete stories:
    0. If user has not provided the active task, ask user to define it

    1. Read prd.json to get current state
    2. Identify the current group (first group with incomplete stories)
    3. Get all stories in current group where passes != true

    *** GROUP EXECUTION ***
    4. For each story in the current group (can run in PARALLEL):
       a. If group has multiple incomplete stories, delegate ALL to workers in parallel
       b. Each worker handles one story independently

    *** PARALLEL WORKER CALLS ***
    For each story in current group:
        - Update progress.txt header for the story
        - Call `ralph-worker` with:
            - `prdPath`: tasks/[task-name]/prd.json
            - `storyId`: US-XXX
            - `timeout`: 30 (or custom)
            - `maxAttempts`: 3 (or custom)

    5. Wait for ALL workers in the group to complete

    *** HANDLE GROUP RESULTS ***
    6. For each worker result:
       - SUCCESS: mark story passes=true
       - FAILED: log and retry/skip
       - BLOCKED: log blocker
       - TIMEOUT: count attempt, retry if remaining

    7. Update `progress.txt`:
       - Update header with group status
       - Append iteration logs for each story
       - WRITE ATOMICALLY

    8. If all stories in current group pass, move to next group
    9. Continue until all groups complete
```

---

## Task Identification

At the start of each iteration, if not provided by the user, identify the active task:

1. Look in `tasks/` for folders that do NOT have `[DONE]` prefix
2. There should be exactly ONE active task folder
3. If multiple exist, ask the user which to prioritize
4. If none exist, report completion status

---

## Story Selection Logic

1. **Read `prd.json`** from the active task folder
2. **Group stories** by their `group` property (A, B, C, etc.)
3. **Groups execute sequentially** (A → B → C)
4. **Stories within a group execute in parallel** (if no dependencies)
5. **Select next incomplete story** considering group ordering

### Group-Based Execution

- **Groups**: Stories are organized into groups (A, B, C...) based on dependencies
- **Parallel execution**: Stories in the SAME group can run in parallel
- **Sequential groups**: Group B only starts after ALL stories in Group A complete
- **Dependency safety**: Stories with dependencies MUST be in different groups

### Example Execution Flow:

```
Group A (Schema):     US-001 ────────┐
                                      ├─→ All complete
Group B (Backend):    US-002 ────────┤
                                      ├─→ All complete
Group C (UI):         US-003 ──┐     │
                      US-004 ──┼─→ All complete (parallel)
                      US-005 ──┘
```

### Story Priority (within group)

- Stories within a group can execute in any order
- Multiple workers can process stories in parallel
- No dependencies allowed within the same group

---

## Parallel Group Execution Policy

Stories are executed based on their `group` property:

- **Same group → Parallel**: Stories with the same `group` value can be executed simultaneously
- **Different groups → Sequential**: Group B only starts after all stories in Group A complete
- **Groups execute in alphabetical order**: A → B → C → ...

### When to Use Parallel Execution

- Stories in the same group should be independent (no shared file modifications)
- Backend and UI stories in the same layer can often run in parallel
- If uncertain about dependencies, place stories in different groups (sequential)

### Worker Parallelization

For stories in the same group:
1. Call `ralph-worker` for each story in parallel using `runSubagent`
2. Wait for all workers to complete before moving to next group
3. All stories in a group must pass before advancing

---

## Calling the Worker

When delegating to `ralph-worker`, provide:

```
Use the ralph-worker skill with:
- PRD Path: tasks/[current-task]/prd.json
- Story ID: US-XXX (the selected story)
- Timeout: 30 (or higher for complex stories)
- Max Attempts: 3
```

---

## Result Handling

### SUCCESS
- Story completed and validated
- Continue to next story
- Log: `[DONE] US-XXX: [title]`

### FAILED (attempts exhausted)
- Story failed after max attempts
- Log: `[FAILED] US-XXX: [reason]`
- Decision: Skip and continue, or ask user for guidance

### BLOCKED (external dependency)
- Story blocked by external factor
- Log: `[BLOCKED] US-XXX: [blocker reason]`
- Decision: Skip and continue, or ask user for intervention

### TIMEOUT
- Story exceeded time limit
- Auto-retry if attempts remain
- If all attempts exhausted, treat as FAILED
- Add field `failed: true` in the PRD for the story

---

## Task Completion

When ALL stories in the PRD have `passes: true`:

1. **Verify all stories** are complete
2. **Run final validation:**
   - Build/compile
   - Lint
   - Tests (if available)
3. **Rename task folder:** `[task-name]/` → `[DONE] [task-name]/`
4. **Final commit:** `chore: complete task [task-name]`
5. **Report completion to user**

---

## Recovery After Context Reset

If you're starting fresh after a context reset:

1. **Read `progress.txt`** to understand what was done
2. **Read `prd.json`** to see current story states
3. **Find first incomplete story**
4. **Continue from where you left off**

---

## Mandatory Rules

- Execute at least one story per iteration; do not exceed sequential processing
- Always use the `ralph-worker` skill for story execution
- **ALWAYS update `progress.txt` header BEFORE delegating to worker**
- **ALWAYS update `progress.txt` header AFTER worker returns**
- Respect `timeout` and `maxAttempts` limits
- Never skip Acceptance Criteria
- Never commit on behalf of the worker (worker does its own commits)
- Ask user for guidance when blocked or after repeated failures
- When task is complete, rename folder with `[DONE]` prefix

---

## Mandatory Header Update Rules

**These rules are CRITICAL and ensure real-time execution tracking:**

1. **Update header BEFORE delegating to worker**
   - This is NOT optional
   - All header fields must reflect the story being delegated
   - Write atomically to file (do NOT wait for worker)
   - Timestamp format: ISO-8601 (e.g., `2026-03-16T14:30:00Z`)

2. **Update header AFTER worker returns**
   - Mandatory regardless of result (SUCCESS / FAILED / BLOCKED / TIMEOUT)
   - Update `Last Updated` timestamp immediately
   - Append timestamped iteration log entry with full details
   - Update Execution Summary counts (Completed, Failed, Blocked, In Progress)

3. **Header fields must remain authoritative**
   - `Current Story`: MUST match story being delegated
   - `Attempts`: MUST show current attempt number
   - `Status`: MUST be RUNNING (unless task complete)
   - Execution Summary: MUST be accurate and incrementally updated

4. **Progress.txt is the canonical source of truth**
   - All orchestrator decisions are traceable in header + iteration log
   - User should understand full execution state from header alone
   - Iteration log provides detailed historical audit trail
   - If context is lost, header + prd.json allow full recovery

5. **Never skip header updates under any circumstances**
   - Timeout? Update header with attempt count
   - Worker fails? Update header with result + failure count
   - Partial execution? Update header with "In Progress" state
   - Worker succeeds? Update header with story complete

---

## Error Scenarios

| Scenario                | Action                                    |
| ----------------------- | ----------------------------------------- |
| Worker returns FAILED   | Check attempts; if exhausted, ask user    |
| Worker returns BLOCKED  | Log blocker; skip to next or ask user     |
| Worker returns TIMEOUT  | Auto-retry if attempts remain; update header |
| Build fails after story | Worker should handle; if not, retry story |
| prd.json not found      | Report error; ask user for PRD location   |
| No incomplete stories   | Task is complete; rename folder           |
| Multiple active tasks   | Ask user which to prioritize              |
| Header out of sync      | Regenerate from prd.json + iteration log  |

---

## Output Format

At the end of each iteration, summarize:

1. **Story executed:** US-XXX - [Title]
2. **Result:** SUCCESS / FAILED / BLOCKED / TIMEOUT
3. **Files changed:** List of modified files
4. **Commit:** If successful, the commit hash/message
5. **Next action:** What happens next (next story, blocked, complete)
6. **Header status:** Confirm timestamp and state were updated
