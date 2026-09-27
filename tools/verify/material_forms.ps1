# Copies the grey scale shapes every material of the game is drawn from.
#
# A material owns no picture of its own: the shapes do, and the colour of the material is
# multiplied over them while they are drawn, see MaterialForm. The shapes come from NONE/, which
# keeps the art pack the project was drawn from, and this script copies the ones the game uses into
# assets/items under the name the form asks for. Because the form spells the name out, a shape that
# is renamed or lost fails the build instead of showing up as a missing icon, see TextureAuditTest.
#
# Run it after the art pack is replaced:
#     powershell -File tools/verify/material_forms.ps1

$map = [ordered]@{
    'dust'             = 'generic_dust'
    'dustSmall'        = 'generic_dust_small'
    'dustTiny'         = 'generic_dust_tiny'
    'ingot'            = 'generic_ingot'
    'nugget'           = 'generic_nugget'
    'plate'            = 'generic_plate'
    'foil'             = 'generic_foil'
    'stick'            = 'generic_rod'
    'stickLong'        = 'generic_long_rod'
    'bolt'             = 'generic_bolt'
    'screw'            = 'generic_screw'
    'ring'             = 'generic_ring'
    'round'            = 'generic_round'
    'wireFine'         = 'generic_fine_wire'
    'wireFine_OVERLAY' = 'generic_fine_wire_overlay'
    'spring'           = 'generic_spring'
    'springSmall'      = 'generic_small_spring'
    'gearGt'           = 'generic_gear'
    'gearGtSmall'      = 'generic_small_gear'
    'rotor'            = 'generic_rotor'
}

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$source = Join-Path $root 'NONE'
$target = Join-Path $root 'assets\items'

if (-not (Test-Path $source)) {
    Write-Error "Missing $source"
    exit 1
}

$missing = @()
foreach ($name in $map.Keys) {
    $from = Join-Path $source ($name + '.png')
    if (-not (Test-Path $from)) {
        $missing += $name
        continue
    }
    Copy-Item -Force $from (Join-Path $target ($map[$name] + '.png'))
    Write-Host ("{0,-20} -> items\{1}.png" -f $name, $map[$name])
}

if ($missing.Count -gt 0) {
    Write-Error ("Missing shapes in NONE/: " + ($missing -join ', '))
    exit 1
}

Write-Host "Wrote $($map.Count) shapes into assets\items"
