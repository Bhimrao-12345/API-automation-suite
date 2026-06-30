# Runs once per calendar day. Prints a reminder to open the Jira QA sync
# workflow in Claude Code when new "Ready for QA" tickets may be waiting.

$today = Get-Date -Format 'yyyyMMdd'
$flag  = "$env:TEMP\lbvoiceser_sync_$today.flag"

if (-not (Test-Path $flag)) {
    New-Item $flag -Force | Out-Null
    Write-Host ""
    Write-Host "------------------------------------------------------------"
    Write-Host " JIRA QA SYNC  |  LBVOICESER - Ready for QA"
    Write-Host "------------------------------------------------------------"
    Write-Host " Open Claude Code and run the ready-for-testing workflow"
    Write-Host " to process any tickets waiting for test automation."
    Write-Host "------------------------------------------------------------"
    Write-Host ""
}
