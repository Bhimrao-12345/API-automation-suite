<#
.SYNOPSIS
    Adds a label to a Jira issue using the REST API.
    Called directly from the Maven output watcher so the label is stamped
    immediately when the Allure report is generated — no Claude notification loop.

.REQUIRES
    $env:JIRA_EMAIL      — your Atlassian account email
    $env:JIRA_API_TOKEN  — Jira API token (Atlassian Account Settings → Security → API tokens)

.EXAMPLE
    powershell -File scripts\jira-label.ps1 -IssueKey LBVOICESER-1330 -Label qa-auto-generated
#>
param(
    [Parameter(Mandatory)][string]$IssueKey,
    [Parameter(Mandatory)][string]$Label
)

$email = $env:JIRA_EMAIL
$token = $env:JIRA_API_TOKEN

if (-not $email -or -not $token) {
    Write-Error "JIRA_EMAIL and JIRA_API_TOKEN environment variables must be set."
    exit 1
}

$bytes  = [System.Text.Encoding]::UTF8.GetBytes("${email}:${token}")
$auth   = "Basic " + [Convert]::ToBase64String($bytes)

$uri  = "https://laerdal.atlassian.net/rest/api/3/issue/$IssueKey"
$body = @{
    update = @{
        labels = @(@{ add = $Label })
    }
} | ConvertTo-Json -Depth 5

try {
    Invoke-RestMethod -Uri $uri -Method Put `
        -Headers @{ Authorization = $auth; "Content-Type" = "application/json" } `
        -Body $body | Out-Null
    Write-Host "[jira-label] Label '$Label' added to $IssueKey"
} catch {
    Write-Error "[jira-label] Failed to add label to ${IssueKey}: $_"
    exit 1
}
