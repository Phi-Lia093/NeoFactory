# Walks the shipped jar through the menu: title, world list, a held button and the
# creation form. Captures every step so the pictures can be checked afterwards.
param([string]$Jar = 'D:\NeoFactory\lwjgl3\build\libs\NeoFactory-1.0.0.jar',
      [string]$Work = 'D:\NeoFactory\build\smoke',
      [string]$Out  = 'D:\NeoFactory\build\verify')

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type -Namespace Win32 -Name Api -MemberDefinition @'
[StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
[StructLayout(LayoutKind.Sequential)] public struct POINT { public int X; public int Y; }
[DllImport("user32.dll")] public static extern bool SetForegroundWindow(System.IntPtr hWnd);
[DllImport("user32.dll")] public static extern bool GetClientRect(System.IntPtr hWnd, out RECT rect);
[DllImport("user32.dll")] public static extern bool ClientToScreen(System.IntPtr hWnd, ref POINT point);
[DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, System.UIntPtr extra);
'@

$before = @(Get-Process java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id)
New-Item -ItemType Directory -Force -Path $Work | Out-Null

$proc = Start-Process -FilePath 'java' -ArgumentList '-jar', $Jar -WorkingDirectory $Work `
    -RedirectStandardOutput "$Out\run-out.log" -RedirectStandardError "$Out\run-err.log" -PassThru
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

[Win32.Api]::SetForegroundWindow($window) | Out-Null
Start-Sleep -Seconds 2

$rect = New-Object Win32.Api+RECT
[Win32.Api]::GetClientRect($window, [ref]$rect) | Out-Null
$origin = New-Object Win32.Api+POINT
[Win32.Api]::ClientToScreen($window, [ref]$origin) | Out-Null
$clientW = $rect.Right - $rect.Left
$clientH = $rect.Bottom - $rect.Top
"window $window, client ${clientW}x${clientH} at $($origin.X),$($origin.Y)"

function Capture([string]$name) {
    $bmp = New-Object System.Drawing.Bitmap -ArgumentList $clientW, $clientH
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($origin.X, $origin.Y, 0, 0, $bmp.Size)
    $g.Dispose()
    $bmp.Save("$Out\$name.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    "captured $name"
}

function MoveTo([double]$fraction) {
    $x = [int]($clientW / 2)
    $y = [int]($clientH * $fraction)
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList ($origin.X + $x), ($origin.Y + $y)
    Start-Sleep -Milliseconds 500
    return $y
}

function Press() { [Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero) }
function Release() { [Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero) }

Capture 'ui_title'

# "Singleplayer" is the upper button of the title screen, about three fifths down.
MoveTo 0.612 | Out-Null
Press
Release
Start-Sleep -Seconds 2
Capture 'ui_list'

# The second button of the list menu is "Create New World". It is held down long
# enough to be captured in its pressed state, and releasing it opens the form.
$pressedY = MoveTo 0.685
Press
Start-Sleep -Milliseconds 700
Capture 'ui_pressed'
Release
Start-Sleep -Seconds 2
Capture 'ui_create'

Stop-Process -Id $target.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
"held at y=$pressedY, stopped"
