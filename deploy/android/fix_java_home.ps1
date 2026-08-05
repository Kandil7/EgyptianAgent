# Fix JAVA_HOME for EgyptianAgent
$javaHome = "C:\Program Files\Java\jdk-21"

Write-Host "Setting JAVA_HOME to: $javaHome" -ForegroundColor Cyan

# Set machine-level JAVA_HOME
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")

# Add Java bin to PATH if not already there
$machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
if ($machinePath -notlike "*$javaHome\bin*") {
    [System.Environment]::SetEnvironmentVariable("Path", "$machinePath;$javaHome\bin", "Machine")
    Write-Host "Added Java to PATH" -ForegroundColor Green
}

# Set for current session
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

Write-Host "`nJAVA_HOME set successfully!" -ForegroundColor Green
Write-Host "Java version:" -ForegroundColor Cyan
java -version

Write-Host "`nNow you can run:" -ForegroundColor Yellow
Write-Host ".\gradlew.bat clean assembleDebug" -ForegroundColor Cyan
