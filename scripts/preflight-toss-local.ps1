$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$frontendDir = Join-Path $repoRoot "frontend"

. (Join-Path $scriptDir "Import-DotEnv.ps1")

$backendEnvFile = Resolve-PreferredEnvFile `
    -PreferredPath (Join-Path $repoRoot ".env.toss.local") `
    -FallbackPath (Join-Path $repoRoot ".env.toss.local.example")

$frontendEnvFile = Resolve-PreferredEnvFile `
    -PreferredPath (Join-Path $frontendDir ".env.toss.local") `
    -FallbackPath (Join-Path $frontendDir ".env.toss.local.example")

Import-DotEnvFile -Path $backendEnvFile
Import-DotEnvFile -Path $frontendEnvFile

$errors = @()

if ($env:APP_PAYMENT_PROVIDER -ne "toss") {
    $errors += "APP_PAYMENT_PROVIDER must be toss."
}

if (-not $env:APP_PAYMENT_TOSS_SECRET_KEY -or $env:APP_PAYMENT_TOSS_SECRET_KEY -like "test_sk_your_*") {
    $errors += "APP_PAYMENT_TOSS_SECRET_KEY must be replaced with a real Toss test secret key."
}

if ($env:SPRING_PROFILES_ACTIVE -notlike "*toss-local*") {
    $errors += "SPRING_PROFILES_ACTIVE should include toss-local."
}

if ($env:NEXT_PUBLIC_PAYMENT_PROVIDER -ne "toss") {
    $errors += "NEXT_PUBLIC_PAYMENT_PROVIDER must be toss."
}

if ($env:APP_FRONTEND_BASE_URL -ne "http://localhost:3000") {
    $errors += "APP_FRONTEND_BASE_URL should be http://localhost:3000 for local Toss tests."
}

if ($errors.Count -gt 0) {
    Write-Host "Toss local preflight failed:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "Toss local preflight passed." -ForegroundColor Green
Write-Host "Backend env file : $backendEnvFile"
Write-Host "Frontend env file: $frontendEnvFile"
Write-Host "Profiles         : $env:SPRING_PROFILES_ACTIVE"
Write-Host "API base URL     : $env:NEXT_PUBLIC_API_BASE_URL"
Write-Host "Storefront URL   : $env:APP_FRONTEND_BASE_URL"
Write-Host ""
Write-Host "Recommended next steps:"
Write-Host "  1. docker compose up -d"
Write-Host "  2. powershell -ExecutionPolicy Bypass -File .\scripts\start-toss-local-backend.ps1"
Write-Host "  3. powershell -ExecutionPolicy Bypass -File .\scripts\start-toss-local-frontend.ps1"
