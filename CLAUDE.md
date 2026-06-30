# CLAUDE.md — conventions for this repo

API test automation: **Java 11 + REST Assured + JUnit 5 + Maven**, Jira/Xray
traceability, Allure reporting. 

## Commands
- `mvn test` — run all tests (uses `config/env.properties` defaults).
- `mvn test -Dtest=ClassName` — run one class.
- `mvn allure:serve` — open the Allure report.
- `mvn -DskipTests eclipse:eclipse` — regenerate Eclipse classpath from `pom.xml`.

## Conventions
- **Config:** never hard-code base URIs/tokens in tests. Read everything through
  `com.laerdal.api.config.EnvConfig`. Resolution order: `-Dkey` → env var
  (`UPPER_SNAKE_CASE`) → `config/env.properties` → default.
- **Request setup:** always start from `SpecFactory.request()` (sets base URI/path,
  JSON headers, auth, timeout, and the Allure request/response filter). Assert
  status via `SpecFactory.okJson()` / `jsonStatus(int)`.
- **Structure:** reusable per-resource call wrappers go in `clients/`; test classes
  in `tests/`; payload fixtures in `src/test/resources/testdata/`.
- **Secrets:** only via env vars / git-ignored `.env`. Never commit tokens; never
  put secrets in `env.properties`.

## Jira / Xray traceability
- Tag each test that maps to Jira with the `xray-junit-extensions` annotations:
  `@Requirement({"<JIRA-STORY-KEY>"})` and `@XrayTest(key = "<XRAY-TEST-KEY>")`
  (package `app.getxray.xray.junit.customjunitxml.annotations`; `@XrayTest` uses a
  `key` attribute, `@Requirement` takes a `String[]` value).
- Results import to Xray Cloud uses `scripts/xray-auth` + `scripts/xray-import`
  (authenticate with client id/secret, POST the enhanced JUnit XML).

## Test style
- One behaviour per `@Test`; use `@DisplayName` for readable report titles.
- Cover happy-path, negative/error cases, auth, and a response-time guard.
- Prefer Hamcrest matchers in `.body(...)`; validate JSON schema where useful via
  `json-schema-validator`.

## Jira-Driven QA Automation (startup workflow)

On VS Code folder open, `scripts/startup-sync-check.ps1` reminds you to run the
sync once per calendar day. Run the workflow via the registered skill:

```
/ready-for-testing
```

**What it does:**
1. Fetches all LBVOICESER stories in "Ready for QA" status via Atlassian MCP.
2. Evaluates each ticket for test-readiness using `.claude/prompts/context-check.md`.
   - **SUFFICIENT** (has ≥1 AC + endpoint + method + required examples) → generates a test class.
   - **INSUFFICIENT** → posts a Jira comment to the assignee listing exactly what's missing.
3. Generated test classes are written to `src/test/java/com/laerdal/api/tests/` and
   presented for human review. Type `approve` to run `mvn test` + Allure report,
   or `reject` to discard.

**Prompt library** (`.claude/prompts/`):

| File | Purpose |
|---|---|
| `context-check.md` | Evaluates ticket test-readiness; returns SUFFICIENT / INSUFFICIENT |
| `test-generation.md` | Generates Java test class following project conventions |
| `story-update.md` | Posts a structured Jira comment requesting missing AC / examples |

Each prompt defines: role, persona, instructions, rules, example, and fallback action.
