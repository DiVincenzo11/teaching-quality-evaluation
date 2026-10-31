param([switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localEnv = Join-Path $projectRoot '.local-env.ps1'
if (Test-Path -LiteralPath $localEnv) { . $localEnv }
if ($env:JAVA_HOME) {
    $env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
}
$javaInfo = New-Object System.Diagnostics.ProcessStartInfo
$javaInfo.FileName = (Get-Command java).Source
$javaInfo.Arguments = '-version'
$javaInfo.UseShellExecute = $false
$javaInfo.CreateNoWindow = $true
$javaInfo.RedirectStandardError = $true
$javaProcess = [System.Diagnostics.Process]::Start($javaInfo)
$version = $javaProcess.StandardError.ReadToEnd()
$javaProcess.WaitForExit()
if ($version -notmatch 'version "(\d+)(?:\.(\d+))?') { throw 'Cannot detect Java. Configure JAVA_HOME to JDK 17 or later.' }
$major = [int]$Matches[1]
if ($major -eq 1) { $major = [int]$Matches[2] }
if ($major -lt 17) { throw 'This project requires JDK 17 or later. Configure JAVA_HOME, or set it in .local-env.ps1.' }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven is not available on PATH.' }
Write-Host "Java $major and Maven are available."
if ($CheckOnly) { exit 0 }
Push-Location (Join-Path $projectRoot 'backend')
try {
    & mvn spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE." }
} finally { Pop-Location }
