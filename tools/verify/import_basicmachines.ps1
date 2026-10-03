# Draws the machines of the line of the power.
#
# The art of a basic machine is two pictures and not one: the casing of its tier - LV, MV or HV - and the
# overlay the pack draws the machine itself with, one file per face of the block. The game draws a machine
# from one picture per face and bakes the icon of its item out of the block, so the two are drawn together
# here: every face of every machine of a tier becomes a picture of its own below
# assets/blocks/basicmachines/<family>/, and the model and the blockstate of the machine are written next
# to them.
#
# The pack keeps the overlay of a machine in <Source>\<family>\OVERLAY_<FACE>.png, with a second file for
# every face the machine shows while it runs, and the casing of a tier in
# assets/blocks/machine_<tier>/machine_<tier>.png. A face the pack draws nothing for - the top of a furnace
# that has nothing on it - is left as the bare casing, so every face of a machine is one picture like any
# other block.
#
# Run it from the project root:   powershell -File tools/verify/import_basicmachines.ps1
# Look at what it would do first: powershell -File tools/verify/import_basicmachines.ps1 -WhatIf
#
# The list of the assets is refreshed after it ran:   .\gradlew.bat generateAssetList

param([string]$Source = 'D:\textures\blocks\basicmachines',
      [string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
      [switch]$WhatIf)

Add-Type -AssemblyName System.Drawing

# Size of a picture of the game, the size the pack draws every overlay at.
$TILE = 16

# The machines of the line: the folder of the pack, which is also the name the block is registered under.
$Families = @('electric_furnace', 'macerator', 'compressor', 'extractor', 'hammer', 'alloy_smelter')

# The tiers of the line, every one of them a casing of its own.
$Tiers = @('lv', 'mv', 'hv')

# The faces of a machine: the name of the picture in the game, the name the pack writes it under, and the
# face of the model it is drawn on. The two flanks share one picture, which is what a machine looks like.
$Faces = @(
    @{ Picture = 'front';  Pack = 'FRONT';  Face = 'north' },
    @{ Picture = 'side';   Pack = 'SIDE';   Face = 'south' },
    @{ Picture = 'top';    Pack = 'TOP';    Face = 'up' },
    @{ Picture = 'bottom'; Pack = 'BOTTOM'; Face = 'down' }
)

# Draws the overlay of a face over the casing of a tier.
function New-FacePicture([string]$casing, [string]$overlay) {
    $picture = New-Object System.Drawing.Bitmap -ArgumentList $TILE, $TILE,
            ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($picture)
    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
    try {
        $base = [System.Drawing.Image]::FromFile($casing)
        try {
            $graphics.DrawImage($base, 0, 0, $TILE, $TILE)
        } finally {
            $base.Dispose()
        }
        $top = [System.Drawing.Image]::FromFile($overlay)
        try {
            $graphics.DrawImage($top, 0, 0, $TILE, $TILE)
        } finally {
            $top.Dispose()
        }
    } finally {
        $graphics.Dispose()
    }
    return $picture
}

# Writes a picture of the game, which is always stored with an alpha channel, see BlockPictures.
function Write-Picture([System.Drawing.Bitmap]$picture, [string]$relative) {
    if ($WhatIf) {
        Write-Host ("would draw {0}" -f $relative)
        return
    }
    $target = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    $picture.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
}

# Writes a file of the project, with the line endings the repository keeps.
function Write-Text([string]$relative, [string]$text) {
    if ($WhatIf) {
        Write-Host ("would write {0}" -f $relative)
        return
    }
    $target = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    [IO.File]::WriteAllText($target, ($text -replace "`r`n", "`n"))
}

# Writes the model of a machine: one picture per face of an ordinary cube.
#
# The front of a machine is the face it looks in, which is the north of the model, the ceiling and the floor
# are the top and the bottom, and both flanks carry the picture of the side. A machine that runs is drawn
# with the pictures of the run and is otherwise the same shape.
function Write-Model([string]$family, [string]$tier, [bool]$active) {
    $name = "${family}_$tier"
    $running = ''
    if ($active) {
        $name = "${name}_active"
        $running = '_active'
    }
    $base = "basicmachines/$family/${family}_$tier"
    $model = @"
{
  "elements": [
    {
      "from": [0, 0, 0],
      "to": [16, 16, 16],
      "faces": {
        "down": { "texture": "${base}_bottom$running", "cullface": "down" },
        "up": { "texture": "${base}_top$running", "cullface": "up" },
        "north": { "texture": "${base}_front$running", "cullface": "north" },
        "south": { "texture": "${base}_side$running", "cullface": "south" },
        "west": { "texture": "${base}_side$running", "cullface": "west" },
        "east": { "texture": "${base}_side$running", "cullface": "east" }
      }
    }
  ]
}
"@
    Write-Text "assets/models/block/$name.json" $model
}

# Writes the blockstate of a machine: the four directions of a block and the lit face of a machine that runs.
function Write-BlockState([string]$family, [string]$tier) {
    $name = "${family}_$tier"
    $states = @"
{
  "properties": {
    "facing": ["north", "east", "south", "west"],
    "lit": ["false", "true"]
  },
  "variants": {
    "facing=north,lit=false": { "model": "$name", "y": 0 },
    "facing=east,lit=false": { "model": "$name", "y": 270 },
    "facing=south,lit=false": { "model": "$name", "y": 180 },
    "facing=west,lit=false": { "model": "$name", "y": 90 },
    "facing=north,lit=true": { "model": "${name}_active", "y": 0 },
    "facing=east,lit=true": { "model": "${name}_active", "y": 270 },
    "facing=south,lit=true": { "model": "${name}_active", "y": 180 },
    "facing=west,lit=true": { "model": "${name}_active", "y": 90 }
  }
}
"@
    Write-Text "assets/blockstates/$name.json" $states
}

# The casing of every tier is the floor of every picture, so a missing one is reported before anything runs.
foreach ($tier in $Tiers) {
    $casing = Join-Path $Root "assets/blocks/machine_$tier/machine_$tier.png"
    if (-not (Test-Path $casing)) {
        throw "the casing of the tier $tier is missing: $casing"
    }
}

$drawn = 0
foreach ($family in $Families) {
    foreach ($tier in $Tiers) {
        $casing = Join-Path $Root "assets/blocks/machine_$tier/machine_$tier.png"
        $picture = "basicmachines/$family/${family}_${tier}"
        foreach ($face in $Faces) {
            $overlay = Join-Path $Source "$family/OVERLAY_$($face.Pack).png"
            if (-not (Test-Path $overlay)) {
                throw "the overlay of $family is missing: $overlay"
            }

            # The machine that stands still.
            $still = New-FacePicture $casing $overlay
            Write-Picture $still "assets/blocks/${picture}_$($face.Picture).png"
            $still.Dispose()
            $drawn++

            # And the one it shows while it runs, a face of its own where the pack draws one.
            $running = Join-Path $Source "$family/OVERLAY_$($face.Pack)_ACTIVE.png"
            if (-not (Test-Path $running)) {
                $running = $overlay
            }
            $turn = New-FacePicture $casing $running
            Write-Picture $turn "assets/blocks/${picture}_$($face.Picture)_active.png"
            $turn.Dispose()
            $drawn++
        }
        Write-Model $family $tier $false
        Write-Model $family $tier $true
        Write-BlockState $family $tier
    }
}

$machines = $Families.Count * $Tiers.Count
Write-Host "$drawn pictures, $($machines * 2) models and $machines blockstates for $machines machines"
Write-Host "Refresh the list of the assets now: .\gradlew.bat generateAssetList"
