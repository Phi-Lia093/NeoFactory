# Generates the models and the blockstates of the transformers of the line of the power.
#
# A transformer is a cube of the casing of the age it is named for that a player turns with the wrench, and
# every size of one age is drawn with the very same model. This script writes
#
#   * one model per age, a whole cube of the casing of that age - the picture a side of a machine of the line
#     that carries a job is drawn over, see MachineCasing, and
#   * one blockstate per transformer, which names the model of its age and the quarter turn of each of the
#     four sides a machine may look in.
#
# The state of a transformer is the facing of every machine of the line, and it is the state that matters more
# here than at any other machine: the front of a transformer is the one side of it that carries the high
# voltage, so a player aims the two ends of it by turning the block, see TransformerMachine.
#
# The pictures are not touched: the casing of an age is the art of the pack, imported by
# tools/verify/import_gregtech_assets.ps1 long before this script.
#
# Run it from the root of the project:  powershell -File tools/gen_transformer_assets.ps1

param(
    [string]$Assets = (Join-Path $PSScriptRoot '..\assets')
)

$ErrorActionPreference = 'Stop'
$Assets = (Resolve-Path $Assets).Path

# The ages a transformer of the game joins, see Transformers.TIERS: the name of the file of the low side of
# it, which is the age it is named for.
$TIERS = @('lv', 'mv')

# The sizes a transformer is built in, see Transformers.SIZES: the amperes of its high side.
$SIZES = @(1, 4, 16)

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
    $path = Join-Path $Assets ('models\block\transformer_' + $tier + '.json')
    SaveText $path ($lines -join "`n")
    Write-Host ('models/block/transformer_' + $tier + '.json')
}

function Write-Blockstate([string]$name, [string]$model) {
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
    $model = 'transformer_' + $tier
    foreach ($size in $SIZES) {
        $name = if ($size -eq $SIZES[0]) { $model } else { $model + '_' + $size }
        Write-Blockstate $name $model
        $written++
    }
}
Write-Host ("{0} files written" -f $written)
