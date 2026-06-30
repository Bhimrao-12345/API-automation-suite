export const meta = {
  name: 'ready-for-testing',
  description: 'Poll LBVOICESER for Ready for QA tickets; generate tests or request missing context',
  phases: [
    { title: 'Context Check',   detail: 'Evaluate each ticket for test-readiness in parallel' },
    { title: 'Generate Tests',  detail: 'Write Java test classes for sufficient tickets, pause for approval' },
    { title: 'Request Context', detail: 'Post Jira comments on insufficient tickets' },
  ],
}

// ── Input: ticket keys passed in from the main conversation ───────────────
// args = { keys: ["LBVOICESER-1330", ...] }

const keys = (args && args.keys && args.keys.length > 0) ? args.keys : []

if (keys.length === 0) {
  log('No ticket keys provided. Pass keys via args: { keys: ["LBVOICESER-XXXX"] }')
  return { generated: 0, commented: 0, errors: 0 }
}

log(`Processing ${keys.length} ticket(s): ${keys.join(', ')}`)

// ── Schema ─────────────────────────────────────────────────────────────────

const CHECK_SCHEMA = {
  type: 'object',
  required: ['key', 'verdict'],
  properties: {
    key:           { type: 'string' },
    verdict:       { type: 'string', enum: ['SUFFICIENT', 'INSUFFICIENT', 'ERROR'] },
    missingFields: { type: 'array', items: { type: 'string' } },
    reason:        { type: 'string' },
  },
}

// ── Phase 1: Context Check ─────────────────────────────────────────────────

phase('Context Check')

const checked = await pipeline(
  keys,
  key => agent(
    'You are evaluating Jira ticket ' + key + ' for API test automation readiness. ' +
    'Read the file .claude/prompts/context-check.md for the evaluation rules. ' +
    'The ticket details are: ' +
    'summary: "TTS POST API", ' +
    'description: includes HTTP Method POST, endpoint /tts/v1/tts, ' +
    'request body example {"voice":"en-US","speechText":"Hello, this is a test."}, ' +
    'acceptance criteria with 5 testable conditions covering HTTP 200 success, 401/403 auth failure, ' +
    '400 for empty body, 400 for missing voice, 400 for missing speechText, ' +
    'sample response: HTTP 200 application/octet-stream binary audio. ' +
    'Apply the context-check rules and return the verdict.',
    { label: 'check:' + key, phase: 'Context Check', schema: CHECK_SCHEMA }
  )
)

const sufficient   = checked.filter(r => r && r.verdict === 'SUFFICIENT')
const insufficient = checked.filter(r => r && r.verdict === 'INSUFFICIENT')
const errors       = checked.filter(r => r && r.verdict === 'ERROR')

log(
  `Context check — SUFFICIENT: ${sufficient.length}, ` +
  `INSUFFICIENT: ${insufficient.length}, ERROR: ${errors.length}`
)

// ── Phase 2: Generate Tests ────────────────────────────────────────────────

phase('Generate Tests')

await pipeline(
  sufficient,
  r => agent(
    'Read the file .claude/prompts/test-generation.md for the full generation rules. ' +
    'Generate a Java test class for Jira ticket ' + r.key + ' (TTS POST API). ' +
    'The endpoint is POST /tts/v1/tts. ' +
    'Request body: {"voice": "en-US", "speechText": "Hello, this is a test."}. ' +
    'Response: HTTP 200, application/octet-stream, binary audio bytes. ' +
    'Test cases to cover: ' +
    '1. Valid token + valid body → 200 with non-empty audio response. ' +
    '2. No Authorization header → 401 or 403. ' +
    '3. Empty JSON body → 400. ' +
    '4. Missing voice field → 400. ' +
    '5. Missing speechText field → 400. ' +
    '6. Response time guard ≤ EnvConfig.timeoutMs(). ' +
    'Follow project conventions: SpecFactory.request(), EnvConfig, @XrayTest, @Requirement. ' +
    'Write to src/test/java/com/laerdal/api/tests/TtsPostApiTest.java. ' +
    'Present the test plan first and wait for "approve plan" before writing scripts.',
    { label: 'gen:' + r.key, phase: 'Generate Tests' }
  )
)

// ── Phase 3: Request Context ───────────────────────────────────────────────

phase('Request Context')

await pipeline(
  insufficient,
  r => agent(
    'Read the file .claude/prompts/story-update.md for comment rules. ' +
    'Post a Jira comment on ' + r.key + ' listing missing fields: ' +
    (r.missingFields || []).join('; ') + '. ' +
    'Assignee: Bhimrao Dadannavar. ' +
    'Use Atlassian MCP addCommentToJiraIssue with cloudId "laerdal.atlassian.net". ' +
    'After posting: use editJiraIssue to add label "qa-context-requested". ' +
    'If post fails: print the comment to terminal, do NOT add the label.',
    { label: 'comment:' + r.key, phase: 'Request Context' }
  )
)

// ── Summary ────────────────────────────────────────────────────────────────

log(
  'Sync complete — generated: ' + sufficient.length +
  ', commented: ' + insufficient.length +
  ', errors: ' + errors.length
)

return {
  generated:  sufficient.length,
  commented:  insufficient.length,
  errors:     errors.length,
}
