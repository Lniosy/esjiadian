$ErrorActionPreference = "Stop"
$RootDir = (Resolve-Path "$PSScriptRoot/..")
$BackendPort = if ($env:BACKEND_PORT) { $env:BACKEND_PORT } else { "8080" }
$FrontendPort = if ($env:FRONTEND_PORT) { $env:FRONTEND_PORT } else { "5173" }

& "$RootDir/scripts/preflight.ps1"

$runDir = "$RootDir/.run"
if (-not (Test-Path $runDir)) { New-Item -Type Directory -Path $runDir | Out-Null }

$backendLog = "$runDir/backend.log"
$frontendLog = "$runDir/frontend.log"

$backend = Start-Process -FilePath "powershell" -ArgumentList "-NoProfile -Command cd '$RootDir/backend'; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=$BackendPort" -RedirectStandardOutput $backendLog -RedirectStandardError $backendLog -PassThru
$frontend = Start-Process -FilePath "powershell" -ArgumentList "-NoProfile -Command cd '$RootDir/frontend'; $env:VITE_PROXY_TARGET='http://localhost:$BackendPort'; npm run dev -- --host 0.0.0.0 --port $FrontendPort" -RedirectStandardOutput $frontendLog -RedirectStandardError $frontendLog -PassThru

Set-Content "$runDir/backend.pid" $backend.Id
Set-Content "$runDir/frontend.pid" $frontend.Id

Write-Host "[OK] started backend pid=$($backend.Id) log=$backendLog"
Write-Host "[OK] started frontend pid=$($frontend.Id) log=$frontendLog"
Write-Host "backend: http://localhost:$BackendPort"
Write-Host "frontend: http://localhost:$FrontendPort"
