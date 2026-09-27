# Brings the art of the machine mod into the game.
#
# The mod draws the casing of a machine, the mouth of a boiler and the overlay of a machine front, and
# those pictures are copied here one block folder at a time. The art of the game itself comes from the pack
# the world is built on, so this script is the whole list of what was taken from the mod and what it is
# called inside the game: the audit of the art reports every picture that no block draws, see
# MultiFaceTextures and TextureAuditTest.
#
# The strip of the burning mouth of a boiler is four frames of a tile stacked into one picture. The game
# draws a machine front as one tile, so the first frame is cut out here; the animation of the mouth is the
# work of the pass that adds it, see Block#animation.
#
# Run it from the project root:   powershell -File tools/verify/import_gregtech_assets.ps1
# Look at what it would do first: powershell -File tools/verify/import_gregtech_assets.ps1 -WhatIf

param([string]$Source = 'D:\textures\blocks\iconsets',
      [string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)),
      [switch]$WhatIf)

Add-Type -AssemblyName System.Drawing

function Copy-Face([string]$from, [string]$to) {
    if ($WhatIf) {
        Write-Host ("would copy {0} -> {1}" -f $from, $to)
        return
    }
    $target = Join-Path $Root $to
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Copy-Item -Path (Join-Path $Source $from) -Destination $target -Force
    Write-Host ("{0} -> {1}" -f $from, $to)
}

# Cuts the first frame out of a strip of frames stacked below each other.
function Copy-FirstFrame([string]$from, [string]$to, [int]$frames) {
    if ($WhatIf) {
        Write-Host ("would cut the first of {0} frames of {1} -> {2}" -f $frames, $from, $to)
        return
    }
    $sheet = [System.Drawing.Image]::FromFile((Join-Path $Source $from))
    try {
        if ($sheet.Height -ne $sheet.Width * $frames) {
            throw ("{0} is {1} by {2}, which is not {3} frames of one tile" -f $from,
                    $sheet.Width, $sheet.Height, $frames)
        }
        $frame = New-Object System.Drawing.Bitmap $sheet.Width, $sheet.Width
        $g = [System.Drawing.Graphics]::FromImage($frame)
        $rect = New-Object System.Drawing.Rectangle 0, 0, $sheet.Width, $sheet.Width
        $g.DrawImage($sheet, $rect, $rect, [System.Drawing.GraphicsUnit]::Pixel)
        $g.Dispose()
        $target = Join-Path $Root $to
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        $frame.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
        $frame.Dispose()
    } finally {
        $sheet.Dispose()
    }
    Write-Host ("{0} (first of {1} frames) -> {2}" -f $from, $frames, $to)
}

# The casing of every machine of the bronze age, one picture per side of a block.
Copy-Face 'MACHINE_BRONZE_TOP.png'    'assets/blocks/bronze_casing/bronze_casing_top.png'
Copy-Face 'MACHINE_BRONZE_SIDE.png'   'assets/blocks/bronze_casing/bronze_casing_side.png'
Copy-Face 'MACHINE_BRONZE_BOTTOM.png' 'assets/blocks/bronze_casing/bronze_casing_bottom.png'

# The mouth of a boiler: the door it shows while it stands still and the door it glows with while it burns.
Copy-Face 'BOILER_FRONT.png' 'assets/blocks/bronze_boiler/bronze_boiler_front.png'
Copy-FirstFrame 'BOILER_FRONT_ACTIVE.png' 'assets/blocks/bronze_boiler/bronze_boiler_front_active.png' 4

# The chest, the block a player keeps things in: the buffer of the automation age, a whole cube of one
# picture like the blocks of the landscape, see Blocks#CHEST.
Copy-Face 'AUTOMATION_CHESTBUFFER.png' 'assets/blocks/chest.png'
