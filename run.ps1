param(
    [int]$Port = 8088,
    [string]$OraclePassword = ""
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
if ([string]::IsNullOrWhiteSpace($OraclePassword)) { $OraclePassword = Read-Host 'Oracle application password' }
$env:DB_PASSWORD = $OraclePassword
$env:OPENAI_API_KEY = [Environment]::GetEnvironmentVariable('OPENAI_API_KEY', 'User')
$classpath = 'out;lib\ojdbc11-23.5.0.24.07.jar;lib\slf4j-api-2.0.13.jar;lib\slf4j-simple-2.0.13.jar'

javac -encoding UTF-8 -cp 'lib\ojdbc11-23.5.0.24.07.jar' -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }

$qdrant = 'E:\test\qdrant\qdrant.exe'
if ((Test-Path $qdrant) -and -not (Test-NetConnection 127.0.0.1 -Port 6333 -InformationLevel Quiet)) {
    Start-Process -FilePath $qdrant -WorkingDirectory (Split-Path $qdrant) -WindowStyle Hidden
}

$listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) { Stop-Process -Id $listener.OwningProcess -Force }
Start-Process -FilePath java -ArgumentList '-cp',$classpath,'project.CinehubServer',$Port -WorkingDirectory $root -WindowStyle Hidden
Start-Sleep -Seconds 2
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$Port/api/health" | Select-Object -ExpandProperty Content
