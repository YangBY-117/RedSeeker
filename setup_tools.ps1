$ErrorActionPreference = "Stop"
$toolsDir = "$PWD\tools_setup"
if (!(Test-Path $toolsDir)) { New-Item -ItemType Directory -Path $toolsDir | Out-Null }

# Maven
$mavenVer = "3.9.6"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVer/binaries/apache-maven-$mavenVer-bin.zip"
$mavenDir = "$toolsDir\apache-maven-$mavenVer"

if (!(Test-Path "$mavenDir\bin\mvn.cmd")) {
    Write-Host "Downloading Maven $mavenVer..."
    $mavenZip = "$toolsDir\maven.zip"
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $mavenZip -DestinationPath $toolsDir -Force
    Remove-Item $mavenZip
} else {
    Write-Host "Maven already installed."
}

# Node.js
$nodeVer = "v20.10.0"
$nodeUrl = "https://nodejs.org/dist/$nodeVer/node-$nodeVer-win-x64.zip"
$nodeDir = "$toolsDir\node-$nodeVer-win-x64"

if (!(Test-Path "$nodeDir\node.exe")) {
    Write-Host "Downloading Node.js $nodeVer..."
    $nodeZip = "$toolsDir\node.zip"
    Invoke-WebRequest -Uri $nodeUrl -OutFile $nodeZip
    Write-Host "Extracting Node.js..."
    Expand-Archive -Path $nodeZip -DestinationPath $toolsDir -Force
    Remove-Item $nodeZip
} else {
    Write-Host "Node.js already installed."
}

# Set Environment Variables permanently (User level)
Write-Host "Setting up environment variables..."

# Set M2_HOME permanently
$currentM2Home = [Environment]::GetEnvironmentVariable("M2_HOME", "User")
if ($currentM2Home -ne $mavenDir) {
    [Environment]::SetEnvironmentVariable("M2_HOME", $mavenDir, "User")
    Write-Host "  M2_HOME set to: $mavenDir"
} else {
    Write-Host "  M2_HOME already set correctly"
}

# Add Maven to PATH permanently (if not already there)
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$mavenBinPath = "$mavenDir\bin"
if ($userPath -notlike "*$mavenBinPath*") {
    $newUserPath = if ($userPath) { "$userPath;$mavenBinPath" } else { $mavenBinPath }
    [Environment]::SetEnvironmentVariable("Path", $newUserPath, "User")
    Write-Host "  Maven bin added to user PATH"
} else {
    Write-Host "  Maven bin already in user PATH"
}

# Refresh current session environment variables
$env:M2_HOME = $mavenDir
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

Write-Host ""
Write-Host "Tools setup complete!"
Write-Host "Note: Environment variables are set permanently. New PowerShell windows will have Maven available."
Write-Host ""
Write-Host "Maven Version:"
mvn -v
Write-Host ""
Write-Host "Node Version:"
node -v
Write-Host ""
Write-Host "npm Version:"
npm -v
