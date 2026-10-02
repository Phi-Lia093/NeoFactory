# Writes the art and the models of the cables of the line of the power.
#
# A cable is not a pipe, whatever its shape:
#   * a bare line is solid metal - every face of every box is iconsets/NONE/wire.png, stretched over that
#     face and painted in the colour of the material;
#   * a line with a skin around it wears the skin of its width on the walls of a stub, the whole skin,
#     cable_insulation/full, on the cap of a side it does not join, and the bare core shows in every mouth.
#
# Run it after the art pack is replaced:  powershell -File tools/gen_cable_models.ps1

$sizes = @{ '1x' = 2; '2x' = 3; '4x' = 4; '8x' = 5; '12x' = 6; '16x' = 7 }
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

# The box of a stub of one side, as text: the outer box and the core inside it, in sixteenths of a block.
function StubBoxes($side, $t) {
    $t = [int]$t
    $u = [int](16 - [int]$t)
    $i = [int]([int]$t + 1)
    $j = [int]([int]$u - 1)
    # A core only fits inside a stub that is wide enough: a box of no thickness is no box, and a model with
    # one is refused by the loader, which is why a wide cable has an open mouth instead of a core in it.
    $hasCore = $i -le $j
    if (-not $hasCore) { $i = $t; $j = $u }
    switch ($side) {
        'north'  { return @(("$t,$t,0|$u,$u,$t"), ("$i,$i,0|$j,$j,$t"), $hasCore) }
        'east'   { return @(("$u,$t,$t|16,$u,$u"), ("$u,$i,$i|16,$j,$j"), $hasCore) }
        'south'  { return @(("$t,$t,$u|$u,$u,16"), ("$i,$i,$u|$j,$j,16"), $hasCore) }
        'west'   { return @(("0,$t,$t|$t,$u,$u"), ("0,$i,$i|$t,$j,$j"), $hasCore) }
        'top'    { return @(("$t,$u,$t|$u,16,$u"), ("$i,$u,$i|$j,16,$j"), $hasCore) }
        'bottom' { return @(("$t,0,$t|$u,$t,$u"), ("$i,0,$i|$j,$t,$j"), $hasCore) }
    }
}

# One box of a model, as text. A side that is left out of $skip is not drawn at all. The overlay is drawn
# over the picture without the colour of the material, which is what keeps the skin of a cable black.
function Box($box, $texture, $skip, $overlay) {
    $faces = @()
    foreach ($side in $sides) {
        if ($side -eq $skip) { continue }
        $face = '"' + $side + '":{"texture":"' + $texture + '"'
        if ($overlay) { $face = $face + ',"overlay":"' + $overlay + '"' }
        $faces += ($face + '}')
    }
    $half = $box.Split('|')
    return '{"from":[' + $half[0] + '],"to":[' + $half[1] + '],"faces":{' + ($faces -join ',') + '}}'
}

# The model of one kind, one width and one mask of connections, as the text of a file.
function ModelOf($size, $mask, $bare) {
    $t = [int]$sizes[$size]
    $mask = [int]$mask
    $skin = 'cable_insulation/' + $size
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
        $face = '"' + $side + '":{"texture":"#wire"'
        if ($overlay) { $face = $face + ',"overlay":"#skin"' }
        if (-not $bare -and -not $joined) { $face = '"' + $side + '":{"texture":"#wire","overlay":"#full"' }
        $faces += ($face + '}')
    }
    $boxes += '{"from":[' + $half[0] + '],"to":[' + $half[1] + '],"faces":{' + ($faces -join ',') + '}}'
    foreach ($side in $sides) {
        if (([int]$mask -band [int]$bits[$side]) -eq 0) { continue }
        $stub = StubBoxes $side $t
        if ($bare) { $boxes += (Box $stub[0] '#wire' '' ''); continue }
        # The mouth of a joined side is open while a core fits inside the stub: the face of the stub that
        # stands at the face of the block is left out and the bare core is drawn in it, so the metal of the
        # line shows through the skin. A cable too wide for a core keeps that face and is closed with it.
        $open = ''
        if ($stub[2]) { $open = $side }
        $boxes += (Box $stub[0] '#wire' $open '#skin')
        if ($stub[2]) { $boxes += (Box $stub[1] '#wire' '' '') }
    }
    $textures = '"wire":"cable_wire/wire"'
    if (-not $bare) {
        $textures = '"wire":"cable_wire/wire","skin":"' + $skin + '","full":"cable_insulation/full"'
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
if ($lines -notcontains 'blocks/cable_wire/wire.png') {
    Add-Content -Path $manifest -Value 'blocks/cable_wire/wire.png'
    Write-Host 'Added blocks/cable_wire/wire.png to the manifest'
}
