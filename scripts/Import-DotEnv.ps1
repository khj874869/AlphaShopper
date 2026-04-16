function Import-DotEnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Env file not found: $Path"
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()

        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            return
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Resolve-PreferredEnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PreferredPath,

        [Parameter(Mandatory = $true)]
        [string]$FallbackPath
    )

    if (Test-Path -LiteralPath $PreferredPath) {
        return $PreferredPath
    }

    if (Test-Path -LiteralPath $FallbackPath) {
        return $FallbackPath
    }

    throw "Neither env file exists: $PreferredPath or $FallbackPath"
}
