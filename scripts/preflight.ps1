$ErrorActionPreference = "Stop"
$RootDir = (Resolve-Path "$PSScriptRoot/..")

function Need-Cmd($Name, $Hint) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] missing '$Name' - $Hint"
    exit 1
  }
  Write-Host "[OK] $Name"
}

function Check-Port($HostName, $Port, $Name) {
  try {
    $ok = Test-NetConnection -ComputerName $HostName -Port $Port -WarningAction SilentlyContinue
    if ($ok.TcpTestSucceeded) {
      Write-Host "[OK] $Name reachable at ${HostName}:$Port"
    } else {
      Write-Host "[WARN] $Name not reachable at ${HostName}:$Port"
    }
  } catch {
    Write-Host "[WARN] $Name check failed at ${HostName}:$Port"
  }
}

Write-Host "== Preflight: used-appliance-system =="
Write-Host "root: $RootDir"

Need-Cmd java "Install JDK 17+"
Need-Cmd mvn "Install Maven 3.9+"
Need-Cmd node "Install Node.js 20+"
Need-Cmd npm "Install npm with Node.js"

Write-Host "== Version Check =="
java -version
mvn -v
node -v
npm -v

Write-Host "== Service Reachability =="
Check-Port "127.0.0.1" 3306 "MySQL"
Check-Port "127.0.0.1" 6379 "Redis"

Write-Host "== Build Smoke Check =="
Push-Location "$RootDir/backend"
mvn -q -DskipTests compile
Pop-Location
Push-Location "$RootDir/frontend"
npm run build --silent | Out-Null
Pop-Location

Write-Host "[OK] preflight complete"
