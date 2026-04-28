$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

Write-Host "Starting backend with local profile"

Set-Location $repoRoot
& .\mvnw.cmd spring-boot:run
