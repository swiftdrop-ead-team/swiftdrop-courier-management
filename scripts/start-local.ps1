param(
    [switch]$Offline,
    [switch]$SkipTests,
    [string]$MavenRepository = ""
)
# Alternative local build when Maven Central is unavailable inside Docker.
# Requires Java 21, Maven, Node/npm, and a running Docker engine.
# Windows PowerShell treats Docker's normal stderr progress as error records.
# Native exit codes below determine failure; explicit throws stop the script.
$ErrorActionPreference = 'Continue'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $projectRoot '.tmp/runtime'
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
Push-Location $projectRoot
try {
    # Webhook secrets must remain decryptable across local restarts.
    $localEnvPath = Join-Path $projectRoot '.env'
    $localEnv = if (Test-Path -LiteralPath $localEnvPath) { [System.IO.File]::ReadAllText($localEnvPath) } else { '' }
    if ($localEnv -notmatch '(?m)^WEBHOOK_ENCRYPTION_KEY=.{32,}$') {
        $secretBytes = New-Object byte[] 48
        $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($secretBytes) } finally { $generator.Dispose() }
        $webhookKey = [Convert]::ToBase64String($secretBytes)
        $localEnv = [regex]::Replace($localEnv, '(?m)^WEBHOOK_ENCRYPTION_KEY=.*\r?\n?', '')
        [System.IO.File]::WriteAllText($localEnvPath, $localEnv.TrimEnd() + "`nWEBHOOK_ENCRYPTION_KEY=$webhookKey`n")
    }
    $servicePaths = [ordered]@{
        'identity-service' = 'services/identity-service'
        'parcel-service' = 'services/parcel-service'
        'merchant-service' = 'services/merchant-service'
        'dispatch-service' = 'services/dispatch-service'
        'pricing-service' = 'services/pricing-service'
        'tracking-service' = 'services/tracking-service'
        'notification-service' = 'services/notification-service'
        'gateway' = 'gateway'
    }
    $composeLines = @('services:')
    foreach ($service in $servicePaths.Keys) {
        $servicePath = $servicePaths[$service]
        $mavenArgs = @('-B', '-f', "$servicePath/pom.xml", 'package')
        if ($Offline) { $mavenArgs += '-o' }
        if ($SkipTests) { $mavenArgs += '-DskipTests' }
        if ($MavenRepository) { $mavenArgs += "-Dmaven.repo.local=$MavenRepository" }
        & mvn @mavenArgs
        if ($LASTEXITCODE -ne 0) { throw "Maven failed for $service" }
        $jar = Get-ChildItem -LiteralPath "$servicePath/target" -Filter '*.jar' -File | Select-Object -First 1
        if (!$jar) { throw "No packaged JAR found for $service" }
        $dockerfile = Join-Path $runtimeDir "$service.Dockerfile"
        @"
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY $($jar.Name) app.jar
USER 1000
ENTRYPOINT ["java", "-jar", "app.jar"]
"@ | Set-Content -LiteralPath $dockerfile -Encoding ASCII
        & docker build --network=none -f $dockerfile -t "swiftdrop-local/$service" "$servicePath/target"
        if ($LASTEXITCODE -ne 0) { throw "Runtime image build failed for $service" }
        $composeLines += "  ${service}:"
        $composeLines += "    image: swiftdrop-local/$service"
        $composeLines += '    pull_policy: never'
    }
    if (!(Test-Path -LiteralPath 'frontend/node_modules/.bin/tsc.cmd')) {
        $installArgs = @('--prefix', 'frontend', 'ci')
        if ($Offline) { $installArgs += '--offline' }
        & npm.cmd @installArgs
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed' }
    }
    & npm.cmd --prefix frontend run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
    Copy-Item -LiteralPath 'frontend/nginx.conf' -Destination 'frontend/dist/nginx.conf'
    $frontendDockerfile = Join-Path $runtimeDir 'frontend.Dockerfile'
    @'
FROM nginx:alpine
COPY . /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN rm /usr/share/nginx/html/nginx.conf
'@ | Set-Content -LiteralPath $frontendDockerfile -Encoding ASCII
    & docker build --network=none -f $frontendDockerfile -t 'swiftdrop-local/frontend' 'frontend/dist'
    if ($LASTEXITCODE -ne 0) { throw 'Frontend runtime image build failed' }
    Remove-Item -LiteralPath 'frontend/dist/nginx.conf'
    $composeLines += @('  frontend:', '    image: swiftdrop-local/frontend', '    pull_policy: never')
    $override = Join-Path $runtimeDir 'compose.yml'
    $composeLines | Set-Content -LiteralPath $override -Encoding ASCII
    & docker compose -f docker-compose.yml -f $override up --no-build -d --wait --wait-timeout 420
    if ($LASTEXITCODE -ne 0) { throw 'Stack startup failed' }
    Write-Host 'Frontend: http://localhost:3000 | Local mail: http://localhost:8025'
    Write-Host 'Next: python scripts/smoke_test.py'
}
finally { Pop-Location }
