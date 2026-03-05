param(
    [string]$RootPath = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

$listsPath = Join-Path $RootPath 'lists'
if (-not (Test-Path -LiteralPath $listsPath)) {
    Write-Error "Lists directory not found: $listsPath"
}

$domainRegex = '^(?:\*\.)?[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+\.?$'
$ipv4CidrRegex = '^(?:(?:25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\/(?:3[0-2]|[12]?\d))?$'

$domainFiles = @(
    'list-general.txt',
    'list-google.txt',
    'list-exclude.txt'
)

$ipFiles = @(
    'ipset-all.txt',
    'ipset-exclude.txt'
)

$issues = New-Object System.Collections.Generic.List[string]

function Get-CleanLines {
    param([string]$Path)

    Get-Content -LiteralPath $Path |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') }
}

foreach ($file in $domainFiles) {
    $path = Join-Path $listsPath $file
    if (-not (Test-Path -LiteralPath $path)) {
        $issues.Add("[missing] $file")
        continue
    }

    $seen = @{}
    foreach ($line in Get-CleanLines -Path $path) {
        $entry = $line.ToLowerInvariant()

        if (-not ($entry -match $domainRegex)) {
            $issues.Add("[domain-format] $file :: $line")
            continue
        }

        if ($seen.ContainsKey($entry)) {
            $issues.Add("[domain-duplicate] $file :: $line")
        } else {
            $seen[$entry] = $true
        }
    }
}

foreach ($file in $ipFiles) {
    $path = Join-Path $listsPath $file
    if (-not (Test-Path -LiteralPath $path)) {
        $issues.Add("[missing] $file")
        continue
    }

    $seen = @{}
    foreach ($line in Get-CleanLines -Path $path) {
        if (-not ($line -match $ipv4CidrRegex)) {
            $issues.Add("[ip-format] $file :: $line")
            continue
        }

        if ($seen.ContainsKey($line)) {
            $issues.Add("[ip-duplicate] $file :: $line")
        } else {
            $seen[$line] = $true
        }
    }
}

if ($issues.Count -eq 0) {
    Write-Host 'Lists validation passed.' -ForegroundColor Green
    exit 0
}

Write-Host 'Lists validation found issues:' -ForegroundColor Yellow
$issues | ForEach-Object { Write-Host " - $_" }

if ($Strict) {
    exit 1
}

exit 0
