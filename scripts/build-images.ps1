$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
Set-Location $repoRoot

$target = if ($args.Count -gt 0) { $args[0] } else { "all" }
$backendTag = if ($env:BACKEND_IMAGE_TAG) { $env:BACKEND_IMAGE_TAG } else { "zigzag-shop-backend:local" }
$frontendTag = if ($env:FRONTEND_IMAGE_TAG) { $env:FRONTEND_IMAGE_TAG } else { "alphashopper-web:local" }

function Build-BackendImage {
    docker build -f Dockerfile -t $backendTag .
}

function Build-FrontendImage {
    docker build -f frontend/Dockerfile -t $frontendTag frontend
}

switch ($target) {
    "all" {
        Build-BackendImage
        Build-FrontendImage
    }
    "backend" {
        Build-BackendImage
    }
    "frontend" {
        Build-FrontendImage
    }
    default {
        throw "Usage: .\scripts\build-images.ps1 [all|backend|frontend]"
    }
}
