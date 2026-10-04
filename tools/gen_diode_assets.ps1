# Generates the models and the blockstates of the diodes of the line of the power.
#
# A diode is a cube of the casing of its tier that a player turns with the wrench to aim the way a line runs
# through it, so every diode of a tier is drawn with the very same model. This script writes
#
#   * one model per tier, a whole cube of the casing of that tier - the picture a side of a machine of the
#     line that carries a job is drawn over, see MachineCasing, and
#   * one blockstate per diode, which names the model of its tier and the quarter turn of each of the four
#     sides a machine may look in.
#
# The state of a diode is the facing of every machine of the line: it is written by the placer while the
# block is built - turned towards the player who built it - and read by the block entity, see BlockPlacer
# and MachineBlockEntity. The two sides a line runs in and out by are not a state at all: they are the two
# flanks of the block and turn with it, see DiodeMachine.
#
# The pictures are not touched: the casing of a tier is the art of the pack, imported by
# tools/verify/import_gregtech_assets.ps1 long before this script.
#
# Run it from the root of the project:  powershell -File tools/gen_diode_assets.ps1

param(
    [string]$Assets = (Join-Path $PSScriptRoot '..\assets')
)

$ErrorActionPreference = 'Stop'
$Assets = (Resolve-Path $Assets).Path

# The tiers of the line of the power, see MachineFamilies.TIERS: the name of the file of a tier, which is
# also the name in the middle of every one of its diodes.
$TIERS = @('lv', 'mv', 'hv')

# The widths a diode is built in, see Diodes.WIDTHS: one ampere to sixteen, and the narrowest one of a tier
# is named the machine casing of that tier, see Diodes.nameOf.
$WIDTHS = @(1, 2, 4, 8, 16)

function SaveText([string]$path, [string]$text) {
    # Written without a byte order mark: the reader of the game parses the file as plain JSON and would
    # trip over an invisible character in front of the first brace, see tools/gen_pipe_models.ps1.
    [System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))
}

function Write-Model([string]$tier) {
    $picture = 'machine_' + $tier + '/machine_' + $tier
    $lines = @(
        '{',
        '  "elements": [',
        '    {',
        '      "from": [0, 0, 0],',
        '      "to": [16, 16, 16],',
        '      "faces": {',
        ('        "down": {{ "texture": "{0}", "cullface": "down" }},' -f $picture),
        ('        "up": {{ "texture": "{0}", "cullface": "up" }},' -f $picture),
        ('        "north": {{ "texture": "{0}", "cullface": "north" }},' -f $picture),
        ('        "south": {{ "texture": "{0}", "cullface": "south" }},' -f $picture),
        ('        "west": {{ "texture": "{0}", "cullface": "west" }},' -f $picture),
        ('        "east": {{ "texture": "{0}", "cullface": "east" }}' -f $picture),
        '      }',
        '    }',
        '  ]',
        '}',
        ''
    )
    $path = Join-Path $Assets ('models\block\machine_casing_' + $tier + '.json')
    SaveText $path ($lines -join "`n")
    Write-Host ('models/block/machine_casing_' + $tier + '.json')
}

function Write-Blockstate([string]$name, [string]$tier) {
    $model = 'machine_casing_' + $tier
    $lines = @(
        '{',
        '  "properties": {',
        '    "facing": ["north", "east", "south", "west"]',
        '  },',
        '  "variants": {',
        ('    "facing=north": {{ "model": "{0}", "y": 0 }},' -f $model),
        ('    "facing=east": {{ "model": "{0}", "y": 270 }},' -f $model),
        ('    "facing=south": {{ "model": "{0}", "y": 180 }},' -f $model),
        ('    "facing=west": {{ "model": "{0}", "y": 90 }}' -f $model),
        '  }',
        '}',
        ''
    )
    $path = Join-Path $Assets ('blockstates\' + $name + '.json')
    SaveText $path ($lines -join "`n")
    Write-Host ('blockstates/' + $name + '.json')
}

$written = 0
foreach ($tier in $TIERS) {
    Write-Model $tier
    $written++
    foreach ($width in $WIDTHS) {
        $name = if ($width -eq $WIDTHS[0]) { 'machine_casing_' + $tier } else { 'cable_diode_' + $tier + '_' + $width }
        Write-Blockstate $name $tier
        $written++
    }
}
Write-Host ("{0} files written" -f $written)
