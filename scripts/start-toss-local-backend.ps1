$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

. (Join-Path $scriptDir "Import-DotEnv.ps1")

$envFile = Resolve-PreferredEnvFile `
    -PreferredPath (Join-Path $repoRoot ".env.toss.local") `
    -FallbackPath (Join-Path $repoRoot ".env.toss.local.example")

Import-DotEnvFile -Path $envFile

if (-not $env:APP_PAYMENT_TOSS_SECRET_KEY -or $env:APP_PAYMENT_TOSS_SECRET_KEY -like "test_sk_your_*") {
    throw "APP_PAYMENT_TOSS_SECRET_KEY is missing. Set a real Toss test secret key in $envFile"
}

Write-Host "Using backend env file: $envFile"
Write-Host "Starting backend with profiles: $env:SPRING_PROFILES_ACTIVE"

Set-Location $repoRoot
& .\mvnw.cmd spring-boot:run
