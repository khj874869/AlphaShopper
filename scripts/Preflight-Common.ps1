function Add-CommonPreflightChecks {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptDir,

        [Parameter(Mandatory = $true)]
        [string]$FrontendDir,

        [Parameter(Mandatory = $true)]
        [ref]$Errors,

        [Parameter(Mandatory = $true)]
        [ref]$Warnings
    )

    try {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $ScriptDir "verify-toolchain-versions.ps1") | Out-Null
    }
    catch {
        $Errors.Value += "Toolchain version alignment failed: $($_.Exception.Message)"
    }

    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        $Errors.Value += "docker CLI is not available."
    }
    elseif (-not (Get-Command docker-compose -ErrorAction SilentlyContinue) -and -not (docker compose version 2>$null)) {
        $Errors.Value += "Docker Compose is not available."
    }

    $frontendNodeModulesDir = Join-Path $FrontendDir "node_modules"
    if (-not (Test-Path -LiteralPath $frontendNodeModulesDir)) {
        $Warnings.Value += "frontend/node_modules is missing. Run 'cd frontend; npm.cmd ci' before starting the frontend."
    }
}

function Write-PreflightWarnings {
    param(
        [Parameter()]
        [AllowEmptyCollection()]
        [string[]]$Warnings
    )

    if ($Warnings.Count -gt 0) {
        Write-Host ""
        Write-Host "Warnings:" -ForegroundColor Yellow
        $Warnings | ForEach-Object { Write-Host " - $_" -ForegroundColor Yellow }
    }
}
