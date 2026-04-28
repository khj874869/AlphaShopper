$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$frontendDir = Join-Path $repoRoot "frontend"

. (Join-Path $scriptDir "Import-DotEnv.ps1")
. (Join-Path $scriptDir "Frontend-Dev.ps1")

$envFile = Resolve-PreferredEnvFile `
    -PreferredPath (Join-Path $frontendDir ".env.toss.local") `
    -FallbackPath (Join-Path $frontendDir ".env.toss.local.example")

$targetEnvFile = Join-Path $frontendDir ".env.local"
$nodeModulesDir = Join-Path $frontendDir "node_modules"

Initialize-FrontendEnvFile -SourcePath $envFile -TargetPath $targetEnvFile
Assert-FrontendNodeModules -NodeModulesPath $nodeModulesDir

Write-Host "Starting Next.js dev server with Toss local settings"

Set-Location $frontendDir
& npm.cmd run dev
