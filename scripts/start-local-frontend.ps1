$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$frontendDir = Join-Path $repoRoot "frontend"
$exampleEnvFile = Join-Path $frontendDir ".env.local.example"
$targetEnvFile = Join-Path $frontendDir ".env.local"
$nodeModulesDir = Join-Path $frontendDir "node_modules"

. (Join-Path $scriptDir "Frontend-Dev.ps1")

Initialize-FrontendEnvFile -SourcePath $exampleEnvFile -TargetPath $targetEnvFile
Assert-FrontendNodeModules -NodeModulesPath $nodeModulesDir

Write-Host "Starting Next.js dev server with local settings"

Set-Location $frontendDir
& npm.cmd run dev
