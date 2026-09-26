# Generates the models and the blockstates of the pipes of the game.
#
# A pipe is drawn from the state of its cell, and that state is the mask of the six directions it is
# joined in, see Pipes. Sixty four masks are more models than a hand writes, so this script writes them
# from the one number that describes a pipe: the thickness of its tube, see PipeSize. It writes
#
#   * one model per family, size and canonical mask - sixteen masks per size, because the four quarter
#     turns of a mask are the same pipe drawn from another side, see Pipes.canonical,
#   * one blockstate per pipe block, naming the model and the quarter turn of each of the sixty four
#     states, and
#   * one model per pipe block, which is a line pointing at the straight pipe of its size, so the item
#     of a pipe is drawn as a piece of pipe and a lost blockstate still draws something.
#
# The pictures are not touched: they are the art of the pack, copied into assets/blocks/pipe_metal and
# assets/blocks/pipe_wood, see PipeTexture.
#
# Run it from the root of the project:  powershell -File tools/gen_pipe_models.ps1

param(
    [string]$Assets = (Join-Path $PSScriptRoot '..\assets')
)

$ErrorActionPreference = 'Stop'
$Assets = (Resolve-Path $Assets).Path

# The sizes of the game, see PipeSize: the thickness of the tube in sixteenths of a block and whether
# the size carries a bundle of tubes instead of one.
$SIZES = @(
    @{ Name = 'tiny';      Thickness = 4;  Bundle = $false },
    @{ Name = 'small';     Thickness = 6;  Bundle = $false },
    @{ Name = 'medium';    Thickness = 8;  Bundle = $false },
    @{ Name = 'large';     Thickness = 10; Bundle = $false },
    @{ Name = 'huge';      Thickness = 14; Bundle = $false },
    @{ Name = 'quadruple'; Thickness = 16; Bundle = $true },
    @{ Name = 'nonuple';   Thickness = 16; Bundle = $true }
)

# The families of art, see PipeTexture: a folder below assets/blocks and whether the colour of the
# material is multiplied over it.
$FAMILIES = @(
    @{ Name = 'metal'; Tint = $true },
    @{ Name = 'wood';  Tint = $false }
)

# The materials of the game, see PipeMaterials, in the order their blocks are numbered in.
$MATERIALS = @(
    @{ Name = 'wood';   Family = 'wood' },
    @{ Name = 'copper'; Family = 'metal' },
    @{ Name = 'bronze'; Family = 'metal' },
    @{ Name = 'steel';  Family = 'metal' }
)

# The six directions in the order the properties of a pipe are declared in, see Pipes. The order is
# what the bits weigh: the first direction is the highest bit of a mask.
$DIRECTIONS = @('north', 'east', 'south', 'west', 'top', 'bottom')

# The direction across from a direction, used by a bundle: a tube that runs towards one side runs away
# from the other.
$OPPOSITE = @(2, 3, 0, 1, 5, 4)

# The mask of a straight run along the north-south axis, the picture the item of a pipe shows.
$STRAIGHT = 32 + 8

function Bit([int]$index) {
    return 1 -shl (5 - $index)
}

# The mask of a quarter turn of the world, see Pipes.turned: north becomes west, east becomes north,
# south becomes east, west becomes south, and the two vertical directions stay where they are.
function Turn([int]$mask) {
    return ((($mask -band 32) -shr 3) -bor (($mask -band 28) -shl 1) -bor ($mask -band 3))
}

function Canonical([int]$mask) {
    $best = $mask
    $turned = $mask
    for ($quarter = 1; $quarter -lt 4; $quarter++) {
        $turned = Turn $turned
        if ($turned -lt $best) { $best = $turned }
    }
    return $best
}

# Quarter turns a state draws the model of its canonical mask with, see Pipes.turnsToDraw.
function TurnsToDraw([int]$mask) {
    $canonical = Canonical $mask
    $turned = $mask
    for ($quarter = 0; $quarter -lt 4; $quarter++) {
        if ($turned -eq $canonical) { return (4 - $quarter) % 4 }
        $turned = Turn $turned
    }
    return 0
}

function ModelName([string]$family, [string]$size, [int]$mask) {
    return 'pipe_{0}_{1}_{2:d2}' -f $family, $size, (Canonical $mask)
}

function SizeOf([string]$name) {
    foreach ($size in $SIZES) { if ($size.Name -eq $name) { return $size } }
    throw "No such pipe size: $name"
}

