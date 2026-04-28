function Initialize-FrontendEnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourcePath,

        [Parameter(Mandatory = $true)]
        [string]$TargetPath
    )

    if (-not (Test-Path -LiteralPath $TargetPath)) {
        Copy-Item -LiteralPath $SourcePath -Destination $TargetPath -Force
        Write-Host "Copied frontend env file to: $TargetPath"
    }
    else {
        Write-Host "Using existing frontend env file: $TargetPath"
    }
}

function Assert-FrontendNodeModules {
    param(
        [Parameter(Mandatory = $true)]
        [string]$NodeModulesPath
    )

    if (-not (Test-Path -LiteralPath $NodeModulesPath)) {
        throw "frontend/node_modules is missing. Run 'cd frontend; npm.cmd ci' first."
    }
}
