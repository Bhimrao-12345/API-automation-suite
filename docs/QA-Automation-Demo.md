# Jira-Driven QA Automation — Demo Presentation
**Project:** LBVOICESER API Test Suite  
**Stack:** Java 11 · REST Assured · JUnit 5 · Maven · Allure · Xray · Claude Code

---

## 1. Problem Statement

| Today (Manual) | With This Automation |
|---|---|
| QA engineer reads Jira ticket | Claude reads the Jira ticket |
| QA manually writes test class | Claude generates the test class |
| QA checks if ticket has enough info | Claude checks and asks for missing info |
| Tests run manually, report shared separately | Tests run automatically, Allure report generated |
| Same ticket can be picked up multiple times | Each ticket processed exactly once |

**The bottleneck:** After a developer moves a ticket to "Ready for QA", a QA engineer has to manually read it, decide if it's testable, write the test, and run it. This is repetitive, error-prone, and slow.

---

## 2. Objective of This Initiative

A **Jira-driven automation loop** that runs inside VS Code / Claude Code.

```
┌─────────────────────────────────────────────────────────────────┐
│                       DEVELOPER                                 │
│         Moves Jira ticket → "Ready for QA"                      │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    VS CODE STARTUP                               │
│    scripts/startup-sync-check.ps1 fires on folder open          │
│    Shows reminder once per day → "Run /ready-for-testing"        │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│               CLAUDE CODE WORKFLOW                               │
│                                                                  │
│  Phase 1 ── Fetch Tickets                                        │
│             JQL: LBVOICESER + "Ready for QA"                     │
│             Excludes already-processed tickets (label filter)    │
│                                                                  │
│  Phase 2 ── Context Check (per ticket)              │
│             Evaluates mandatory fields                           │
│             Returns: SUFFICIENT or INSUFFICIENT                  │
│                                                                  │
│  Phase 3a ─ Generate Tests  (SUFFICIENT tickets)                │
│             Step 1: Generate test case plan                      │
│                     → Human reviews plan                         │
│                     → approve / reject                           │
│             Step 2: Write automation scripts                     │
│                     → Human reviews scripts                      │
│                     → approve → mvn test → Allure report         │
│             → stamps label "qa-auto-generated" on Jira ticket    │
│                                                                  │
│  Phase 3b ─ Request Context  (INSUFFICIENT tickets)              │
│             Posts structured Jira comment to assignee            │
│             → stamps label "qa-context-requested" on Jira ticket │
│                                                                  │
│  Phase 4 ── Summary report to QA engineer                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Key Design Decisions

### 3.1 One-time processing per ticket
Every processed ticket gets a Jira label stamped on it.  
The JQL query **excludes** these labels on every subsequent run.

```
qa-auto-generated     → test was generated & approved — never touch again
qa-context-requested  → comment posted — wait until assignee updates & removes label
```

**Result:** A ticket is processed exactly once. No duplicate test files. No duplicate comments.

---

### 3.2 Human-in-the-loop review gate

Claude does NOT write scripts or run tests automatically. There are **two review checkpoints**.

```
┌─────────────────────────────────────────────────────────────┐
│  GATE 1 — Plan Review                                        │
│                                                             │
│  Claude reads the Jira ticket                               │
│          │                                                  │
│          ▼                                                  │
│  Claude presents a test case plan:                          │
│    - List of test scenarios to cover                        │
│    - Which ACs map to which test methods                    │
│    - Edge cases and negative tests identified               │
│          │                                                  │
│  QA Engineer: "approve plan" or "reject plan"               │
│          │                                                  │
│     ┌────┴────┐                                             │
│  approve    reject                                          │
│     │          │                                            │
│     ▼          ▼                                            │
│  Proceed    Stop — ticket re-enters queue next sync         │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 2 — Script Review                                      │
│                                                             │
│  Claude writes the Java test class                          │
│          │                                                  │
│          ▼                                                  │
│  Claude presents:                                           │
│    - File path (clickable in VS Code)                       │
│    - Table: test method | DisplayName | what it covers      │
│          │                                                  │
│  QA Engineer: "approve" or "reject"                         │
│          │                                                  │
│     ┌────┴────┐                                             │
│  approve    reject                                          │
│     │          │                                            │
│     ▼          ▼                                            │
│  mvn test   File deleted                                    │
│  Allure     Ticket NOT labelled                             │
│  report     (re-enters queue next sync)                     │
│     │                                                       │
│     ▼                                                       │
│  Label "qa-auto-generated" added to Jira ticket             │
└─────────────────────────────────────────────────────────────┘
```

---

### 3.3 Scenario-based Prompt Library

All AI behaviour is defined in `.claude/prompts/` — not hardcoded.  
Each prompt file follows the same 6-part schema:

| Section | Purpose |
|---|---|
| **Role** | Who Claude is acting as |
| **Persona** | Domain expertise profile |
| **Instructions** | Step-by-step what to do |
| **Rules** | Hard constraints (never break these) |
| **Example** | Input → output sample |
| **Fallback Action** | What to do when something goes wrong |

**Three prompts drive the entire workflow:**

| Prompt | Triggered when | What it does |
|---|---|---|
| `context-check.md` | Every ticket, every sync | Scores ticket for testability |
| `test-generation.md` | Ticket passes context check | Generates Java test class |
| `story-update.md` | Ticket fails context check | Posts comment to Jira assignee |

---

## 4. Mandatory Jira Fields

For a ticket to pass the context check and get a test generated automatically:

### Required on ALL tickets

| Field | Example |
|---|---|
| **Summary** | "GET voice configurations returns list for authenticated tenant" |
| **Description** | "The GET /tts/v1/voice-configurations endpoint returns all voices available to the calling tenant." |
| **Acceptance Criteria** | At least 1 testable condition (see below) |
| **HTTP Method** | `GET` / `POST` / `PUT` / `PATCH` / `DELETE` |
| **Endpoint Path** | `/tts/v1/voice-configurations` |

### Additional field — by HTTP method

| Method | Extra required field | Example |
|---|---|---|
| **GET** | Sample response body | `[{"tenantId": 201, "voice": "en-US"}]` |
| **POST / PUT / PATCH** | Request body example OR API docs link | `{"voice": "en-US", "speechText": "Hello"}` |


---

## 5. What Gets Generated

Claude follows the exact same patterns as the hand-written test classes already in the project.

**Generated test class structure:**
```java
@Feature("Voice Configuration API")
@Requirement({"LBVOICESER-1283"})
@XrayTest(key = "LBVOICESER-1283")
class VoiceConfigApiTest {

