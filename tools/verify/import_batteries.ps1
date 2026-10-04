# Draws the batteries of the industry.
#
# A battery is one pack of steel with a window in it, and what tells one battery from another is poured into
# that window: the colour of its chemistry and how much of it is left. The game draws a cell from a strip of
# frames, one per amount of charge, so the window of a pack is poured five times - once per chemistry of
# Batteries - and every pour is stacked into the frames of a strip of its own here.
#
# The frames of a strip begin at a full cell and end at an empty one: the window is filled from its bottom
# up, so the first frame carries the colour over the whole of it and the last one carries none at all, and
# the game cuts the frame of a stack out of its charge, see GuiItemRenderer. The colour is multiplied with
# the brightness of the pixel it covers instead of being painted flat over it, which is the arithmetic the
# window of a cell of fluid is drawn with as well, see CellIconFactory: the shading the pack drew into the
# glass stays readable once it is filled.
#
# The pack of a cell comes from the mod and there are two of them, one for the cells of the low voltage and
# one for the two tiers above them, because the steel around a window is the same whatever is inside it. The
# window of each is a plain grey block of pixels in the middle of the picture, and the script refuses to
# draw a strip when a window no longer lies in its picture or when it covers a pixel that carries a colour
# of its own: a pack that was replaced cannot silently paint over the steel of a battery.
#
# Run it from the project root:   powershell -File tools/verify/import_batteries.ps1
# Look at what it would do first: powershell -File tools/verify/import_batteries.ps1 -WhatIf
#
# The list of the assets is refreshed after it ran:   .\gradlew.bat generateAssetList

param([string]$Source = 'D:\textures',
      [string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
      [switch]$WhatIf)

Add-Type -AssemblyName System.Drawing

# Size of a picture of the game, the size an icon of an item is drawn at.
$TILE = 16

# The five chemistries of Batteries, with the colour their window is poured in. The colours stand here a
# second time and are the ones of BatteryChemistry: a script cannot read them out of the game.
$Chemistries = @(
    @{ Name = 'acid';    Colour = 0xE08A18 },
    @{ Name = 'mercury'; Colour = 0xD8AEB4 },
    @{ Name = 'sodium';  Colour = 0x2838D8 },
    @{ Name = 'cadmium'; Colour = 0xB83A9E },
    @{ Name = 'lithium'; Colour = 0x9A8AE0 }
)

# The two packs of the mod and where the window of each lies: the left and the upper edge, how wide and how
# tall it is. A window that is six pixels tall carries seven frames, one per amount of rows that is left
# filled, so an empty cell is a frame of its own and not a strip without a picture.
$Packs = @(
    @{ Name = 'small'; Picture = 'battery.png';          X = 7; Y = 7; Width = 2; Height = 6 },
    @{ Name = 'large'; Picture = 'advanced_battery.png'; X = 5; Y = 6; Width = 6; Height = 7 }
)

# Reads a picture of the pack and checks that its window lies where this script expects it, so a pack that
# was replaced is reported instead of being poured at the wrong place.
function Read-Pack([string]$picture, [hashtable]$pack) {
    $file = Join-Path $Source $picture
    if (-not (Test-Path $file)) {
        throw "the pack of a battery is missing: $file"
    }
    $image = [System.Drawing.Bitmap]::FromFile($file)
    if ($image.Width -ne $TILE -or $image.Height -ne $TILE) {
        $size = "$($image.Width)x$($image.Height)"
        $image.Dispose()
        throw "$picture is $size, which is not one tile of the game"
    }
    $pixels = New-Object 'int[]' -ArgumentList ($TILE * $TILE)
    for ($y = 0; $y -lt $TILE; $y++) {
        for ($x = 0; $x -lt $TILE; $x++) {
            # The picture is kept as one row of pixels after the other: PowerShell cannot read a pixel out
            # of an array of two dimensions without a slice of its own, and this script is plain arithmetic.
            $pixels[$y * $TILE + $x] = $image.GetPixel($x, $y).ToArgb()
        }
    }
    $image.Dispose()

    for ($y = $pack.Y; $y -lt $pack.Y + $pack.Height; $y++) {
        for ($x = $pack.X; $x -lt $pack.X + $pack.Width; $x++) {
            $pixel = [System.Drawing.Color]::FromArgb($pixels[$y * $TILE + $x])
            if ($pixel.A -eq 0) {
                throw "the window of $picture holds a see through pixel at $x,$y"
            }
            if ($pixel.R -ne $pixel.G -or $pixel.G -ne $pixel.B) {
                throw "the window of $picture holds a coloured pixel at $x,$y, which a pour would lose"
            }
        }
    }
    return $pixels
}

# Pours the colour of a chemistry into the window of a pack and stacks the frames of a strip.
function New-Strip($pixels, [hashtable]$pack, [hashtable]$chemistry) {
    $frames = $pack.Height + 1
    $strip = New-Object System.Drawing.Bitmap -ArgumentList $TILE, ($TILE * $frames),
            ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $red = ($chemistry.Colour -shr 16) -band 0xFF
    $green = ($chemistry.Colour -shr 8) -band 0xFF
    $blue = $chemistry.Colour -band 0xFF

    for ($frame = 0; $frame -lt $frames; $frame++) {
        # How many rows of the window a frame leaves empty is what the frame counts, and the rows are
        # emptied from the top of the window down: the bottom of a cell is what a player reads first.
        $filled = $pack.Height - $frame
        for ($y = 0; $y -lt $TILE; $y++) {
            for ($x = 0; $x -lt $TILE; $x++) {
                $pixel = [System.Drawing.Color]::FromArgb($pixels[$y * $TILE + $x])
                $inside = $x -ge $pack.X -and $x -lt $pack.X + $pack.Width -and
                          $y -ge $pack.Y -and $y -lt $pack.Y + $pack.Height
                if ($inside -and ($y - $pack.Y) -ge ($pack.Height - $filled)) {
                    $brightness = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B))
                    $pixel = [System.Drawing.Color]::FromArgb($pixel.A,
                            [Math]::Round($red * $brightness / 255),
                            [Math]::Round($green * $brightness / 255),
                            [Math]::Round($blue * $brightness / 255))
                }
                $strip.SetPixel($x, $frame * $TILE + $y, $pixel)
            }
        }
    }
    return $strip
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
foreach ($pack in $Packs) {
    $pixels = Read-Pack $pack.Picture $pack
    foreach ($chemistry in $Chemistries) {
        $strip = New-Strip $pixels $pack $chemistry
        Write-Picture $strip "assets/items/battery_$($chemistry.Name)_$($pack.Name).png"
        $strip.Dispose()
        $drawn++
    }
}

Write-Host "$drawn strips for $($Chemistries.Count) chemistries and $($Packs.Count) packs"
Write-Host "Refresh the list of the assets now: .\gradlew.bat generateAssetList"