# ------------------------------------------------------------------
# The geometry of one pipe: the boxes its model is made of.
# ------------------------------------------------------------------

function Connected([int]$mask, [int]$index) {
    return ($mask -band (Bit $index)) -ne 0
}

# The four faces that run along a tube, the ones the picture of a side covers.
function SideFaces([int]$index) {
    $faces = @{}
    for ($other = 0; $other -lt 6; $other++) {
        if ($other -eq $index -or $other -eq $OPPOSITE[$index]) { continue }
        $faces[$DIRECTIONS[$other]] = 'side'
    }
    return $faces
}

# The box of one arm of a tube: from the core of the pipe to the border of the block.
function ArmBox($size, [int]$index) {
    $from = [int]((16 - $size.Thickness) / 2)
    $to = [int]((16 + $size.Thickness) / 2)
    switch ($DIRECTIONS[$index]) {
        'north' { return @{ From = @($from, $from, 0); To = @($to, $to, $from) } }
        'south' { return @{ From = @($from, $from, $to); To = @($to, $to, 16) } }
        'west'  { return @{ From = @(0, $from, $from); To = @($from, $to, $to) } }
        'east'  { return @{ From = @($to, $from, $from); To = @(16, $to, $to) } }
        'top'   { return @{ From = @($from, $to, $from); To = @($to, 16, $to) } }
        default { return @{ From = @($from, 0, $from); To = @($to, $from, $to) } }
    }
}

# The boxes of one size joined in one way.
function Boxes($size, [int]$mask) {
    if ($size.Bundle) {
        # A bundle fills its cell, so it is one box and every one of its six sides carries the plate of the
        # bundle - the picture that shows four or nine tubes side by side. A bundle is a block of tubes and
        # not a tube: a player reads it by its cross section, wherever they look at it, which is also what
        # the item of a bundle shows in a slot, see Pipes#itemState. The column of a bundle is therefore made
        # of the plate and of nothing else, and no side of it is left out, because a side that is left out is
        # a hole a player looks straight through, see Boxes.
        $faces = @{}
        for ($index = 0; $index -lt 6; $index++) {
            $faces[$DIRECTIONS[$index]] = 'end'
        }
        return @{ From = @(0, 0, 0); To = @(16, 16, 16); Faces = $faces }
    }
    $boxes = New-Object System.Collections.Generic.List[hashtable]
    # The core, whose sides look out where the pipe is not joined: a bare tube ends there.
    $from = [int]((16 - $size.Thickness) / 2)
    $to = [int]((16 + $size.Thickness) / 2)
    $coreFaces = @{}
    for ($index = 0; $index -lt 6; $index++) {
        if (-not (Connected $mask $index)) { $coreFaces[$DIRECTIONS[$index]] = 'end' }
    }
    if ($coreFaces.Count -eq 0) {
        # Every side is joined, so the core is the joint the six arms meet in. Its faces are inside the tube
        # and a player never sees them, but they have to be there: without them the six arms enclose an open
        # middle and a fully joined pipe - the cross of a line - is looked through.
        for ($index = 0; $index -lt 6; $index++) {
            $coreFaces[$DIRECTIONS[$index]] = 'side'
        }
    }
    $boxes.Add(@{ From = @($from, $from, $from); To = @($to, $to, $to); Faces = $coreFaces })
    for ($index = 0; $index -lt 6; $index++) {
        if (-not (Connected $mask $index)) { continue }
        # The arm of the tube, which reaches the border of the block and carries the plate of this size
        # there. Two pipes of the same size put the very same plate against each other, so the seam
        # between them is invisible and a line reads as one tube; two pipes of different sizes cover one
        # plate with the other, so the plate of the wider tube is what shows - the collar of a reducer,
        # which is the only place the art of a pipe is seen from the end.
        $arm = ArmBox $size $index
        $arm.Faces = SideFaces $index
        $arm.Faces[$DIRECTIONS[$index]] = 'end'
        $boxes.Add($arm)
    }
    return $boxes.ToArray()
}


# ------------------------------------------------------------------
# Writing the files.
# ------------------------------------------------------------------

function Element([hashtable]$box) {
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('    {')
    $lines.Add('      "from": [' + ($box.From -join ', ') + '],')
    $lines.Add('      "to": [' + ($box.To -join ', ') + '],')
    $lines.Add('      "faces": {')
    $body = New-Object System.Collections.Generic.List[string]
    foreach ($direction in $DIRECTIONS) {
        if ($box.Faces.ContainsKey($direction)) {
            $body.Add('        "' + $direction + '": { "texture": "#' + $box.Faces[$direction] + '" }')
        }
    }
    $lines.Add(($body -join ",`n"))
    $lines.Add('      }')
    $lines.Add('    }')
    return ($lines -join "`n")
}

