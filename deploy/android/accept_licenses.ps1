# Accept Android SDK licenses using PowerShell
$env:ANDROID_HOME = "C:\Android\Sdk"
$env:PATH = "$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

$sdkManager = "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat"

Write-Host "Accepting Android SDK licenses..."

# Create a temporary script that will accept licenses
$licenseScript = @"
$start = Get-Date
while ((Get-Date) -lt $start.AddSeconds(30)) {
    if (Test-Path "C:\Android\Sdk\licenses\android-sdk-license") {
        break
    }
    Start-Sleep -Milliseconds 100
}
"@

# Run sdkmanager with licenses in a way that accepts them
$processInfo = New-Object System.Diagnostics.ProcessStartInfo
$processInfo.FileName = $sdkManager
$processInfo.Arguments = "--licenses"
$processInfo.RedirectStandardInput = $true
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $true
$processInfo.UseShellExecute = $false
$processInfo.CreateNoWindow = $true

$process = [System.Diagnostics.Process]::Start($processInfo)

# Send 'y' responses
for ($i = 0; $i -lt 30; $i++) {
    $process.StandardInput.WriteLine("y")
    Start-Sleep -Milliseconds 200
}

$process.StandardInput.Close()
$output = $process.StandardOutput.ReadToEnd()
$error = $process.StandardError.ReadToEnd()
$process.WaitForExit()

Write-Host "Output:"
Write-Host $output
if ($error) {
    Write-Host "Errors:"
    Write-Host $error
}

Write-Host ""
Write-Host "Checking licenses directory..."
if (Test-Path "C:\Android\Sdk\licenses") {
    Get-ChildItem "C:\Android\Sdk\licenses"
} else {
    Write-Host "Licenses directory not found"
}
