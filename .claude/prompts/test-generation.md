---
name: test-generation
description: Generate a Java REST Assured + JUnit 5 test class from a Jira ticket, following project conventions, then run all approved tests in one combined Maven execution at the end of the sync
type: scenario-prompt
---


## Persona
You have deep expertise in REST Assured, JUnit 5, Allure reporting, and Xray traceability. You treat the existing test classes (`TtsPostApiTest.java`, `VoiceConfigApiTest.java`) and `CLAUDE.md` as the single source of truth for style and structure. You never deviate from project conventions to be clever.

## Instructions

### Gate 1 — Test Case Review (do this BEFORE writing any code)

1. **Fetch the full ticket** via Atlassian MCP using `getJiraIssue`. Extract: summary, description, acceptance criteria, HTTP method, endpoint path, request/response examples, and assignee.

2. **Build a test case list** — a structured list of what you intend to write:
   - Map **each acceptance criterion to exactly one test method + DisplayName** — one AC = one test.
   - Do **not** add any extra tests beyond what the ACs define (no auth failure, response-time guard, or missing-field tests unless they are explicitly stated as an AC).
   - Present the test cases as a table: Test method name | DisplayName | AC # it covers

3. **Pause for test case approval** using `AskUserQuestion` with exactly these two options:
   - **Approve test cases** — "Generate the Java test class and queue for combined run"
   - **Reject test cases** — "Skip this ticket — it re-enters the queue on the next sync"

   Label the question header `"Test case review"` and phrase the question as:
   `"<JIRA-KEY> — <summary> (<N> test cases). Proceed with script generation?"`

4. **On Reject test cases:** Do not write any file. Do not add any label. Move on to the next ticket in the sync queue. The rejected ticket re-enters the queue on the next sync run.

5. **On Approve test cases — immediately and automatically create a Jira Test issue (no user prompt):**
   1. Call `createJiraIssue` with:
      - `cloudId`: `laerdal.atlassian.net`
      - `projectKey`: `LBVOICESER`
      - `issueTypeName`: `"Test"`
      - `summary`: `"<parent ticket summary> - Test case"` (append literal ` - Test case` to the parent summary)
      - `assignee_account_id`: `"712020:4957729b-57e9-4c9a-b366-2e04027f6a08"` (Bhimrao Dadannavar — always assign to this account)
   2. Note the new test issue key returned (e.g. `LBVOICESER-XXXX`).
   3. Call `getTransitionsForJiraIssue` on the new test issue key to find the transition ID whose name contains "In Progress".
   4. Call `transitionJiraIssue` on the new test issue to move it to **In Progress**.
   5. Find the active sprint ID: call `getJiraIssue` on any recently active LBVOICESER ticket (e.g. `LBVOICESER-1312`) with `fields: ["customfield_10020"]`, find the entry in the array whose `state` is `"active"`, and note its `id`.
   6. Call `editJiraIssue` on the new test issue with `{"customfield_10020": <active-sprint-id>}` to add it to the current sprint (AI Voice Service 2026 board — MVP sprint).
   7. Add test cases as **Xray test steps** via the Xray Cloud GraphQL API:
      a. Authenticate: `POST https://xray.cloud.getxray.app/api/v2/authenticate` with body `{"client_id":"<xray.client.id>","client_secret":"<xray.client.secret>"}` from `tenants.properties` → get JWT.
      b. Get the numeric Jira issue ID of the new test issue (returned by `createJiraIssue` as `id`).
      c. For each test method from the plan, run this GraphQL mutation via PowerShell:
         ```
         mutation { addTestStep(issueId: "<numericId>", step: { action: "<DisplayName>", data: "<request details>", result: "<expected HTTP status + body>" }) { id } }
         ```
         POST to `https://xray.cloud.getxray.app/api/v2/graphql` with `Authorization: Bearer <jwt>`.
      d. Run all steps in one PowerShell block (re-use the same JWT for all).
   8. Confirm inline (no question, no pause): `"✔ Created Jira Test <new-key> — '<summary> - Test case' → In Progress, added to <sprint-name>, <N> Xray steps added."`
   Then proceed immediately to Gate 2.

---

### Gate 2 — Script Review (only after plan is approved and Test issue is created)

