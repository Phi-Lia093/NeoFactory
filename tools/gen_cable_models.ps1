# Writes the art and the models of the cables of the line of the power.
#
# A cable is not a pipe, whatever its shape:
#   * a bare line is solid metal - every face of every box is iconsets/NONE/wire.png, stretched over that
#     face and painted in the colour of the material;
#   * a line with a skin around it wears the skin of its width on the walls of a stub, the whole skin,
#     cable_insulation/full, on the cap of a side it does not join, and the bare core shows in every mouth.
#
# Run it after the art pack is replaced:  powershell -File tools/gen_cable_models.ps1

# Thickness of a line, as the number of sixteenths of a cell its tube is inset from the edge on every side:
# the single line is the narrowest and the sixteenth the widest, so the inset counts down as the width grows.
$sizes = @{ '1x' = 7; '2x' = 6; '4x' = 5; '8x' = 4; '12x' = 3; '16x' = 2 }
$sides = @('north', 'east', 'south', 'west', 'top', 'bottom')
$bits = @{ north = 32; east = 16; south = 8; west = 4; top = 2; bottom = 1 }

$root = Split-Path -Parent $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding($false)
$models = Join-Path $root 'assets\models\block'
$states = Join-Path $root 'assets\blockstates'
$wires = Join-Path $root 'assets\blocks\cable_wire'
$manifest = Join-Path $root 'assets\assets.txt'

New-Item -ItemType Directory -Force -Path $wires | Out-Null
Copy-Item -Force 'D:\textures\blocks\materialicons\NONE\wire.png' (Join-Path $wires 'wire.png')
# The pictures of a skin are kept exactly as the pack draws them, and a darker copy of each of them is written
# next to it. The darker copy is turned into a shade of grey first and darkened after that: a skin has to read
# as black rubber under the colour of any material, and a skin that keeps its own hue turns the colour of the
# metal under it into red rubber or blue rubber - see the colour a block paints its faces with.
Add-Type -AssemblyName System.Drawing
$insulationDir = Join-Path $root 'assets\blocks\cable_insulation'
$shade = 0.18
$skins = @{ '1x' = 'TINY'; '2x' = 'SMALL'; '4x' = 'MEDIUM'; '8x' = 'MEDIUM_PLUS'; '12x' = 'LARGE'
    '16x' = 'HUGE'; 'full' = 'FULL' }
