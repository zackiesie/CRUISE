param(
    [ValidateSet('Check', 'Backend', 'Client', 'Webhooks')]
    [string]$Target = 'Check'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$allowedNames = @('JAVA_HOME', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD',
    'STRIPE_SECRET_KEY', 'STRIPE_WEBHOOK_SECRET', 'GOOGLE_CLIENT_ID',
    'GOOGLE_CLIENT_SECRET', 'GOOGLE_DESKTOP_CLIENT_ID', 'GOOGLE_DESKTOP_CLIENT_SECRET',
    'CRUISE_ADMIN_USERNAME', 'CRUISE_ADMIN_PASSWORD')

# Read values as literal text. Never execute configuration as PowerShell code.
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        $entry = $line.Trim()
        if (!$entry -or $entry.StartsWith('#')) { continue }
        if ($entry -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') {
            throw 'Invalid .env entry. Use NAME=value, one entry per line.'
        }
        $configName = $Matches[1]
        $configValue = $Matches[2].Trim()
        if ($configName -notin $allowedNames) { throw "Unsupported .env setting: $configName" }
        if ($configValue.Length -ge 2 -and
            (($configValue.StartsWith('"') -and $configValue.EndsWith('"')) -or
             ($configValue.StartsWith("'") -and $configValue.EndsWith("'")))) {
            $configValue = $configValue.Substring(1, $configValue.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($configName, $configValue, 'Process')
    }
}

function Require-Config([string[]]$Names) {
    $missing = @($Names | Where-Object {
        $value = [Environment]::GetEnvironmentVariable($_, 'Process')
        [string]::IsNullOrWhiteSpace($value) -or $value -match 'REPLACE_ME|CHANGE_ME'
    })
    if ($missing.Count) { throw ('Configure these values in .env: ' + ($missing -join ', ')) }
}

if ($Target -eq 'Webhooks') {
    if (!(Get-Command stripe -ErrorAction SilentlyContinue)) { throw 'Install Stripe CLI first.' }
    & stripe listen --events checkout.session.completed,checkout.session.expired --forward-to localhost:8080/api/stripe/webhook
    exit $LASTEXITCODE
}

if ($env:JAVA_HOME) {
    $javaBin = Join-Path $env:JAVA_HOME 'bin'
    if (!(Test-Path -LiteralPath (Join-Path $javaBin 'javac.exe'))) {
        throw 'JAVA_HOME must point to an installed JDK folder, not its bin folder.'
    }
    $env:Path = "$javaBin;$env:Path"
}
if (!(Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install JDK 21 and configure JAVA_HOME.' }
if (!(Get-Command javac -ErrorAction SilentlyContinue)) { throw 'Install a full JDK 21, not only a Java runtime.' }
if (!(Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Install Maven and add its bin folder to PATH.' }
$javaProbe = New-Object System.Diagnostics.Process
$javaProbe.StartInfo.FileName = (Get-Command java).Source
$javaProbe.StartInfo.Arguments = '-version'
$javaProbe.StartInfo.UseShellExecute = $false
$javaProbe.StartInfo.RedirectStandardError = $true
$javaProbe.StartInfo.RedirectStandardOutput = $true
$javaProbe.StartInfo.CreateNoWindow = $true
try {
    [void]$javaProbe.Start()
    $javaVersionText = $javaProbe.StandardError.ReadToEnd() + $javaProbe.StandardOutput.ReadToEnd()
    $javaProbe.WaitForExit()
    if ($javaProbe.ExitCode -ne 0) { throw 'Java failed to start. Check JAVA_HOME.' }
} finally { $javaProbe.Dispose() }
if ($javaVersionText -notmatch 'version "(\d+)') { throw 'Unable to identify Java version.' }
if ([int]$Matches[1] -lt 21) { throw 'This project requires JDK 21+. Configure JAVA_HOME in .env.' }

$backendNames = @('STRIPE_SECRET_KEY', 'STRIPE_WEBHOOK_SECRET', 'GOOGLE_CLIENT_ID', 'GOOGLE_CLIENT_SECRET', 'CRUISE_ADMIN_USERNAME', 'CRUISE_ADMIN_PASSWORD')
$clientNames = @('GOOGLE_DESKTOP_CLIENT_ID', 'GOOGLE_DESKTOP_CLIENT_SECRET')
switch ($Target) {
    'Check' {
        Require-Config ($backendNames + $clientNames)
        & mvn -version
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Write-Host 'Java, Maven and required configuration are ready.'
        Write-Host 'Also start MySQL, create simpleapi_db and keep the Stripe listener running.'
    }
    'Backend' {
        Require-Config $backendNames
        Push-Location (Join-Path $projectRoot 'simpleapi')
        try { & mvn spring-boot:run; $runExitCode = $LASTEXITCODE }
        finally { Pop-Location }
        exit $runExitCode
    }
    'Client' {
        Require-Config $clientNames
        Push-Location (Join-Path $projectRoot 'simpleclient')
        try { & mvn javafx:run; $runExitCode = $LASTEXITCODE }
        finally { Pop-Location }
        exit $runExitCode
    }
}
