# Runs the game, switches a world to creative mode and works the creative inventory
# with the real mouse and keyboard, so the runtime path of the screen is checked and
# not only the unit tests.
#
# Like the chat run, every step is confirmed against the log the game writes itself:
# the world is only entered when "Entered world" shows up, the screen is only worked
# when "Creative inventory opened" arrived and the mode is read back from the status
# line of the log. Clicks are placed in virtual pixels of the interface, which are
# computed from the size of the window exactly the way GuiViewport does it.
param([string]$Jar = 'D:\NeoFactory\lwjgl3\build\libs\NeoFactory-1.0.0.jar',
      [string]$Work = 'D:\NeoFactory\build\smoke',
      [string]$Source = 'D:\NeoFactory\run\saves\save_muar9pxp',
      [string]$Out  = 'D:\NeoFactory\build\verify')

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type -Namespace Win32 -Name Api -MemberDefinition @'
[StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
[StructLayout(LayoutKind.Sequential)] public struct POINT { public int X; public int Y; }
[DllImport("user32.dll")] public static extern bool SetForegroundWindow(System.IntPtr hWnd);
[DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
[DllImport("user32.dll")] public static extern bool MoveWindow(System.IntPtr hWnd, int x, int y, int width, int height, bool repaint);
[DllImport("user32.dll")] public static extern bool GetClientRect(System.IntPtr hWnd, out RECT rect);
[DllImport("user32.dll")] public static extern bool ClientToScreen(System.IntPtr hWnd, ref POINT point);
[DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, System.UIntPtr extra);
'@

[Win32.Api]::SetProcessDPIAware() | Out-Null

$gameLog = "$Work\logs\neofactory.log"
$before = @(Get-Process java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id)
New-Item -ItemType Directory -Force -Path $Work | Out-Null
New-Item -ItemType Directory -Force -Path "$Work\saves" | Out-Null
Remove-Item -Recurse -Force "$Work\saves\*" -ErrorAction SilentlyContinue
Copy-Item -Recurse -Force $Source "$Work\saves\"
Remove-Item -Recurse -Force "$Work\logs" -ErrorAction SilentlyContinue

$proc = Start-Process -FilePath 'java' -ArgumentList '-jar', $Jar -WorkingDirectory $Work `
    -RedirectStandardOutput "$Out\run-creative-stdout.log" -RedirectStandardError "$Out\run-creative-stderr.log" -PassThru
Start-Sleep -Seconds 12

$window = 0
$target = $null
foreach ($p in (Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Sort-Object StartTime -Descending)) {
    if ($before -notcontains $p.Id) { $target = $p; $window = $p.MainWindowHandle; break }
}
if ($window -eq 0) {
    "no window found, started pid $($proc.Id)"
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    exit 1
}
"window handle $window of pid $($target.Id)"
[Win32.Api]::MoveWindow($window, 0, 0, 900, 640, $true) | Out-Null
[Win32.Api]::SetForegroundWindow($window) | Out-Null
Start-Sleep -Seconds 1

$rect = New-Object Win32.Api+RECT
[Win32.Api]::GetClientRect($window, [ref]$rect) | Out-Null
$origin = New-Object Win32.Api+POINT
[Win32.Api]::ClientToScreen($window, [ref]$origin) | Out-Null
$clientW = $rect.Right - $rect.Left
$clientH = $rect.Bottom - $rect.Top
"client ${clientW}x${clientH} at $($origin.X),$($origin.Y)"

# The interface is drawn in virtual pixels that are blown up by a whole number, see
# GuiViewport. The panel of the creative inventory is centred in those pixels and is
# 195 by 136 of them, the tabs stand 31 above its upper edge.
$scale = [Math]::Max(1, [Math]::Min([Math]::Min([int]($clientW / 320), [int]($clientH / 240)), 4))
$guiW = $clientW / $scale
$guiH = $clientH / $scale
$panelX = [Math]::Round(($guiW - 195) / 2)
$panelY = [Math]::Round(($guiH - 136) / 2)
"interface ${guiW}x${guiH} virtual pixels, scale $scale, panel at $panelX,$panelY"

function Lines() {
    if (Test-Path $gameLog) { Get-Content $gameLog -ErrorAction SilentlyContinue } else { @() }
}

function WaitFor([string]$pattern, [int]$seconds) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        if ((Lines) -match $pattern) { return $true }
        Start-Sleep -Milliseconds 400
    }
    return $false
}

function State([string]$label) {
    $line = (Lines | Where-Object { $_ -match 'World .* \| Block' } | Select-Object -Last 1)
    if (-not $line) { "$label : no status line yet"; return }
    $block = [regex]::Match($line, 'Block \((-?\d+), (-?\d+)\)').Groups[0].Value
    $mode = [regex]::Match($line, '\| mode (\w+)').Groups[1].Value
    $inv = [regex]::Match($line, '\| inventory (.+?) \| chat').Groups[1].Value
    "$label : $block mode=$mode inventory=$inv"
}

# The answer of /gamemode goes into the chat, which is not the log file, so the mode
# is read from the status line the screen writes every couple of seconds.
function WaitMode([string]$mode, [int]$seconds) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        $line = (Lines | Where-Object { $_ -match 'World .* \| Block' } | Select-Object -Last 1)
        if ($line -match "\| mode $mode \|") { return $true }
        Start-Sleep -Milliseconds 400
    }
    return $false
}

function Capture([string]$name) {
    $bmp = New-Object System.Drawing.Bitmap -ArgumentList $clientW, $clientH
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($origin.X, $origin.Y, 0, 0, $bmp.Size)
    $g.Dispose()
    $bmp.Save("$Out\$name.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    "captured $name"
}

# Moves the mouse to a point of the interface, given in its virtual pixels with Y
# measured upwards the way the screen draws.
function Point([double]$gx, [double]$gy) {
    $px = $origin.X + [int]($gx * $scale)
    $py = $origin.Y + [int](($guiH - $gy) * $scale)
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList $px, $py
    Start-Sleep -Milliseconds 300
}

function ClickGui([double]$gx, [double]$gy, [int]$count = 1) {
    Point $gx $gy
    for ($i = 0; $i -lt $count; $i++) {
        [Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero)
        [Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero)
        Start-Sleep -Milliseconds 140
    }
    "clicked $gx,$gy x$count"
}

# Drags the mouse between two points of the interface with the left button held. The
# moves have to arrive between the press and the release, because that is what the game
# reads as a drag; they are sent as relative moves, which is what a real held button
# produces, and the absolute placement makes sure the pointer really arrives.
function DragGui([double]$fromX, [double]$fromY, [double]$toX, [double]$toY, [int]$steps = 8) {
    $prevX = $origin.X + [int]($fromX * $scale)
    $prevY = $origin.Y + [int](($guiH - $fromY) * $scale)
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList $prevX, $prevY
    Start-Sleep -Milliseconds 350
    [Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero)
    Start-Sleep -Milliseconds 150
    for ($i = 1; $i -le $steps; $i++) {
        $t = $i / $steps
        $nextX = $origin.X + [int](($fromX + ($toX - $fromX) * $t) * $scale)
        $nextY = $origin.Y + [int](($guiH - ($fromY + ($toY - $fromY) * $t)) * $scale)
        $dx = [BitConverter]::ToUInt32([BitConverter]::GetBytes([int]($nextX - $prevX)), 0)
        $dy = [BitConverter]::ToUInt32([BitConverter]::GetBytes([int]($nextY - $prevY)), 0)
        [Win32.Api]::mouse_event(0x0001, $dx, $dy, 0, [System.UIntPtr]::Zero)
        [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList $nextX, $nextY
        $prevX = $nextX
        $prevY = $nextY
        Start-Sleep -Milliseconds 120
    }
    [Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero)
    Start-Sleep -Milliseconds 250
    "dragged $fromX,$fromY -> $toX,$toY"
}

# Sends notches of the wheel, negative for turning down. The delta is signed and has to
# reach the call as a bit pattern, because a cast of a negative number would throw.
function Wheel([int]$notches) {
    $delta = [BitConverter]::ToUInt32([BitConverter]::GetBytes([int]($notches * 120)), 0)
    [Win32.Api]::mouse_event(0x0800, 0, 0, $delta, [System.UIntPtr]::Zero)
    Start-Sleep -Milliseconds 400
    "wheel $notches"
}

function Keys([string]$text) {
    [System.Windows.Forms.SendKeys]::SendWait($text)
    Start-Sleep -Milliseconds 450
    "typed $text"
}

function StopGame() {
    Stop-Process -Id $target.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}

# Clicks a point of the window, given as a fraction of its size, which is how the
# buttons of the title screen and the rows of the world list are found.
function ClickScreen([double]$fx, [double]$fy, [int]$count = 1) {
    $px = $origin.X + [int]($clientW * $fx)
    $py = $origin.Y + [int]($clientH * $fy)
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList $px, $py
    Start-Sleep -Milliseconds 350
    for ($i = 0; $i -lt $count; $i++) {
        [Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero)
        [Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero)
        Start-Sleep -Milliseconds 120
    }
    "clicked screen $fx,$fy x$count"
}

# --- the title screen, then the list of worlds ---------------------------------
ClickScreen 0.5 0.612
if (-not (WaitFor 'Switched to screen WORLD_SELECT' 15)) {
    "FAILED: the world list did not open"
    Capture 'creative_failed_list'
    StopGame
    exit 1
}
"the world list is open"

$entered = $false
foreach ($row in 0.16, 0.22, 0.28) {
    ClickScreen 0.5 $row 2
    if (WaitFor 'Entered world' 10) { $entered = $true; break }
    ClickScreen 0.5 $row 1
    ClickScreen 0.5 0.52 1
    if (WaitFor 'Entered world' 10) { $entered = $true; break }
}
if (-not $entered) {
    "FAILED: no world was entered"
    Capture 'creative_failed_world'
    StopGame
    exit 1
}
"entered the world"
Start-Sleep -Seconds 3
State 'in the world'
Capture 'creative_world'

# --- into creative mode --------------------------------------------------------
Keys 't'
Keys '/gamemode creative'
Keys '{ENTER}'
if (-not (WaitMode 'creative' 15)) {
    "FAILED: /gamemode creative did not answer"
    Capture 'creative_failed_mode'
    StopGame
    exit 1
}
State 'after switching to creative'

# --- the creative inventory ----------------------------------------------------
Keys 'e'
if (-not (WaitFor 'Creative inventory opened' 10)) {
    "FAILED: the creative inventory did not open"
    Capture 'creative_failed_inventory'
    StopGame
    exit 1
}
Start-Sleep -Seconds 1
Capture 'creative_blocks'
State 'with the creative inventory open'

# Places inside the screen, in virtual pixels, see CreativeLayout:
# a tab is 27 wide and starts 4 pixels right of the panel edge, the next one 26 later;
# the first slot of the grid has its icon at 9, 18 and is 16 pixels square.
$tabLeft = { param($index) $panelX + 4 + 26 * $index }
$tabY = $panelY + 136 + 12
$firstSlotX = $panelX + 9 + 8
$firstSlotY = $panelY + 136 - 18 - 8

ClickGui (& $tabLeft 2) $tabY
Start-Sleep -Seconds 1
Capture 'creative_food'
State 'on the food tab'

ClickGui (& $tabLeft 5) $tabY
Start-Sleep -Seconds 1
Capture 'creative_search_empty'
State 'on the search tab with an empty box, which lists every item'

# The row of the scroll bar and the places it is worked from, in virtual pixels, see
# CreativeLayout: the track starts 175 right of the panel edge and is 12 wide, its upper
# end lies 17 below the top edge of the panel and it is 111 tall.
$scrollX = $panelX + 175 + 6
$trackTop = $panelY + 136 - 17 - 7
$trackBottom = $panelY + 136 - 17 - 111 + 7

# An empty box lists every item of the game, which is far more than one page, so the wheel
# has somewhere to go: three notches down walk the list three rows on and leave the thumb
# at the lower end of its track. A search that matches only a few items has nothing to
# scroll, which is why this is done before the box is filled in.
Wheel -3
Start-Sleep -Seconds 1
Capture 'creative_scrolled'
State 'after scrolling the list three rows down with the wheel'

# The thumb of the scroll bar walks the list as well: it is dragged from the lower end of
# the track back to the upper one, which brings the first row back. A list that slid down
# on its own could never be brought up there again.
DragGui $scrollX $trackBottom $scrollX $trackTop
Start-Sleep -Seconds 1
Capture 'creative_scroll_dragged'
State 'after dragging the thumb back to the upper end'

# A click on the track asks for the row under the mouse, so the lower end is reached by a
# single click as well.
ClickGui $scrollX $trackBottom
Start-Sleep -Seconds 1
Capture 'creative_scroll_clicked'
State 'after clicking the lower end of the scroll track'

# Back to the first row, so the stack taken below is the one of the first slot.
DragGui $scrollX $trackBottom $scrollX $trackTop
Start-Sleep -Milliseconds 500

# The search box takes the keyboard once it was clicked.
ClickGui ($panelX + 80 + 30) ($panelY + 136 - 4 - 6)
Keys 'iron'
Start-Sleep -Seconds 1
Capture 'creative_search_iron'

# A search that matches many items fills the grid from its first row again.
Keys '{BACKSPACE}{BACKSPACE}{BACKSPACE}{BACKSPACE}'
Keys 'a'
Start-Sleep -Seconds 1
Capture 'creative_search_many'
State 'after narrowing the list down'

# Taking a stack from the grid, the supply behind it refills the slot.
ClickGui $firstSlotX $firstSlotY
Start-Sleep -Seconds 1
Capture 'creative_carried'

# The name of the item under the mouse.
Point $firstSlotX ($firstSlotY - 18)
Start-Sleep -Seconds 1
Capture 'creative_tooltip'

# A stack that is clicked into the grid is thrown away: the grid owns every item of the
# game and fills itself again anyway, see ContainerMenu#setVoidsOverflow.
ClickGui $firstSlotX $firstSlotY
Start-Sleep -Seconds 1
Capture 'creative_destroyed'
State 'after throwing the carried stack into the grid'

# The last tab shows the inventory of the player, which keeps its crafting field and
# hands its own items to the world when a stack is dragged out of the panel.
ClickGui (& $tabLeft 6) $tabY
Start-Sleep -Seconds 1
Capture 'creative_player_inventory'
State 'on the inventory tab'

Keys '{ESC}'
Start-Sleep -Seconds 1
Capture 'creative_closed'
State 'after closing the creative inventory'

# --- back to survival ----------------------------------------------------------
Keys 't'
Keys '/gamemode survival'
Keys '{ENTER}'
if (-not (WaitMode 'survival' 15)) {
    "FAILED: /gamemode survival did not answer"
    Capture 'creative_failed_survival'
    StopGame
    exit 1
}
Keys 'e'
if (-not (WaitFor 'Inventory opened' 10)) {
    "FAILED: the inventory of the player did not open"
    Capture 'creative_failed_player_inventory'
    StopGame
    exit 1
}
Start-Sleep -Seconds 1
Capture 'creative_survival_inventory'
State 'back in survival'

StopGame
"stopped"