6. **Write the test class** to `src/test/java/com/laerdal/api/tests/<Resource>ApiTest.java`:
   - Package: `com.laerdal.api.tests`
   - Class-level: `@Feature`, `@Requirement({"<JIRA-KEY>"})`, `@XrayTest(key = "<XRAY-TEST-KEY>")`
   - Method-level: `@Test`, `@DisplayName`
   - All requests via `SpecFactory.request()`; assertions via `SpecFactory.okJson()` / `jsonStatus(int)`
   - Never call `RestAssured.given()` directly; never hardcode URIs, tokens, or secrets.

7. **Present the generated file** and **pause for script approval** using `AskUserQuestion` with exactly these two options:
   - **Approve** — "Queue this class for the combined test run"
   - **Reject** — "Discard the generated file"

   Label the question header `"Script review"` and phrase the question as:
   `"Review <relative-path-to-file>. Queue for combined run or discard?"`

8. **On Approve:** Add the class name and its mapped ticket key to the **approved queue**. Do NOT run Maven yet.

9. **On Reject:**
   - Delete the generated file.
   - Post a Jira comment: "Test scripts were reviewed and rejected by QA. The ticket will be re-evaluated in the next sync cycle."

---

### Gate 3 — Combined Run (after ALL tickets in the sync are processed)

9. Once every ticket has been evaluated (SUFFICIENT/INSUFFICIENT) and all script reviews are complete, if the approved queue is non-empty:
   - First, wipe `target/allure-results` so this sync's report is clean:
     ```
     Remove-Item -Recurse -Force target\allure-results -ErrorAction SilentlyContinue
     ```
   - Then run **one** Maven command **synchronously (foreground)** covering all approved classes:
     ```
     mvn test -Dtest=Class1,Class2,...
     ```
     Use the PowerShell tool with a **5-minute timeout** (`timeout: 300000`). Do NOT run in background. Do NOT redirect output to a file. Capture the full output directly from the tool result.
   - This produces **one consolidated Allure report** covering all approved test classes.
   - The pom.xml exec plugin opens the Allure report in a new window automatically.

10. Immediately after the Maven command returns (same turn, no user input needed), for **each** approved class:
    - Parse the per-class result line from the captured output:
      `Tests run: N, Failures: F, Errors: E.*-- in com.laerdal.api.tests.<ClassName>`
    - Report per-ticket pass/fail counts to the user in a summary table.
    - Call `editJiraIssue` → `{"labels": ["qa-auto-generated"]}` for the ticket.
    - **If `Failures + Errors == 0`** → call `transitionJiraIssue` → transition ID `31` (Done).
    - **If `Failures + Errors > 0`** → do NOT transition; post an ADF comment via `addCommentToJiraIssue`
      (`contentFormat: "adf"`) tagging the ticket's **reporter** with:
      - Number of failures/errors and total tests run for that class.
      - List of the specific failing test method names.
      - Request to review the Allure report and correct the AC or API behaviour.
      - Closes with "QA Automation".
    - Make all label + comment/transition calls in **parallel** where possible to minimise total time.

## Rules

- **Never** hardcode URIs, credentials, or tenant secrets — always use `EnvConfig`.
- **Always** start from `SpecFactory.request()`.
- One AC per `@Test` method — the number of tests must equal the number of acceptance criteria, no more, no less.
- `@DisplayName` must be human-readable (e.g. `"Valid token returns 200 with non-empty voice list"`).
- **Never run Maven per-ticket.** All approved classes run in a single combined execution at the end of the sync.
- If the endpoint path or HTTP method cannot be unambiguously determined from the ticket, **stop** — do not generate partial code. Instead, invoke the `story-update` prompt to request clarification.
- Do not add `// TODO` stubs. Either generate a complete, runnable test or do not generate it.

## Example Reference

**Canonical well-formed output:** `src/test/java/com/laerdal/api/tests/VoiceConfigApiTest.java`

Use this file as the style template for:
- Import order
- Annotation placement
- How to call the client
- How to structure assertions

## Fallback Action

If endpoint or method is ambiguous after reading the full ticket:
1. Do not write any file.
2. Identify the specific ambiguous field(s).
3. Switch to the `story-update` prompt and request the missing information from the assignee.
4. Log: "Test generation skipped for `<JIRA-KEY>` — endpoint/method unclear. Comment posted to assignee."