function SaveText([string]$path, [string]$text) {
    # Written without a byte order mark: the reader of the game parses the file as plain JSON and would
    # trip over an invisible character in front of the first brace.
    [System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))
}

# One model of one family and size, joined in one way.
function WriteModel([string]$family, [string]$size, [int]$mask, [bool]$tinted) {
    $path = Join-Path $Assets "models/block/$(ModelName $family $size $mask).json"
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('{')
    $lines.Add('  "textures": {')
    $lines.Add('    "side": "pipe_' + $family + '/side",')
    $lines.Add('    "end": "pipe_' + $family + '/' + $size + '"')
    $lines.Add('  },')
    if ($tinted) {
        $lines.Add('  "tint": true,')
    }
    $lines.Add('  "elements": [')
    $elements = New-Object System.Collections.Generic.List[string]
    foreach ($box in (Boxes (SizeOf $size) $mask)) {
        # A box whose every side is covered by the tube next to it is not written down: no pipe of the game
        # has such a box any more, because a box that is left out entirely is a hole - the core of a fully
        # joined pipe and every side of a bundle carry the faces they need, see Boxes.
        if ($box.Faces.Count -eq 0) { continue }
        $elements.Add((Element $box))
    }
    $lines.Add(($elements -join ",`n"))
    $lines.Add('  ]')
    $lines.Add('}')
    SaveText $path ($lines -join "`n")
}

# The state of one pipe block: the six directions as properties and one variant per way to join them.
function WriteBlockState([string]$material, [string]$family, [string]$size) {
    $path = Join-Path $Assets "blockstates/${material}_pipe_$size.json"
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('{')
    $lines.Add('  "properties": {')
    for ($index = 0; $index -lt 6; $index++) {
        $comma = ''
        if ($index -lt 5) { $comma = ',' }
        $lines.Add('    "' + $DIRECTIONS[$index] + '": ["false", "true"]' + $comma)
    }
    $lines.Add('  },')
    $lines.Add('  "variants": {')
    $variants = New-Object System.Collections.Generic.List[string]
    for ($mask = 0; $mask -lt 64; $mask++) {
        $key = New-Object System.Collections.Generic.List[string]
        for ($index = 0; $index -lt 6; $index++) {
            $value = 'false'
            if (Connected $mask $index) { $value = 'true' }
            $key.Add($DIRECTIONS[$index] + '=' + $value)
        }
        $comma = ''
        if ($mask -lt 63) { $comma = ',' }
        $model = ModelName $family $size $mask
        $turn = 90 * (TurnsToDraw $mask)
        $variant = '    "' + ($key -join ',') + '": { "model": "' + $model + '", "y": ' + $turn + ' }' + $comma
        $variants.Add($variant)
    }
    $lines.Add(($variants -join "`n"))
    $lines.Add('  }')
    $lines.Add('}')
    SaveText $path ($lines -join "`n")
}

# The model of one pipe block: a line pointing at the straight pipe of its size, which is what the item
# of a pipe is drawn from.
function WriteBlockModel([string]$material, [string]$family, [string]$size) {
    $path = Join-Path $Assets "models/block/${material}_pipe_$size.json"
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('{')
    $lines.Add('  "parent": "' + (ModelName $family $size $STRAIGHT) + '"')
    $lines.Add('}')
    SaveText $path ($lines -join "`n")
}

# ------------------------------------------------------------------
# The run: every canonical model of every family and size, then the blockstates and block models of
# every pipe of the game.
# ------------------------------------------------------------------

$models = 0
$states = 0
foreach ($family in $FAMILIES) {
    foreach ($size in $SIZES) {
        $written = @{}
        foreach ($mask in 0..63) {
            $canonical = Canonical $mask
            if ($written.ContainsKey($canonical)) { continue }
            $written[$canonical] = $true
            WriteModel $family.Name $size.Name $canonical $family.Tint
            $models++
        }
    }
}
foreach ($material in $MATERIALS) {
    foreach ($size in $SIZES) {
        WriteBlockState $material.Name $material.Family $size.Name
        WriteBlockModel $material.Name $material.Family $size.Name
        $states++
    }
}
Write-Output "Wrote $models models and $states blockstates plus their block models."

