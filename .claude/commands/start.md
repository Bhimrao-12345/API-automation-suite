Run the LBVOICESER QA sync in the main conversation (no subagents):

1. Call the Atlassian MCP tool `searchJiraIssuesUsingJql` with cloudId `laerdal.atlassian.net` and this exact JQL:
   ```
   project = LBVOICESER AND status = "Ready for testing" AND issuetype in (Story, Task)
   AND sprint in openSprints()
   ORDER BY updated DESC
   ```

2. If no tickets found: reply "No new 'Ready for testing' tickets in LBVOICESER." and stop.

3. For each ticket found, call `getJiraIssue` to fetch full details, then evaluate it against the context-check rules in `.claude/prompts/context-check.md`:
   - Required on ALL tickets: summary, description, at least 1 testable AC, HTTP method, endpoint path
   - GET endpoints also need: sample response body
   - POST/PUT/PATCH endpoints also need: request body example OR API docs link

4. For SUFFICIENT tickets:
   - Read `.claude/prompts/test-generation.md` for generation rules
   - Present a test plan table: test method | @DisplayName | what it covers
   - Ask: "Type `approve plan` to generate scripts or `reject plan` to skip."

5. For INSUFFICIENT tickets:
   - Read `.claude/prompts/story-update.md` for comment rules
   - Post a structured Jira comment via `addCommentToJiraIssue` listing exactly what's missing with examples
   - After comment posted: add label `qa-context-requested` via `editJiraIssue`
   - Confirm: "Comment posted on [KEY] for assignee [NAME]."

Do everything in this conversation — do not delegate to a workflow or subagent.
