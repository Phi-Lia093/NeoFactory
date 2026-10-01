# Imports the art of the cables of the line of the power and writes every file that is built from it.
#
# A cable is drawn twice: the bare line is the grey scale tube of a metal, see PipeTexture, and the wrapped
# one wears a skin of the pack. The skins come out of iconsets/, where the pack keeps one picture per width
# of a line, and this script copies them under the names of CableSize, writes the models of the wrapped line
# out of the models of a tube, and writes one blockstate per material, width and kind of CableKind.
#
# Run it after the art pack is replaced:
#     powershell -File tools/import_cables.ps1

$sizes = [ordered]@{
    '1x'  = 'TINY'
    '2x'  = 'SMALL'
    '4x'  = 'MEDIUM'
    '8x'  = 'MEDIUM_PLUS'
    '12x' = 'LARGE'
    '16x' = 'HUGE'
}

# Width of a cable -> size of the tube whose geometry it is drawn with, see CableSize#tube().
$tubes = [ordered]@{
    '1x'  = 'tiny'
    '2x'  = 'small'
    '4x'  = 'medium'
    '8x'  = 'large'
    '12x' = 'huge'
    '16x' = 'huge'
}

$root = Split-Path -Parent $PSScriptRoot
$iconsets = 'D:\textures\blocks\iconsets'
$insulation = Join-Path $root 'assets\blocks\cable_insulation'
$models = Join-Path $root 'assets\models\block'
$states = Join-Path $root 'assets\blockstates'
$manifest = Join-Path $root 'assets\assets.txt'

# The skins of the pack, under the names of the widths of a cable.
New-Item -ItemType Directory -Force -Path $insulation | Out-Null
foreach ($size in $sizes.Keys) {
    Copy-Item -Force (Join-Path $iconsets ('INSULATION_' + $sizes[$size] + '.png')) `
        (Join-Path $insulation ($size + '.png'))
}
Copy-Item -Force (Join-Path $iconsets 'INSULATION_FULL.png') (Join-Path $insulation 'full.png')

# The models of a wrapped line: the geometry of a tube with the skin of the pack on it.
$written = 0
foreach ($size in $tubes.Keys) {
    $tube = $tubes[$size]
    foreach ($file in Get-ChildItem $models -Filter ('pipe_metal_' + $tube + '_*.json')) {
        $text = Get-Content $file.FullName -Raw
        $text = $text.Replace('"pipe_metal/side"', ('"cable_insulation/' + $size + '"'))
        $text = $text.Replace(('"pipe_metal/' + $tube + '"'), ('"cable_insulation/' + $size + '"'))
        $mask = $file.BaseName.Substring($file.BaseName.LastIndexOf('_') + 1)
        Set-Content -NoNewline -Value $text -Path (Join-Path $models ('cable_insulation_' + $size + '_' + $mask + '.json'))
        $written++
    }
}
Write-Host "Wrote $written models of the wrapped line"

# One blockstate per material, width and kind: a bare line draws the tube of its width, a wrapped one the
# skin of it, and both name the models that stand above.
$materials = Get-ChildItem $states -Filter '*_cable_1x.json' |
    ForEach-Object { $_.BaseName -replace '_cable_1x$', '' }
foreach ($material in $materials) {
    foreach ($size in $tubes.Keys) {
        $tube = $tubes[$size]
        $wire = Get-Content (Join-Path $states ('bronze_pipe_' + $tube + '.json')) -Raw
        Set-Content -NoNewline -Value $wire -Path (Join-Path $states ($material + '_wire_' + $size + '.json'))
        $cable = $wire.Replace(('pipe_metal_' + $tube + '_'), ('cable_insulation_' + $size + '_'))
        Set-Content -NoNewline -Value $cable -Path (Join-Path $states ($material + '_cable_' + $size + '.json'))
    }
}
Write-Host ("Wrote blockstates for " + $materials.Count + " materials, every width and both kinds")

# The pictures of a skin are new files, so the audit has to hear about them.
$lines = Get-Content $manifest
foreach ($size in $sizes.Keys) {
    $line = 'blocks/cable_insulation/' + $size + '.png'
    if ($lines -notcontains $line) {
        Add-Content -Path $manifest -Value $line
        Write-Host ("Added " + $line + " to the manifest")
    }
}
