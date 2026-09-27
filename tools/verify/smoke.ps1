# Smoke test of the shipped jar: start the game, click "Singleplayer" with the
# real mouse, capture both screens and stop the process again. No code hooks.
param([string]$Jar = 'D:\NeoFactory\lwjgl3\build\libs\NeoFactory-1.0.0.jar',
      [string]$Work = 'D:\NeoFactory\build\smoke',
      [string]$Out  = 'D:\NeoFactory\build\verify')

Add-Type -AssemblyName System.Drawing
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
Remove-Item -Recurse -Force "$Work\saves" -ErrorAction SilentlyContinue

$proc = Start-Process -FilePath 'java' -ArgumentList '-jar', $Jar -WorkingDirectory $Work `
    -RedirectStandardOutput "$Out\run-out.log" -RedirectStandardError "$Out\run-err.log" -PassThru
Start-Sleep -Seconds 12

# The launcher may hand over to a second JVM, so look for the newest java process
# that owns a window instead of trusting the process we started.
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

[Win32.Api]::SetForegroundWindow($window) | Out-Null
Start-Sleep -Seconds 2

$rect = New-Object Win32.Api+RECT
[Win32.Api]::GetClientRect($window, [ref]$rect) | Out-Null
$origin = New-Object Win32.Api+POINT
[Win32.Api]::ClientToScreen($window, [ref]$origin) | Out-Null
$clientW = $rect.Right - $rect.Left
$clientH = $rect.Bottom - $rect.Top
"client ${clientW}x${clientH} at $($origin.X),$($origin.Y)"

function Capture([string]$name) {
    $bmp = New-Object System.Drawing.Bitmap -ArgumentList $clientW, $clientH
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($origin.X, $origin.Y, 0, 0, $bmp.Size)
    $g.Dispose()
    $bmp.Save("$Out\$name.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    "captured $name"
}

function Click([int]$x, [int]$y) {
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point -ArgumentList ($origin.X + $x), ($origin.Y + $y)
    Start-Sleep -Milliseconds 400
    [Win32.Api]::mouse_event(0x0002, 0, 0, 0, [System.UIntPtr]::Zero)
    [Win32.Api]::mouse_event(0x0004, 0, 0, 0, [System.UIntPtr]::Zero)
    "clicked $x,$y"
}

Capture 'smoke_title'
Add-Type -AssemblyName System.Windows.Forms
# Interface scale is 3 at 1280 by 800, the Singleplayer button sits in the middle.
Click ([int]($clientW / 2)) ([int]($clientH * 0.612))
Start-Sleep -Seconds 4
Capture 'smoke_list'

Stop-Process -Id $target.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
"stopped"