foreach ($size in $skins.Keys) {
    $source = Join-Path 'D:\textures\blocks\iconsets' ('INSULATION_' + $skins[$size] + '.png')
    Copy-Item -Force $source (Join-Path $insulationDir ($size + '.png'))
    # The picture is read into memory before it is written back: a bitmap that keeps the file open cannot be
    # saved over it, which is what GDI+ refuses to do.
    $stream = New-Object System.IO.MemoryStream(, [System.IO.File]::ReadAllBytes($source))
    $bitmap = New-Object System.Drawing.Bitmap($stream)
    for ($x = 0; $x -lt $bitmap.Width; $x++) {
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            $pixel = $bitmap.GetPixel($x, $y)
            if ($pixel.A -eq 0) { continue }
            $grey = [int][Math]::Round(($pixel.R * 0.3) + ($pixel.G * 0.59) + ($pixel.B * 0.11))
            $value = [int][Math]::Round($grey * $shade)
            $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $value, $value, $value))
        }
    }
    $bitmap.Save((Join-Path $insulationDir ($size + '_dark.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    $stream.Dispose()
}
Write-Host 'Wrote the skins of the line, and a dark grey copy of every one'

# The box of the tube of one side and the box of the skin around it, in sixteenths of a block. The skin is
# one sixteenth wider than the tube on every side, so the metal fills the skin and no gap shows between them.
function StubBoxes($side, $t) {
    $t = [int]$t
    $u = [int](16 - [int]$t)
    $a = [int]([int]$t - 1)
    $b = [int]([int]$u + 1)
    switch ($side) {
        'north'  { return @(("$t,$t,0|$u,$u,$t"), ("$a,$a,0|$b,$b,$t")) }
        'east'   { return @(("$u,$t,$t|16,$u,$u"), ("$b,$a,$a|16,$b,$b")) }
        'south'  { return @(("$t,$t,$u|$u,$u,16"), ("$a,$a,$u|$b,$b,16")) }
        'west'   { return @(("0,$t,$t|$t,$u,$u"), ("0,$a,$a|$t,$b,$b")) }
        'top'    { return @(("$t,$u,$t|$u,16,$u"), ("$a,$u,$a|$b,16,$b")) }
        'bottom' { return @(("$t,0,$t|$u,$t,$u"), ("$a,0,$a|$b,$t,$b")) }
    }
}

# One face of a box, as text: the picture and, when it has one, the picture drawn over it.
function Face($side, $texture, $overlay) {
    $face = '"' + $side + '":{"texture":"' + $texture + '"'
    if ($overlay) { $face = $face + ',"overlay":"' + $overlay + '"' }
    return ($face + '}')
}

# One box of a model, as text, with every face of it the same picture.
function Box($box, $texture, $skip, $overlay, $unused) {
    $faces = @()
    foreach ($side in $sides) {
        if ($side -eq $skip) { continue }
        $faces += (Face $side $texture $overlay)
    }
    $half = $box.Split('|')
    return '{"from":[' + $half[0] + '],"to":[' + $half[1] + '],"faces":{' + ($faces -join ',') + '}}'
}

# One box whose walls are one picture and whose face at the end of a run is a ring of the skin over metal.
function BoxNoEnd($box, $wall, $endBase, $endOverlay, $end) {
    $opposite = @{ north = 'south'; south = 'north'; east = 'west'; west = 'east'; top = 'bottom'
        bottom = 'top' }
    $faces = @()
    foreach ($side in $sides) {
        if ($side -eq $end) { $faces += (Face $side $endBase $endOverlay); continue }
        # The face the box turns towards the middle of the line is left out: it lies on the face of the box of
        # the middle, and two faces that lie on each other let the inside of the line show at the seam.
        if ($side -eq $opposite[$end]) { continue }
        $faces += (Face $side $wall '')
    }
    $half = $box.Split('|')
    return '{"from":[' + $half[0] + '],"to":[' + $half[1] + '],"faces":{' + ($faces -join ',') + '}}'
}

# The model of one kind, one width and one mask of connections, as the text of a file.
function ModelOf($size, $mask, $bare) {
    $t = [int]$sizes[$size]
    $mask = [int]$mask
    $skin = 'cable_insulation/' + $size + '_dark'
    $u = [int](16 - [int]$t)
    $core = "$t,$t,$t|" + $u + ',' + $u + ',' + $u
    $half = $core.Split('|')
    $coreBox = $half[0] + '|' + $half[1]
    $boxes = @()
    # The box of the middle: the cap of every side the cable does not join, the skin of the ones it does.
    $faces = @()
    foreach ($side in $sides) {
        $joined = (([int]$mask -band [int]$bits[$side]) -ne 0)
        $overlay = 'cable_insulation/full'
        if ($bare) { $overlay = '' }
        elseif ($joined) { $overlay = $skin }
        if ($bare) {
            $faces += ('"' + $side + '":{"texture":"#wire"}')
        }
        elseif ($joined) {
            $faces += ('"' + $side + '":{"texture":"#wire"}')
        }
        else {
            # A cap: the whole skin, and the metal of the material is not drawn under it at all.
            $faces += ('"' + $side + '":{"texture":"#full"}')
        }
    }
    $boxes += '{"from":[' + $half[0] + '],"to":[' + $half[1] + '],"faces":{' + ($faces -join ',') + '}}'
    foreach ($side in $sides) {
        if (([int]$mask -band [int]$bits[$side]) -eq 0) { continue }
        $stub = StubBoxes $side $t
        if ($bare) { $boxes += (Box $stub[0] '#wire' '' '' '' ''); continue }
        # The skin of a run has no thickness of its own: the walls of the tube are wrapped in it, and the
        # mouth of the line is the ring of the width over the metal of the material, whose middle is open.
        $boxes += (BoxNoEnd $stub[0] '#skin' '#wire' '#skin' $side)
    }
    $textures = '"wire":"cable_wire/wire"'
    if (-not $bare) {
        $textures = '"wire":"cable_wire/wire","skin":"' + $skin + '","full":"cable_insulation/full_dark"'
    }
    return '{"textures":{' + $textures + '},"tint":true,"elements":[' + ($boxes -join ',') + ']}'
}

$written = 0
foreach ($size in $sizes.Keys) {
    foreach ($bare in @($true, $false)) {
        $family = 'cable_wire_' + $size
        if (-not $bare) { $family = 'cable_insulation_' + $size }
        for ($mask = 0; $mask -le 63; $mask++) {
            $text = ModelOf $size $mask $bare
            [System.IO.File]::WriteAllText((Join-Path $models ($family + '_' + $mask.ToString('00') + '.json')), $text, $utf8)
            $written++
        }
    }
}
Write-Host ('Wrote ' + $written + ' models of the line')

# The state of every cable: the six sides it joins, one model per mask, named like the models above.
$keys = @()
for ($mask = 0; $mask -le 63; $mask++) {
    $key = @()
    foreach ($side in $sides) {
        $value = 'false'
        if (([int]$mask -band [int]$bits[$side]) -ne 0) { $value = 'true' }
        $key += ($side + '=' + $value)
    }
    $keys += ($key -join ',')
}
$properties = '"properties":{"north":["false","true"],"east":["false","true"],"south":["false","true"],' +
    '"west":["false","true"],"top":["false","true"],"bottom":["false","true"]}'
$names = Get-ChildItem $states -Filter '*_wire_1x.json' | ForEach-Object { $_.BaseName -replace '_wire_1x$', '' }
foreach ($material in $names) {
    foreach ($size in $sizes.Keys) {
        foreach ($kind in @('wire', 'cable')) {
            $family = 'cable_wire_' + $size
            if ($kind -eq 'cable') { $family = 'cable_insulation_' + $size }
            $variants = @()
            for ($mask = 0; $mask -le 63; $mask++) {
                $variants += ('"' + $keys[$mask] + '":{"model":"' + $family + '_' + $mask.ToString('00') + '","y":0}')
            }
            $text = '{' + "`n" + '  ' + $properties + ',' + "`n" + '  "variants": {' + "`n" + '    ' +
                ($variants -join (',' + "`n" + '    ')) + "`n" + '  }' + "`n" + '}'
            [System.IO.File]::WriteAllText((Join-Path $states ($material + '_' + $kind + '_' + $size + '.json')), $text, $utf8)
        }
    }
}
Write-Host ('Wrote the states of ' + $names.Count + ' materials, every width and both kinds')

$lines = Get-Content $manifest
foreach ($name in @('blocks/cable_wire/wire.png', 'blocks/cable_insulation/1x.png',
        'blocks/cable_insulation/2x.png', 'blocks/cable_insulation/4x.png',
        'blocks/cable_insulation/8x.png', 'blocks/cable_insulation/12x.png',
        'blocks/cable_insulation/16x.png', 'blocks/cable_insulation/full.png',
        'blocks/cable_insulation/1x_dark.png', 'blocks/cable_insulation/2x_dark.png',
        'blocks/cable_insulation/4x_dark.png', 'blocks/cable_insulation/8x_dark.png',
        'blocks/cable_insulation/12x_dark.png', 'blocks/cable_insulation/16x_dark.png',
        'blocks/cable_insulation/full_dark.png')) {
    if ($lines -notcontains $name) {
        Add-Content -Path $manifest -Value $name
        Write-Host ('Added ' + $name + ' to the manifest')
    }
}
