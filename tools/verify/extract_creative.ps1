# Copies the pieces of the creative inventory out of the original art pack and into
# the sheet the interface panels come from, gui/inventory_icons.png.
#
# The pack keeps its container art in one picture per screen, while this project cuts
# every panel out of a single sheet, see PanelTextures. The sheet only used its upper
# left corner so far, which is too small for a 195 x 136 panel, so it is grown from
# 256 x 256 to 512 x 512 and the creative art lands on the right half.
#
# Run it from the project root:  powershell -File tools/verify/extract_creative.ps1
param([string]$Assets = 'D:\NeoFactory\assets',
      [string]$Out    = 'D:\NeoFactory\build\verify')

Add-Type -AssemblyName System.Drawing

$sheetPath = "$Assets\gui\inventory_icons.png"
$pack = "$Assets\gui\container\creative_inventory"

# Pieces to copy: source picture, source rectangle, target position.
#
# The two scroll bar thumbs are the pair the original game keeps for a list that fits
# into the panel and for one that does not: the bright one on the left is dragged, the
# dark one on the right tells the player there is nothing to scroll. The track itself
# is part of the panel picture, so it is not copied.
$pieces = @(
    @{ Name = 'panel_items';    From = 'tab_items.png';       X = 0;   Y = 0;  W = 195; H = 136; To = 256; ToY = 0 },
    @{ Name = 'panel_search';   From = 'tab_item_search.png'; X = 0;   Y = 0;  W = 195; H = 136; To = 256; ToY = 136 },
    @{ Name = 'tab_idle';       From = 'tabs.png';            X = 0;   Y = 2;  W = 27;  H = 31;  To = 256; ToY = 280 },
    @{ Name = 'tab_active';     From = 'tabs.png';            X = 0;   Y = 33; W = 27;  H = 31;  To = 285; ToY = 280 },
    @{ Name = 'scroll_active';  From = 'tabs.png';            X = 232; Y = 0;  W = 12;  H = 15;  To = 256; ToY = 316 },
    @{ Name = 'scroll_idle';    From = 'tabs.png';            X = 244; Y = 0;  W = 12;  H = 15;  To = 270; ToY = 316 }
)

$sheet = [System.Drawing.Bitmap]::FromFile($sheetPath)
"sheet is $($sheet.Width)x$($sheet.Height)"

# A fresh canvas carries the old sheet in its upper left corner, so the coordinates the
# game already uses keep pointing at the same pixels. Running the script a second time is
# safe: only the original 256 square corner is read back, while the right half that holds
# the creative art is written from scratch every time.
$original = 256
$grown = New-Object System.Drawing.Bitmap -ArgumentList 512, 512,
        ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($y = 0; $y -lt [Math]::Min($original, $sheet.Height); $y++) {
    for ($x = 0; $x -lt [Math]::Min($original, $sheet.Width); $x++) {
        $grown.SetPixel($x, $y, $sheet.GetPixel($x, $y))
    }
}
$sheet.Dispose()

foreach ($piece in $pieces) {
    $source = [System.Drawing.Bitmap]::FromFile("$pack\$($piece.From)")
    $visible = 0
    for ($row = 0; $row -lt $piece.H; $row++) {
        for ($column = 0; $column -lt $piece.W; $column++) {
            $colour = $source.GetPixel($piece.X + $column, $piece.Y + $row)
            $grown.SetPixel($piece.To + $column, $piece.ToY + $row, $colour)
            if ($colour.A -gt 0) { $visible++ }
        }
    }
    $source.Dispose()
    $share = [math]::Round(100.0 * $visible / ($piece.W * $piece.H), 1)
    "$($piece.Name.PadRight(13)) $($piece.W)x$($piece.H) from $($piece.From) at $($piece.X),$($piece.Y) -> $($piece.To),$($piece.ToY) : $visible pixels ($share%)"
}

$grown.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
"saved $sheetPath ($($grown.Width)x$($grown.Height))"

# A zoomed picture of the right half, so the copy can be checked without a viewer
# that shows alpha.
$zoom = 2
$crop = New-Object System.Drawing.Bitmap -ArgumentList (256 * $zoom), (340 * $zoom)
$graphics = [System.Drawing.Graphics]::FromImage($crop)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$graphics.Clear([System.Drawing.Color]::FromArgb(255, 32, 32, 32))
$source = New-Object System.Drawing.Rectangle -ArgumentList 256, 0, 256, 340
$target = New-Object System.Drawing.Rectangle -ArgumentList 0, 0, (256 * $zoom), (340 * $zoom)
$graphics.DrawImage($grown, $target, $source, [System.Drawing.GraphicsUnit]::Pixel)
$graphics.Dispose()
$crop.Save("$Out\creative_sheet.png", [System.Drawing.Imaging.ImageFormat]::Png)
$crop.Dispose()
"preview written to $Out\creative_sheet.png"
$grown.Dispose()
