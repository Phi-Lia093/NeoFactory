# Builds the grey scale sheet every fluid of the game is painted from.
#
# The sheet is built from one picture of the art pack - the still water of the original game, whose
# frames and transparency travel with it. All fluids share the result and differ in the colour
# Fluids gives them, see Fluids#SHEET, so one grey scale picture serves water, lava and every fluid
# of the industry that arrives later.
#
# Run it after the art pack is replaced, or to build the sheet from another picture:
#     powershell -File tools/verify/grayscale_fluid.ps1
#     powershell -File tools/verify/grayscale_fluid.ps1 -Source blocks\water_still.png -Target generic_fluid

param(
    [string]$Source = 'blocks\water_still.png',
    [string]$Target = 'generic_fluid'
)

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$source = Join-Path $root (Join-Path 'assets' $Source)
$output = Join-Path $root (Join-Path 'assets\blocks' ($Target + '.png'))

if (-not (Test-Path $source)) {
    Write-Error "Missing $source"
    exit 1
}

$picture = [System.Drawing.Bitmap]::FromFile($source)
try {
    $grey = New-Object System.Drawing.Bitmap($picture.Width, $picture.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt $picture.Height; $y++) {
            for ($x = 0; $x -lt $picture.Width; $x++) {
                $pixel = $picture.GetPixel($x, $y)
                # The brightest channel, so a picture of a colour stays as bright as it was and
                # the colour of the fluid brings the hue back.
                $value = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B))
                $grey.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $value, $value, $value))
            }
        }
        $grey.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "Wrote $output ($($picture.Width)x$($picture.Height))"
    } finally {
        $grey.Dispose()
    }
} finally {
    $picture.Dispose()
}
