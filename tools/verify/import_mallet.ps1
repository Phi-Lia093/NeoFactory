# Draws the mallet of the workshop: the soft hammer a player turns a transformer around with.
#
# A tool of the industry is drawn from two parts of the material it is made of - a handle and the head that
# sits on it - and the mod kept them apart: the pack holds one picture of the handle of a mallet and one of
# its head, both of them already standing where they belong on the finished tool. This script pours the two
# of them together into the one picture the item of the game is drawn with, the way the wrench of the game
# was drawn from its own two parts.
#
# The two parts come out of the mod and are not touched: what this script writes is the picture of the item
# itself, assets/items/mallet.png, and a part that was replaced in the pack shows up here the next time the
# script runs.
#
# Run it from the project root:   powershell -File tools/verify/import_mallet.ps1
# Look at what it would do first: powershell -File tools/verify/import_mallet.ps1 -WhatIf
#
# The list of the assets is refreshed after it ran:   .\gradlew.bat generateAssetList

param([string]$Source = 'D:\textures\items\materialicons\WOOD',
      [string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
      [switch]$WhatIf)

Add-Type -AssemblyName System.Drawing

# Size of a picture of the game, the size an icon of an item is drawn at.
$TILE = 16

# The two parts of a mallet of wood, the handle first: the head lies over the end of it, see the mod.
$Parts = @('handleMallet.png', 'toolHeadMallet.png')

$target = Join-Path $Root 'assets\items\mallet.png'
if ($WhatIf) {
    Write-Host ("would draw {0} from {1}" -f 'assets/items/mallet.png', ($Parts -join ' over '))
    return
}

$mallet = New-Object System.Drawing.Bitmap -ArgumentList $TILE, $TILE,
        ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$canvas = [System.Drawing.Graphics]::FromImage($mallet)
$canvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
try {
    foreach ($part in $Parts) {
        $path = Join-Path $Source $part
        if (-not (Test-Path $path)) {
            throw "The part $part is not in the pack at $Source"
        }
        $image = [System.Drawing.Image]::FromFile($path)
        try {
            $canvas.DrawImage($image, 0, 0, $TILE, $TILE)
        } finally {
            $image.Dispose()
        }
    }
} finally {
    $canvas.Dispose()
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
$mallet.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
$mallet.Dispose()
Write-Host 'assets/items/mallet.png'
