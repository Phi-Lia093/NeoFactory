# Sends real mouse wheel notches to the running game and reads the status log back,
# which checks the wheel routing without touching the game code: the hotbar has to
# change, the camera zoom has to stay where it was, and with control held the two
# have to swap roles.
param([string]$Jar = 'D:\NeoFactory\lwjgl3\build\libs\NeoFactory-1.0.0.jar',
      [string]$Work = 'D:\NeoFactory\build\smoke',
      [string]$Source = 'D:\NeoFactory\assets\saves\save_mu9r7ci8',
      [string]$Out  = 'D:\NeoFactory\build\verify')

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type -Namespace Win32 -Name Api -MemberDefinition @'
[StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
[StructLayout(LayoutKind.Sequential)] public struct POINT { public int X; public int Y; }
[DllImport("user32.dll")] public static extern bool SetForegroundWindow(System.IntPtr hWnd);
[DllImport("user32.dll")] public static extern bool GetClientRect(System.IntPtr hWnd, out RECT rect);
[DllImport("user32.dll")] public static extern bool ClientToScreen(System.IntPtr hWnd, ref POINT point);
[DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, int data, System.UIntPtr extra);
[DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, System.UIntPtr extra);
'@

$log = "$Out\wheel-out.log"
$before = @(Get-Process java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id)
New-Item -ItemType Directory -Force -Path $Work | Out-Null
New-Item -ItemType Directory -Force -Path "$Work\saves" | Out-Null
Remove-Item -Recurse -Force "$Work\saves\*" -ErrorAction SilentlyContinue
Copy-Item -Recurse -Force $Source "$Work\saves\"

$proc = Start-Process -FilePath 'java' -ArgumentList '-jar', $Jar -WorkingDirectory $Work `
    -RedirectStandardOutput $log -RedirectStandardError "$Out\wheel-err.log" -PassThru
Start-Sleep -Seconds 12

$window = 0
$target = $null
foreach ($p in (Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Sort-Object StartTime -Descending)) {
    if ($before -notcontains $p.Id) { $target = $p; $window = $p.MainWindowHandle; break }
}
if ($window -eq 0) {
    "no window found"
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    exit 1
}
[Win32.Api]::SetForegroundWindow($window) | Out-Null
Start-Sleep -Seconds 1

$rect = New-Object Win32.Api+RECT
[Win32.Api]::GetClientRect($window, [ref]$rect) | Out-Null
$origin = New-Object Win32.Api+POINT
[Win32.Api]::ClientToScreen($window, [ref]$origin) | Out-Null
$clientW = $rect.Right - $rect.Left
$clientH = $rect.Bottom - $rect.Top

function Lmb([int]$x, [int]$y, [int]$count = 1) {
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList ($origin.X + $x), ($origin.Y + $y)
    Start-Sleep -Milliseconds 300
    for ($i = 0; $i -lt $count; $i++) {
        [Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero)
        [Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero)
        Start-Sleep -Milliseconds 120
    }
}

function Wheel([int]$notches) {
    $forward = if ($notches -gt 0) { 120 } else { -120 }
    for ($i = 0; $i -lt [Math]::Abs($notches); $i++) {
        [Win32.Api]::mouse_event(0x0800, 0, 0, $forward, [System.UIntPtr]::Zero)
        Start-Sleep -Milliseconds 150
    }
    "wheel $notches"
}

function State([string]$label) {
    $line = (Get-Content $log | Where-Object { $_ -match 'World .* \| Block' } | Select-Object -Last 1)
    if (-not $line) { "$label : no status line yet"; return }
    $zoom = [regex]::Match($line, 'zoom ([0-9.]+)').Groups[1].Value
    $hotbar = [regex]::Match($line, 'hotbar (\d+)').Groups[1].Value
    "$label : zoom=$zoom hotbar=$hotbar"
}

# Open the world: Singleplayer, then the first row of the world list.
[System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList ($origin.X + [int]($clientW / 2)), ($origin.Y + [int]($clientH * 0.612))
Start-Sleep -Milliseconds 400
[Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero)
[Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero)
Start-Sleep -Seconds 4
Lmb ([int]($clientW / 2)) ([int]($clientH * 0.30)) 2
Start-Sleep -Seconds 8
State 'after opening the world'

# The wheel has to walk the hotbar and leave the camera alone.
Wheel -3
Start-Sleep -Seconds 2
State 'after 3 notches back'
Wheel 3
Start-Sleep -Seconds 2
State 'after 3 notches forward'

# Holding control has to send the same notches to the camera.
[Win32.Api]::keybd_event(0x11, 0, 0, [System.UIntPtr]::Zero)
Start-Sleep -Milliseconds 200
Wheel -3
[Win32.Api]::keybd_event(0x11, 0, 2, [System.UIntPtr]::Zero)
Start-Sleep -Seconds 2
State 'after CTRL and 3 notches back'

Stop-Process -Id $target.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
"stopped"