    @Test
    @DisplayName("Valid token returns 200 with non-empty voice list")
    void tc01_validTokenReturns200() { ... }

    @Test
    @DisplayName("Request without auth returns 401 or 403")
    void tc02_missingAuthReturnsError() { ... }

    @Test
    @DisplayName("Response time is within acceptable limit")
    void tc03_responseTimeGuard() { ... }
}
```

**Every generated class automatically includes:**
- Happy path from acceptance criteria
- Auth failure (no token → ≥ 400)
- Response-time guard (≤ configured timeout)
- Xray + Requirement annotations for traceability

---

## 6. Tech Stack & Integrations

```
┌──────────────┐    MCP      ┌─────────────────┐
│  Claude Code │ ──────────► │  Atlassian Jira  │
│  (VS Code)   │ ◄────────── │  LBVOICESER      │
└──────┬───────┘             └─────────────────┘
       │
       │ generates
       ▼
┌──────────────┐    runs     ┌──────────────────┐
│  Java Test   │ ──────────► │  Maven + JUnit 5  │
│  Class       │             │  REST Assured     │
└──────────────┘             └────────┬─────────┘
                                      │
                                      ▼
                             ┌──────────────────┐
                             │  Allure Report   │
                             │  Xray Results    │
                             └──────────────────┘
```

| Component | Technology |
|---|---|
| AI Agent | Claude Code (claude-sonnet-4-6) |
| Jira integration | Atlassian MCP server |
| Test framework | JUnit 5 + REST Assured 5.4 |
| Reporting | Allure 2.27 |
| Traceability | Xray JUnit Extensions |
| Build | Maven 3.9, Java 11 |

---

## 7. Demo Script

### Step 1 — Show the Jira ticket
Open a LBVOICESER ticket in "Ready for QA" status.  
Point out the mandatory fields: Summary, Description, ACs, HTTP Method, Endpoint, Sample Response.

### Step 2 — Open VS Code
Show the terminal startup reminder from `scripts/startup-sync-check.ps1`.

### Step 3 — Run the workflow
In Claude Code chat, type:
```
/ready-for-testing
```
Show the 4 phases running: Fetch → Context Check → Generate / Comment.

### Step 4a — Sufficient ticket
Show Claude presenting the generated test file path and test case table.  
Type `approve` → watch `mvn test` run → show Allure report.  
Go to Jira — show the `qa-auto-generated` label on the ticket.

### Step 4b — Insufficient ticket (if available)
Show Claude posting the structured comment on Jira.  
Show the `qa-context-requested` label stamped on the ticket.  
Explain: "Once the developer updates the ticket and removes this label, the next sync will pick it up."

### Step 5 — Run the workflow again
Type `/ready-for-testing` again.  
Show that the already-processed ticket does **not** appear — it is skipped by the JQL label filter.

---

## 8. Benefits

| Benefit | How |
|---|---|
| **Faster QA start** | Tests generated same day as "Ready for QA" — no waiting for manual scripting |
| **Consistent test coverage** | Every generated class always has auth + response-time tests, no matter who runs it |
| **Traceability built-in** | Xray + Requirement annotations added automatically |
| **No duplicate work** | Label filter ensures each ticket is processed exactly once |
| **Clear feedback loop** | Vague tickets get a structured Jira comment — developers know exactly what's missing |
| **Human control** | Approve/reject gate means no test runs without QA sign-off |
