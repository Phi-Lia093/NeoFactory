# Runs the game, opens a world with the real mouse and keyboard and types into the
# chat, so the runtime path of the input line is checked and not only the unit tests.
#
# Every step is confirmed against the log file the game writes itself: the world is
# only entered when "Entered world" shows up, and a click that missed the list is
# retried at another row and through the button below the list. The status lines of
# the game are read from that log as well, so a message that never arrived cannot be
# mistaken for a slow one.
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
# A fresh log makes the stage checks below unambiguous.
Remove-Item -Recurse -Force "$Work\logs" -ErrorAction SilentlyContinue

$proc = Start-Process -FilePath 'java' -ArgumentList '-jar', $Jar -WorkingDirectory $Work `
    -RedirectStandardOutput "$Out\run-chat-stdout.log" -RedirectStandardError "$Out\run-chat-stderr.log" -PassThru
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
$global:origin = $origin
"client ${clientW}x${clientH} at $($origin.X),$($origin.Y)"

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
    $chat = [regex]::Match($line, '\| chat (.+?) \| target').Groups[1].Value
    "$label : $block chat=$chat"
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

function Click([double]$fx, [double]$fy, [int]$count = 1) {
    $x = [int]($clientW * $fx)
    $y = [int]($clientH * $fy)
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList ($origin.X + $x), ($origin.Y + $y)
    Start-Sleep -Milliseconds 350
    for ($i = 0; $i -lt $count; $i++) {
        [Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero)
        [Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero)
        Start-Sleep -Milliseconds 120
    }
    "clicked $x,$y x$count"
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

# --- the title screen, then the list of worlds ---------------------------------
Click 0.5 0.612
if (-not (WaitFor 'Switched to screen WORLD_SELECT' 15)) {
    "FAILED: the world list did not open"
    Capture 'chat_failed_list'
    StopGame
    exit 1
}
"the world list is open"

# The rows of the list sit in its upper part, the button that opens the selected
# world stands below it: both are tried until the log says the world was entered.
$entered = $false
foreach ($row in 0.16, 0.22, 0.28) {
    Click 0.5 $row 2
    if (WaitFor 'Entered world' 10) { $entered = $true; break }
    Click 0.5 $row 1
    Click 0.5 0.52 1
    if (WaitFor 'Entered world' 10) { $entered = $true; break }
}
if (-not $entered) {
    "FAILED: no world was entered"
    Capture 'chat_failed_world'
    StopGame
    exit 1
}
"entered the world"
Start-Sleep -Seconds 3
State 'in the world'
Capture 'chat_world'

# --- the chat ------------------------------------------------------------------
Keys 't'
Keys 'hello there'
Capture 'chat_typed'
Keys '{ENTER}'
Start-Sleep -Seconds 1
Capture 'chat_message'

Keys '/seed'
Keys '{ENTER}'
Start-Sleep -Seconds 1
Capture 'chat_command'

# A line longer than the box has to be cut at the front.
Keys '/give planks_oak 111 222 333 444 555 666 777 888'
Capture 'chat_long_line'
Keys '{ESC}'
Start-Sleep -Seconds 1
Capture 'chat_after_escape'
State 'after escape'

# The answer of a command ends up in the world: the log shows the new position.
Keys '/tp 100 100'
Keys '{ENTER}'
Start-Sleep -Seconds 3
Capture 'chat_after_tp'
State 'after the teleport'

StopGame
"stopped"
