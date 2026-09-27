# Repairs the art of the game after an editor or a tool emptied or deleted it.
#
# The project let a lot of the art pack go on purpose - the ores of no material, the glass, the wool,
# the rails, the plants but the oak and so on - and those files have to stay gone. Everything else
# under assets/ belongs to the game, so a file of it that is empty or that is missing from the work
# tree is damage: it is fetched back from the last commit here.
#
# The list of what was removed on purpose lives in removed_assets.txt next to this script, so a file
# that was emptied *and* had been removed on purpose is reported and left alone.
#
# Run it from the project root:  powershell -File tools/verify/restore_assets.ps1
# Look first, change nothing:    powershell -File tools/verify/restore_assets.ps1 -WhatIf

param([switch]$WhatIf)

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Push-Location $root
try {
    $removed = @()
    $list = Join-Path $PSScriptRoot 'removed_assets.txt'
    if (Test-Path $list) {
        $removed = Get-Content $list | Where-Object { $_.Trim() -ne '' }
    }

    # Damage of two kinds: a file that is there but holds nothing, and a tracked file that is gone.
    $damaged = @()
    $damaged += Get-ChildItem 'assets' -Recurse -File | Where-Object { $_.Length -eq 0 } |
            ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\', '/') }
    $damaged += git ls-files --deleted
    $damaged += git diff --cached --name-only --diff-filter=D

    $fix = $damaged | Sort-Object -Unique |
            Where-Object { $_ -like 'assets/*' -and $removed -notcontains $_ }

    if ($fix.Count -eq 0) {
        Write-Host 'Nothing to repair: no asset of the game is empty or missing.'
        return
    }

    Write-Host ("{0} files of the art are damaged:" -f $fix.Count)
    $fix | ForEach-Object { Write-Host ("  " + $_) }
    if ($WhatIf) {
        Write-Host 'Nothing was touched, this was a -WhatIf run.'
        return
    }

    git checkout HEAD -- $fix
    Write-Host ("Fetched back from the last commit, git said " + $LASTEXITCODE)
    Write-Host 'Run `gradlew build` afterwards: the texture audit is the check that all of it is there.'
} finally {
    Pop-Location
}
