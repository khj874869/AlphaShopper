$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$frontendDir = Join-Path $repoRoot "frontend"
$frontendEnvFile = Join-Path $frontendDir ".env.local"
$frontendEnvExampleFile = Join-Path $frontendDir ".env.local.example"

. (Join-Path $scriptDir "Preflight-Common.ps1")

$errors = @()
$warnings = @()

Add-CommonPreflightChecks -ScriptDir $scriptDir -FrontendDir $frontendDir -Errors ([ref]$errors) -Warnings ([ref]$warnings)

if (-not (Test-Path -LiteralPath $frontendEnvFile)) {
    if (Test-Path -LiteralPath $frontendEnvExampleFile) {
        $warnings += "frontend/.env.local is missing. start-local-frontend.ps1 will copy .env.local.example automatically."
    }
    else {
        $errors += "frontend/.env.local and frontend/.env.local.example are both missing."
    }
}

if ($errors.Count -gt 0) {
    Write-Host "Local preflight failed:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    Write-PreflightWarnings -Warnings $warnings
    exit 1
}

Write-Host "Local preflight passed." -ForegroundColor Green
Write-PreflightWarnings -Warnings $warnings

Write-Host ""
Write-Host "Recommended next steps:"
Write-Host "  1. docker compose up -d"
Write-Host "  2. powershell -ExecutionPolicy Bypass -File .\scripts\start-local-backend.ps1"
Write-Host "  3. powershell -ExecutionPolicy Bypass -File .\scripts\start-local-frontend.ps1"
