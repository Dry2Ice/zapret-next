param(
    [string]$ManifestPath = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..')).Path 'bin/checksums.sha256')
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $ManifestPath)) {
    Write-Error "Manifest file not found: $ManifestPath"
}

$root = Split-Path -Parent (Split-Path -Parent $ManifestPath)
$failed = New-Object System.Collections.Generic.List[string]

Get-Content -LiteralPath $ManifestPath |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and -not $_.StartsWith('#') } |
    ForEach-Object {
        if ($_ -notmatch '^([a-fA-F0-9]{64})\s+\*(.+)$') {
            $failed.Add("[manifest-format] $_")
            return
        }

        $expected = $matches[1].ToLowerInvariant()
        $relative = $matches[2]
        $targetPath = Join-Path $root $relative

        if (-not (Test-Path -LiteralPath $targetPath)) {
            $failed.Add("[missing-file] $relative")
            return
        }

        $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $targetPath).Hash.ToLowerInvariant()
        if ($actual -ne $expected) {
            $failed.Add("[hash-mismatch] $relative")
        } else {
            Write-Host "OK  $relative"
        }
    }

if ($failed.Count -gt 0) {
    Write-Host 'Integrity check failed:' -ForegroundColor Red
    $failed | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host 'Integrity check passed.' -ForegroundColor Green
exit 0
