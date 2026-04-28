$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

function Fail([string]$Message) {
    throw $Message
}

function Expect-Equal {
    param(
        [string]$Label,
        [string]$Actual,
        [string]$Expected
    )

    if ($Actual -ne $Expected) {
        Fail "$Label mismatch. expected='$Expected' actual='$Actual'"
    }
}

$nodeVersion = (Get-Content (Join-Path $repoRoot ".nvmrc")).Trim()
$pomXml = Get-Content (Join-Path $repoRoot "pom.xml") -Raw
$packageJson = Get-Content (Join-Path $repoRoot "frontend\package.json") -Raw
$frontendDockerfile = Get-Content (Join-Path $repoRoot "frontend\Dockerfile")
$backendDockerfile = Get-Content (Join-Path $repoRoot "Dockerfile")
$workflowFiles = Get-ChildItem (Join-Path $repoRoot ".github\workflows") -Filter *.yml

$javaVersion = [regex]::Match($pomXml, "<java.version>([^<]+)</java.version>").Groups[1].Value

if (-not $nodeVersion) {
    Fail "Unable to read Node version from .nvmrc"
}

if (-not $javaVersion) {
    Fail "Unable to read Java version from pom.xml"
}

$nodeMajor = [int]($nodeVersion.Split(".")[0])
$expectedNodeEngine = ">=$nodeVersion <{0}" -f ($nodeMajor + 1)
$packageNodeEngine = [regex]::Match($packageJson, '"node"\s*:\s*"([^"]+)"').Groups[1].Value
Expect-Equal -Label "frontend/package.json engines.node" -Actual $packageNodeEngine -Expected $expectedNodeEngine

$frontendDockerVersions = @(
    $frontendDockerfile |
        Select-String '^FROM node:([^ ]+)' -AllMatches |
        ForEach-Object { $_.Matches } |
        ForEach-Object { $_.Groups[1].Value }
) | Sort-Object -Unique

Expect-Equal -Label "frontend/Dockerfile Node base image" -Actual ($frontendDockerVersions -join ",") -Expected "$nodeVersion-alpine"

$backendBuildJava = ([regex]::Match(($backendDockerfile -join "`n"), '^FROM maven:.*-eclipse-temurin-([0-9]+) AS build$', [System.Text.RegularExpressions.RegexOptions]::Multiline)).Groups[1].Value
$backendRuntimeJava = ([regex]::Match(($backendDockerfile -join "`n"), '^FROM eclipse-temurin:([0-9]+)-jre$', [System.Text.RegularExpressions.RegexOptions]::Multiline)).Groups[1].Value
Expect-Equal -Label "Dockerfile build Java" -Actual $backendBuildJava -Expected $javaVersion
Expect-Equal -Label "Dockerfile runtime Java" -Actual $backendRuntimeJava -Expected $javaVersion

$workflowJavaVersions = @(
    $workflowFiles |
        ForEach-Object { Get-Content $_.FullName } |
        Select-String 'java-version: "([0-9]+)"' -AllMatches |
        ForEach-Object { $_.Matches } |
        ForEach-Object { $_.Groups[1].Value }
) | Sort-Object -Unique

Expect-Equal -Label "GitHub Actions Java version" -Actual ($workflowJavaVersions -join ",") -Expected $javaVersion

$workflowNodeVersionFiles = @(
    $workflowFiles |
        ForEach-Object { Get-Content $_.FullName } |
        Select-String 'node-version-file: (.+)$' -AllMatches |
        ForEach-Object { $_.Matches } |
        ForEach-Object { $_.Groups[1].Value.Trim().Trim('"') }
) | Sort-Object -Unique

Expect-Equal -Label "GitHub Actions node-version-file" -Actual ($workflowNodeVersionFiles -join ",") -Expected ".nvmrc"

Write-Host "Toolchain versions are aligned."
