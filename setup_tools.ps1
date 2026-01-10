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

# Set Environment Variables for this session
$env:M2_HOME = "$mavenDir"
$env:PATH = "$mavenDir\bin;$nodeDir;$env:PATH"

Write-Host "Tools setup complete."
Write-Host "Maven Version:"
mvn -v
Write-Host "Node Version:"
node -v
Write-Host "npm Version:"
npm -v
