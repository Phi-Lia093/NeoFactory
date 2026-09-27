# Copies the art of the original pack that a voxel world needs.
#
# The game looked at the world from above until now, which is why assets/blocks held the view from
# above and nothing else: the side, the bottom and the front of every block were removed while the
# flat engine was built, and TextureAuditTest failed the build when one of them came back. A world
# of cubes shows six faces, so those faces come home here.
#
# Four things are copied, and only what the project does not have yet:
#
#   blocks      every picture of the pack the game misses. That is the faces of the blocks that are
#               more than one picture - the grass with its snowy side and its biome overlay, the
#               logs with their bark, the furnace with its front - plus the animated sheets of
#               water, lava, fire and the portal together with their animation metadata
#   colormap    the two pictures the grass and the leaves are painted through, so a biome colours
#               them the way the original game does
#   environment the sun, the moon and its phases, the clouds, the rain and the snow of the sky
#   misc        the underwater filter, the vignette and the shadow an entity drops
#
# Run it from the project root:  powershell -File tools/verify/extract_3d_assets.ps1
# From another pack:             powershell -File tools/verify/extract_3d_assets.ps1 -Pack <folder>

param([string]$Pack = (Join-Path $env:USERPROFILE 'Downloads\assets\minecraft\textures'))

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$target = Join-Path $root 'assets'

if (-not (Test-Path $Pack)) {
    Write-Error "Missing art pack: $Pack"
    exit 1
}

# Copies every file of a folder that the target does not have yet.
#
# A file that is already there is left alone, so the script can be run again after the pack was
# replaced: it only ever adds.
function Copy-Missing {
    param([string]$From, [string]$To)

    New-Item -ItemType Directory -Force -Path $To | Out-Null
    $copied = 0
    foreach ($file in Get-ChildItem $From -File) {
        $destination = Join-Path $To $file.Name
        if (Test-Path $destination) {
            continue
        }
        Copy-Item $file.FullName $destination
        $copied++
    }
    return $copied
}

# Copies the named files of a folder that the target does not have yet.
function Copy-Named {
    param([string]$From, [string]$To, [string[]]$Names)

    New-Item -ItemType Directory -Force -Path $To | Out-Null
    $copied = 0
    $absent = @()
    foreach ($name in $Names) {
        $source = Join-Path $From $name
        if (-not (Test-Path $source)) {
            $absent += $name
            continue
        }
        $destination = Join-Path $To $name
        if (Test-Path $destination) {
            continue
        }
        Copy-Item $source $destination
        $copied++
    }
    if ($absent.Count -gt 0) {
        Write-Warning ("not in the pack: " + ($absent -join ', '))
    }
    return $copied
}

$total = 0

# The faces and the animated sheets. Everything is wanted here, including the metadata of an
# animation, so the whole folder is walked.
$added = Copy-Missing (Join-Path $Pack 'blocks') (Join-Path $target 'blocks')
Write-Host ("blocks      {0,3} files" -f $added)
$total += $added

$added = Copy-Missing (Join-Path $Pack 'colormap') (Join-Path $target 'colormap')
Write-Host ("colormap    {0,3} files" -f $added)
$total += $added

$environment = @('sun.png', 'moon_phases.png', 'clouds.png', 'rain.png', 'snow.png',
        'end_sky.png')
$added = Copy-Named (Join-Path $Pack 'environment') (Join-Path $target 'environment') $environment
Write-Host ("environment {0,3} files" -f $added)
$total += $added

# The glint of an enchanted item, the blur of a pumpkin on the head and the placeholder sheet of a
# broken pack are deliberately left out: nothing uses them and none of them is art of this game.
$misc = @('underwater.png', 'vignette.png', 'vignette.png.mcmeta', 'shadow.png',
        'shadow.png.mcmeta')
$added = Copy-Named (Join-Path $Pack 'misc') (Join-Path $target 'misc') $misc
Write-Host ("misc        {0,3} files" -f $added)
$total += $added

Write-Host ""
Write-Host "Copied $total files from $Pack"
Write-Host ("blocks folder now holds {0} pictures" -f (Get-ChildItem (Join-Path $target 'blocks') -File -Filter *.png).Count)
