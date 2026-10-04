# Draws the terminals of the power: the plug of a machine in the colour of the age it was built in.
#
# A machine of the line is drawn at the side a player gave a job to with the casing of its tier and the
# overlay of that job over it - the stub of a pipe for a fluid, the plug of the line of cables for the power,
# see FacePicture. The plug of the power is the one overlay that says which age a machine belongs to: the art
# of the mod draws it in grey, and the two ages above the first wear the very same plug in their own colour,
# see MachineTerminals. The picture of the low voltage is therefore the one the mod drew and is left where it
# is - assets/blocks/machine_overlay/energy_in.png and energy_out.png - while this script draws the yellow of
# the middle voltage and the orange of the high one beside them.
#
# The colour is multiplied with the brightness of the pixel it covers instead of being painted flat over it,
# which is the arithmetic the window of a cell of a battery is drawn with as well, see import_batteries.ps1:
# the shading of the plug and the way it stands out of the casing stay readable once it is coloured. A pixel
# that carries no colour at all is left alone, because the plug is drawn over the casing and not over it.
#
# Run it from the project root:   powershell -File tools/verify/import_energy_terminals.ps1
# Look at what it would do first: powershell -File tools/verify/import_energy_terminals.ps1 -WhatIf
#
# The list of the assets is refreshed after it ran:   .\gradlew.bat generateAssetList

param([string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
      [switch]$WhatIf)

Add-Type -AssemblyName System.Drawing

# Size of a picture of the game, the size an icon of an item is drawn at.
$TILE = 16

# The two plugs of the power as the mod draws them, which are the pictures of the low voltage.
$Plugs = @('energy_in', 'energy_out')

# The two later ages of the industry, with the colour a plug of each is drawn in. The colours stand here a
# second time and are the ones of MachineTerminals: a script cannot read them out of the game.
$Ages = @(
    @{ Suffix = 'mv'; Name = 'the middle voltage'; Colour = 0xFFD700 },
    @{ Suffix = 'hv'; Name = 'the high voltage'; Colour = 0xFF8C1E }
)

# Reads one plug of the mod, which is a picture of the game and lies in the game itself.
function Read-Plug([string]$plug) {
    $path = Join-Path $Root "assets/blocks/machine_overlay/$plug.png"
    if (-not (Test-Path $path)) {
        throw "The plug $plug is not in the game: the art of the mod is imported by import_gregtech_assets.ps1"
    }
    $image = [System.Drawing.Image]::FromFile($path)
    try {
        $bitmap = New-Object System.Drawing.Bitmap $image
        try {
            $pixels = New-Object 'int[]' ($TILE * $TILE)
            for ($y = 0; $y -lt $TILE; $y++) {
                for ($x = 0; $x -lt $TILE; $x++) {
                    $pixels[$y * $TILE + $x] = $bitmap.GetPixel($x, $y).ToArgb()
                }
            }
            return $pixels
        } finally {
            $bitmap.Dispose()
        }
    } finally {
        $image.Dispose()
    }
}

# Draws one plug in the colour of an age, keeping the shading and the transparency it came with.
function New-Plug($pixels, [hashtable]$age) {
    $plug = New-Object System.Drawing.Bitmap -ArgumentList $TILE, $TILE,
            ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $red = ($age.Colour -shr 16) -band 0xFF
    $green = ($age.Colour -shr 8) -band 0xFF
    $blue = $age.Colour -band 0xFF

    for ($y = 0; $y -lt $TILE; $y++) {
        for ($x = 0; $x -lt $TILE; $x++) {
            $pixel = [System.Drawing.Color]::FromArgb($pixels[$y * $TILE + $x])
            if ($pixel.A -gt 0) {
                $brightness = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B))
                $pixel = [System.Drawing.Color]::FromArgb($pixel.A,
                        [Math]::Round($red * $brightness / 255),
                        [Math]::Round($green * $brightness / 255),
                        [Math]::Round($blue * $brightness / 255))
            }
            $plug.SetPixel($x, $y, $pixel)
        }
    }
    return $plug
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
    Write-Host ("{0}" -f $relative)
}

$drawn = 0
foreach ($plug in $Plugs) {
    $pixels = Read-Plug $plug
    foreach ($age in $Ages) {
        $coloured = New-Plug $pixels $age
        Write-Picture $coloured "assets/blocks/machine_overlay/$($plug)_$($age.Suffix).png"
        $coloured.Dispose()
        $drawn++
    }
    Write-Host ("{0} is the picture of the low voltage and was left alone" -f $plug)
}
Write-Host ("{0} pictures drawn" -f $drawn)
